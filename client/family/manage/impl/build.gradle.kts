plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.family.manage.public)
            implementation(projects.client.family.data.public)
            implementation(projects.client.text.public)
            implementation(projects.client.toast.public)
            implementation(projects.client.shared)
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(projects.client.family.data.testing)
            implementation(projects.client.toast.testing)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.family.manage.impl"
    enableDi = true
    enableTesting = true
}
