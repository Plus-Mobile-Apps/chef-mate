package com.plusmobileapps.chefmate.grocery.core.impl.list

import chefmate.client.grocery.core.public.generated.resources.Res
import chefmate.client.grocery.core.public.generated.resources.grocery_autocomplete_manage
import chefmate.client.grocery.core.public.generated.resources.grocery_autocomplete_saved
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.grocery.core.detail.GroceryDetailBloc
import com.plusmobileapps.chefmate.grocery.core.impl.list.GroceryListPresenter.Event
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryFilter
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GrocerySort
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryListInvite
import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.toast.ToastService
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = GroceryListBloc.Factory::class,
)
class GroceryListBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<GroceryListBloc.Output>,
    viewModelFactory: () -> GroceryListViewModel,
    private val groceryDetailFactory: GroceryDetailBloc.Factory,
    private val toastService: ToastService,
) : GroceryListBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    private val sheetNavigation = SlotNavigation<SheetConfig>()
    private val sheetRouter =
        childSlot(
            source = sheetNavigation,
            serializer = SheetConfig.serializer(),
            key = "GroceryListBloc_Sheet",
            childFactory = ::createSheet,
        )

    override val childSlot: Value<ChildSlot<*, GroceryListBloc.Sheet>> = sheetRouter

    private val sheetBackCallback =
        BackCallback(isEnabled = sheetRouter.value.child != null) { sheetNavigation.dismiss() }

    init {
        backHandler.register(sheetBackCallback)
        sheetRouter.subscribe { slot -> sheetBackCallback.isEnabled = slot.child != null }
    }

    override val state: StateFlow<GroceryListBloc.Model> =
        viewModel.state.mapState {
            GroceryListBloc.Model(
                groupedItems = it.groupedItems.toImmutableList(),
                sort = it.sort,
                filter = it.filter,
                recipeFilter = it.recipeFilter,
                availableRecipes = it.availableRecipes.toImmutableList(),
                autocompleteSuggestions = it.autocompleteSuggestions.toImmutableList(),
                queryMatchesSavedAutocomplete = it.queryMatchesSavedAutocomplete,
                hasNoRecipeItems = it.hasNoRecipeItems,
                isSyncing = it.isSyncing,
                lists = it.lists.toImmutableList(),
                selectedList = it.selectedList,
                showCreateListDialog = it.showCreateListDialog,
                showDeleteDialog = it.showDeleteDialog,
                showListSelector = it.showListSelector,
                pendingInvitations = it.pendingInvitations,
                currentUserRole = it.currentUserRole,
                showSyncTooltip = it.showSyncTooltip,
            )
        }

    override val newGroceryItemName: StateFlow<String> = viewModel.newGroceryItemName

    override fun onGroceryItemCheckedChange(item: GroceryItem, isChecked: Boolean) {
        viewModel.take(Event.CheckedChange(item, isChecked))
    }

    override fun onGroceryItemDelete(item: GroceryItem) {
        viewModel.take(Event.DeleteItem(item))
    }

    override fun onNewGroceryItemNameChange(name: String) {
        if (name.contains("\n")) {
            viewModel.saveGroceryItem()
        } else {
            viewModel.onNewGroceryItemNameChange(name)
        }
    }

    override fun saveGroceryItem() {
        viewModel.saveGroceryItem()
    }

    override fun onSaveAutocompleteItem(name: String) {
        if (name.isBlank()) return
        viewModel.take(Event.SaveAutocompleteItem(name))
        // Confirm the save and offer a jump to the settings screen that lists every saved item. The
        // action routes through the app-scoped output rather than capturing this screen, since the
        // toast callback outlives the composition.
        toastService.show(
            message = ResourceString(Res.string.grocery_autocomplete_saved),
            actionLabel = ResourceString(Res.string.grocery_autocomplete_manage),
            onAction = { output.onNext(GroceryListBloc.Output.OpenAutocompleteSettings) },
        )
    }

    override fun onGroceryItemClicked(item: GroceryItem) {
        sheetNavigation.activate(SheetConfig.GroceryDetail(item.id))
    }

    override fun onDismissSheet() {
        sheetNavigation.dismiss()
    }

    override fun onSyncClicked() {
        viewModel.take(Event.Sync)
    }

    override fun onSyncTooltipDismissed() {
        viewModel.take(Event.DismissSyncTooltip)
    }

    override fun onListSelected(list: GroceryListModel) {
        viewModel.take(Event.SelectList(list))
    }

    override fun onCreateListClicked() {
        viewModel.take(Event.ShowCreateList)
    }

    override fun onCreateListDismissed() {
        viewModel.take(Event.DismissCreateList)
    }

    override fun onCreateListConfirmed(name: String) {
        viewModel.take(Event.CreateList(name))
    }

    override fun onDeleteListClicked(list: GroceryListModel) {
        viewModel.take(Event.DeleteList(list))
    }

    override fun onApplySortAndFilter(
        sort: GrocerySort,
        filter: GroceryFilter,
        recipeFilter: String?,
    ) {
        viewModel.take(Event.ApplySortAndFilter(sort, filter, recipeFilter))
    }

    override fun onClearFiltersClicked() {
        viewModel.take(Event.ClearFilters)
    }

    override fun onDeleteClicked() {
        viewModel.take(Event.ShowDeleteDialog)
    }

    override fun onDeleteDismissed() {
        viewModel.take(Event.DismissDeleteDialog)
    }

    override fun onDeletePurchasedConfirmed() {
        viewModel.take(Event.DeletePurchased)
    }

    override fun onDeleteAllConfirmed() {
        viewModel.take(Event.DeleteAll)
    }

    override fun onListSelectorClicked() {
        viewModel.take(Event.ShowListSelector)
    }

    override fun onListSelectorDismissed() {
        viewModel.take(Event.DismissListSelector)
    }

    override fun onBrowseRecipesClicked() {
        output.onNext(GroceryListBloc.Output.OpenRecipes)
    }

    override fun onEditListClicked(list: GroceryListModel) {
        output.onNext(GroceryListBloc.Output.OpenEditList(list.id))
    }

    override fun onAcceptInvitation(invite: GroceryListInvite) {
        viewModel.take(Event.AcceptInvitation(invite))
    }

    override fun onRejectInvitation(invite: GroceryListInvite) {
        viewModel.take(Event.RejectInvitation(invite))
    }

    private fun createSheet(config: SheetConfig, context: BlocContext): GroceryListBloc.Sheet =
        when (config) {
            is SheetConfig.GroceryDetail ->
                GroceryListBloc.Sheet.GroceryDetail(
                    bloc =
                        groceryDetailFactory.create(
                            context = context,
                            id = config.itemId,
                            output = { detailOutput ->
                                when (detailOutput) {
                                    GroceryDetailBloc.Output.Finished -> sheetNavigation.dismiss()
                                }
                            },
                        )
                )
        }

    @Serializable
    sealed class SheetConfig {
        @Serializable data class GroceryDetail(val itemId: Long) : SheetConfig()
    }
}
