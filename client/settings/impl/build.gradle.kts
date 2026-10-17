plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.settings.public)
            implementation(projects.client.text.public)
            implementation(projects.client.ui.public)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(projects.client.shared)
            implementation(projects.client.auth.data.public)
            implementation(projects.client.auth.usecase.public)
            implementation(projects.client.browser.public)
            implementation(projects.client.featureflag.public)
            implementation(projects.client.subscription.public)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(projects.client.auth.data.testing)
            implementation(projects.client.browser.testing)
            implementation(projects.client.featureflag.testing)
            implementation(projects.client.subscription.testing)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.settings.impl"
    enableDi = true
    enableTesting = true
}
