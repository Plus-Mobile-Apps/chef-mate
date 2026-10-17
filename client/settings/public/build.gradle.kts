plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.arkivanov.decompose.core)
            implementation(projects.client.shared)
            api(projects.client.text.public)
            api(projects.client.browser.public)
            implementation(projects.client.ui.public)
            implementation(projects.client.util.public)
            implementation(projects.client.subscription.public)
            implementation(compose.components.resources)
        }
    }
}

compose { resources { publicResClass = true } }

plusLibrary { namespace = "com.plusmobileapps.chefmate.settings" }
