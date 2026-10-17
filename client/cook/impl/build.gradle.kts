plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.cook.public)
            implementation(projects.client.recipe.data.public)
            implementation(projects.client.shared)
            implementation(projects.client.database.core)
            implementation(projects.client.text.public)
            implementation(projects.client.ui.public)
            implementation(projects.client.featureflag.public)
            implementation(projects.client.subscription.public)
            implementation(projects.client.review.public)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.multiplatform.settings.test)
            implementation(projects.client.recipe.data.testing)
            implementation(projects.client.featureflag.testing)
            implementation(projects.client.subscription.testing)
            implementation(projects.client.review.testing)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.cook.impl"
    enableDi = true
    enableTesting = true
}
