plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.cook.public)
            implementation(projects.client.meal.core.public)
            implementation(projects.client.meal.data.public)
            implementation(projects.client.recipe.core.public)
            implementation(projects.client.recipe.data.public)
            implementation(projects.client.ui.public)
            implementation(projects.client.toast.public)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.client.shared)
            implementation(projects.client.text.public)
            implementation(projects.client.util.public)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(projects.client.meal.data.testing)
            implementation(projects.client.util.testing)
            implementation(projects.client.toast.testing)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.meal.core.impl"
    enableDi = true
    enableTesting = true
}
