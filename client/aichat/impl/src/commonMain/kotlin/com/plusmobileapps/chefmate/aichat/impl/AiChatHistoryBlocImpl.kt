package com.plusmobileapps.chefmate.aichat.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.aichat.AiChatHistoryBloc
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = AiChatHistoryBloc.Factory::class,
)
class AiChatHistoryBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<AiChatHistoryBloc.Output>,
    viewModelFactory: () -> AiChatHistoryViewModel,
) : AiChatHistoryBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<AiChatHistoryBloc.Model> =
        viewModel.conversations.mapState {
            AiChatHistoryBloc.Model(conversations = it.toImmutableList())
        }

    override fun onConversationClick(conversationId: Long) {
        output.onNext(AiChatHistoryBloc.Output.OpenConversation(conversationId))
    }

    override fun onDeleteConversationClick(conversationId: Long) {
        viewModel.deleteConversation(conversationId)
    }

    override fun onDeleteAllClick() {
        viewModel.deleteAllConversations()
    }

    override fun onNewConversationClick() {
        output.onNext(AiChatHistoryBloc.Output.NewConversation)
    }

    override fun onBackClicked() {
        output.onNext(AiChatHistoryBloc.Output.Back)
    }
}
