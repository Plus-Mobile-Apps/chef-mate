plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.coroutines.core)
            api(libs.arkivanov.decompose.core)
            api(projects.client.shared)
            api(projects.client.text.public)
            api(projects.client.grocery.data.public)
            implementation(projects.client.grocery.core.public)
            implementation(projects.client.ui.public)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(compose.components.resources)
        }
    }
}

compose { resources { publicResClass = true } }

plusLibrary { namespace = "com.plusmobileapps.chefmate.grocery.categoryrules" }
