plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.client.settings.public)
            api(projects.client.bottomnav.public)
            api(projects.client.grocery.autocomplete.public)
            api(projects.client.grocery.categoryRules.public)
            api(projects.client.recipe.categories.public)
            api(projects.client.recipe.exporter.public)
            api(projects.client.recipe.importer.public)
            implementation(projects.client.ui.public)
            implementation(projects.client.shared)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
        }
    }
}

plusLibrary { namespace = "com.plusmobileapps.chefmate.settings.root" }
