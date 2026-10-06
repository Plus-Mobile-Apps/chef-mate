plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.coroutines.core)
            api(libs.arkivanov.decompose.core)
            api(projects.client.shared)
            api(projects.client.text.public)
            api(projects.client.ui.public)
            api(projects.client.family.data.public)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(compose.components.resources)
        }
    }
}

compose { resources { publicResClass = true } }

plusLibrary { namespace = "com.plusmobileapps.chefmate.family.manage" }
