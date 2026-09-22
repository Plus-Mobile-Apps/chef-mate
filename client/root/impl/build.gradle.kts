plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.root.public)
            implementation(libs.arkivanov.decompose.core)
            implementation(projects.client.shared)
            implementation(projects.client.aichat.public)
            implementation(projects.client.bottomnav.public)
            implementation(projects.client.browser.public)
            implementation(projects.client.cook.public)
            implementation(projects.client.family.manage.public)
            implementation(projects.client.featureflag.public)
            implementation(projects.client.grocery.core.public)
            implementation(projects.client.notifications.public)
            implementation(projects.client.onboarding.public)
            implementation(projects.client.profile.public)
            implementation(projects.client.recipe.core.public)
            implementation(projects.client.recipe.exporter.public)
            implementation(projects.client.recipebook.edit.public)
            implementation(projects.client.settings.root.public)
            implementation(projects.client.auth.ui.public)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(projects.client.auth.data.testing)
            implementation(projects.client.featureflag.testing)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.root.impl"
    enableDi = true
    enableTesting = true
}
