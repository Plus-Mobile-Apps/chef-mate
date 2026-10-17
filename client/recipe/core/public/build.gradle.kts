plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.client.recipe.data.public)
            api(projects.client.recipebook.data.public)
            api(projects.client.text.public)
            api(libs.kotlinx.serialization.json)

            api(projects.client.meal.data.public)
            implementation(libs.kotlinx.datetime)
            implementation(projects.client.shared)
            implementation(projects.client.ui.public)
            implementation(projects.client.toast.public)
            implementation(projects.client.util.public)
            implementation(projects.client.grocery.data.public)
            implementation(projects.client.grocery.core.public)
            implementation(projects.client.recipe.categories.public)
            implementation(projects.client.subscription.public)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(libs.arkivanov.decompose.core)
            implementation(compose.components.resources)
        }
    }
}

compose { resources { publicResClass = true } }

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.recipe.core"
    enableTesting = true
}
