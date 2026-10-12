plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.browser.public)
            implementation(projects.client.shared)
            implementation(projects.client.recipe.data.public)
            implementation(projects.client.database.core)
            implementation(projects.client.text.public)
            implementation(projects.client.ui.public)
            implementation(projects.client.auth.data.public)
            implementation(projects.client.util.core.public)
            implementation(libs.supabase.client)
            implementation(libs.supabase.postgrest)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ksoup)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.multiplatform.settings.test)
            implementation(projects.client.recipe.data.testing)
            implementation(projects.client.auth.data.testing)
            implementation(projects.client.browser.testing)
            implementation(projects.client.util.testing)
        }
        getByName("androidMain").dependencies { implementation(libs.ktor.client.cio) }
        getByName("jvmMain").dependencies { implementation(libs.ktor.client.cio) }
        getByName("iosMain").dependencies { implementation(libs.ktor.client.darwin) }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.browser.impl"
    enableDi = true
    enableTesting = true
    enableDatabaseTesting = true
}
