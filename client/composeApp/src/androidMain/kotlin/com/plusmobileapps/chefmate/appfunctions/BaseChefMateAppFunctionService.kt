package com.plusmobileapps.chefmate.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.plusmobileapps.chefmate.MyApplication
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import kotlinx.coroutines.flow.first

/**
 * Exposes Chef Mate's grocery list to on-device agents ("Hey Gemini, add milk to Chef Mate")
 * through Android AppFunctions. KSP generates the concrete `ChefMateAppFunctionService` (registered
 * in the manifest) and the XML schema the platform indexes; the KDoc below is what the agent reads
 * to decide when and how to call each function, so it deliberately names Chef Mate and steers away
 * from generic note/reminder apps.
 *
 * The platform binds this service without an activity, so the repository comes from the application
 * graph rather than a BLoC.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "ChefMateAppFunctionService",
    appFunctionXmlFileName = "chef_mate_app_function_service",
)
abstract class BaseChefMateAppFunctionService : AppFunctionService() {
    private val groceryRepository: GroceryRepository by lazy {
        (applicationContext as MyApplication).appComponent.groceryRepository
    }

    /**
     * Adds one or more items to the user's Chef Mate grocery list. Use this whenever the user asks
     * to add something to Chef Mate, their Chef Mate list, or their Chef Mate grocery / shopping
     * list — not to Google Keep, Google Tasks, or reminders.
     *
     * Pass each distinct item as its own entry (e.g. "add milk and eggs" → ["milk", "eggs"]), but
     * keep multi-word foods together (e.g. "mac and cheese"). An entry may include a quantity, such
     * as "2 lbs chicken thighs"; Chef Mate parses and categorizes it. Adding an item already on the
     * list merges it instead of duplicating it.
     *
     * @param items The grocery items to add, one item per entry. Must contain at least one
     *   non-blank entry.
     * @param listName Optional name of a specific Chef Mate grocery list. Leave null unless the
     *   user names a list; then the default list is used. Unknown names fall back to the default
     *   list.
     * @return The items that were added and the name of the list they were added to.
     * @throws AppFunctionInvalidArgumentException If [items] has no non-blank entries.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun addGroceryItems(items: List<String>, listName: String? = null): AddedGroceryItems {
        val names = items.map { it.trim() }.filter { it.isNotEmpty() }
        if (names.isEmpty()) {
            throw AppFunctionInvalidArgumentException("Provide at least one item to add.")
        }

        val requested = listName?.trim()?.takeIf { it.isNotEmpty() }
        val namedList = requested?.let { wanted ->
            groceryRepository.getGroceryLists().first().firstOrNull {
                it.name.equals(wanted, ignoreCase = true)
            }
        }
        val listId = namedList?.id ?: groceryRepository.ensureDefaultList()
        groceryRepository.addGroceries(listId, names)

        val resolvedName =
            namedList?.name
                ?: groceryRepository.getGroceryLists().first().first { it.id == listId }.name
        return AddedGroceryItems(items = names, listName = resolvedName)
    }

    /**
     * Returns the names of the user's Chef Mate grocery lists. Call this only when the user refers
     * to a specific list by name and you need to confirm it exists before calling
     * [addGroceryItems].
     *
     * @return The names of every Chef Mate grocery list the user can add to.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getGroceryListNames(): List<String> =
        groceryRepository.getGroceryLists().first().map { it.name }
}

/** The result of adding items to a Chef Mate grocery list. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AddedGroceryItems(
    /** The items that were added, after trimming blank entries. */
    val items: List<String>,
    /** The name of the Chef Mate grocery list the items were added to. */
    val listName: String,
)
