plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.coroutines.core)
            api(projects.client.text.public)
            implementation(projects.client.ui.public)
            implementation(compose.components.resources)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.play.review.ktx)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.review"
    enableTesting = true
}
