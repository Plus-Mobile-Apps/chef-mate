plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.client.text.public)
            api(libs.arkivanov.decompose.core)
            api(libs.arkivanov.decompose.compose.extensions)
            api(libs.compose.material.expressive)
            api(libs.coil.compose)
            api(libs.coil.network.ktor3)
            api(libs.richeditor.compose)
            implementation(compose.components.resources)
            // Backdrop blur for the glass navigation surfaces. Kept as
            // `implementation` (not `api`) — AppBackdrop wraps HazeState so no
            // Haze type leaks into this module's public signatures.
            implementation(libs.haze)
            implementation(libs.haze.blur)
        }
    }
}

compose { resources { publicResClass = true } }

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.ui"
    enableTesting = true
}
