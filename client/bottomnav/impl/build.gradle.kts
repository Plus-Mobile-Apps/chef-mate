plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.bottomnav.public)
            implementation(projects.client.text.public)
            implementation(projects.client.ui.public)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(projects.client.shared)
            implementation(projects.client.notifications.data.public)
            implementation(projects.client.toast.public)
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.components.resources)
            api(projects.client.browser.public)
            api(projects.client.grocery.core.public)
            api(projects.client.meal.core.public)
            api(projects.client.recipe.list.public)
            api(projects.client.settings.public)
        }
        commonTest.dependencies {
            implementation(projects.client.notifications.data.testing)
            implementation(projects.client.toast.testing)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.recipe.bottomnav.impl"
    enableDi = true
    enableTesting = true
}
