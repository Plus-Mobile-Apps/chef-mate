plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.grocery.categoryRules.public)
            implementation(projects.client.grocery.data.public)
            implementation(projects.client.shared)
            implementation(projects.client.ui.public)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
        }
        commonTest.dependencies { implementation(projects.client.grocery.data.testing) }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.grocery.categoryrules.impl"
    enableDi = true
    enableTesting = true
}
