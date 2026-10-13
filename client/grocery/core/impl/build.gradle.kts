plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.auth.data.public)
            implementation(projects.client.grocery.data.public)
            implementation(projects.client.grocery.core.public)
            implementation(projects.client.text.public)
            implementation(projects.client.toast.public)
            implementation(projects.client.ui.public)
            implementation(projects.client.shared)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(projects.client.database.core)
            implementation(compose.components.resources)
            implementation(libs.molecule.runtime)
        }
        commonTest.dependencies {
            implementation(projects.client.grocery.data.testing)
            implementation(projects.client.toast.testing)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.grocery.core.impl"
    enableDi = true
    enableTesting = true
}
