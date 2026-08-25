plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.settings.root.public)
            implementation(projects.client.settings.public)
            implementation(projects.client.bottomnav.public)
            implementation(projects.client.grocery.autocomplete.public)
            implementation(projects.client.grocery.categoryRules.public)
            implementation(projects.client.recipe.categories.public)
            implementation(projects.client.recipe.exporter.public)
            implementation(projects.client.recipe.importer.public)
            implementation(projects.client.ui.public)
            implementation(projects.client.shared)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.settings.root.impl"
    enableDi = true
    enableTesting = true
}
