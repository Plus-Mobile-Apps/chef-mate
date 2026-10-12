plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.recipe.data.public)
            implementation(projects.client.recipebook.data.public)
            implementation(projects.client.recipe.core.public)
            implementation(projects.client.recipe.categories.public)
            implementation(projects.client.ui.public)
            implementation(projects.client.toast.public)
            implementation(projects.client.featureflag.public)
            implementation(projects.client.subscription.public)
            implementation(projects.client.review.public)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(projects.client.shared)
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.client.text.public)
            implementation(projects.client.grocery.data.public)
            implementation(projects.client.grocery.core.public)
            implementation(projects.client.meal.data.public)
            implementation(projects.client.util.public)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(projects.client.util.testing)
            implementation(projects.client.recipe.data.testing)
            implementation(projects.client.recipebook.data.testing)
            implementation(projects.client.grocery.data.testing)
            implementation(projects.client.toast.testing)
            implementation(projects.client.featureflag.testing)
            implementation(projects.client.subscription.testing)
            implementation(projects.client.review.testing)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.recipe.core.impl"
    enableDi = true
    enableTesting = true
}
