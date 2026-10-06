package com.plusmobileapps.chefmate.recipe.list

/**
 * Stable test tags applied to the recipe list screen. Shared with `RecipeListRobot` so UI tests can
 * scope semantics-tree lookups to this screen and avoid colliding with identical text rendered
 * elsewhere (e.g. a recipe title shown on the detail screen header).
 */
object RecipeListTestTags {
    const val SCREEN: String = "recipe_list_screen"
    const val ADD_RECIPE_BUTTON: String = "recipe_list_add_recipe_button"
    const val ADD_MENU_CREATE: String = "recipe_list_add_menu_create"
    const val ADD_MENU_SCAN: String = "recipe_list_add_menu_scan"
    const val SCANNING_DIALOG: String = "recipe_list_scanning_dialog"
    const val SCAN_ERROR_DIALOG: String = "recipe_list_scan_error_dialog"
    const val BOOK_SELECTOR: String = "recipe_list_book_selector"
    const val RECIPE_COUNT: String = "recipe_list_recipe_count"
    const val BOOK_PICKER: String = "recipe_list_book_picker"
    const val BOOK_PICKER_ALL_RECIPES: String = "recipe_list_book_picker_all_recipes"
    const val BOOK_PICKER_CREATE: String = "recipe_list_book_picker_create"
    const val BOOK_PICKER_SHARED_HEADER: String = "recipe_list_book_picker_shared_header"
    const val SELECTION_OVERFLOW: String = "recipe_list_selection_overflow"
    const val SELECTION_ADD_TO_BOOK: String = "recipe_list_selection_add_to_book"
    const val SELECTION_ADD_TO_CATEGORY: String = "recipe_list_selection_add_to_category"
    const val BULK_BOOK_SHEET: String = "recipe_list_bulk_book_sheet"
    const val BULK_CATEGORY_SHEET: String = "recipe_list_bulk_category_sheet"
}
