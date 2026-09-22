plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.client.aichat.public)
            api(projects.client.bottomnav.public)
            api(projects.client.browser.public)
            api(projects.client.cook.public)
            api(projects.client.developerSettings.public)
            api(projects.client.family.manage.public)
            api(projects.client.featureflag.public)
            api(projects.client.grocery.core.public)
            api(projects.client.notifications.public)
            api(projects.client.onboarding.public)
            api(projects.client.profile.public)
            api(projects.client.recipe.core.public)
            api(projects.client.recipe.exporter.public)
            api(projects.client.recipebook.edit.public)
            api(projects.client.auth.ui.public)
            api(projects.client.settings.root.public)
            api(libs.arkivanov.decompose.core)
            implementation(projects.client.shared)
            implementation(projects.client.ui.public)
            implementation(libs.arkivanov.decompose.compose.extensions)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}

plusLibrary { namespace = "com.plusmobileapps.chefmate.root" }
