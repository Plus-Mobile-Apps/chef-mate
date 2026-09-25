plugins { alias(libs.plugins.kmpLibrary) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.notifications.data.public)
            implementation(projects.client.grocery.data.public)
            implementation(projects.client.recipebook.data.public)
            implementation(projects.client.family.data.public)
            implementation(projects.client.shared)
        }
        commonTest.dependencies {
            implementation(projects.client.grocery.data.testing)
            implementation(projects.client.recipebook.data.testing)
            implementation(projects.client.family.data.testing)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.notifications.data.impl"
    enableDi = true
    enableTesting = true
}
