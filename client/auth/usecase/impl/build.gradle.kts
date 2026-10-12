plugins { alias(libs.plugins.kmpLibrary) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.auth.usecase.public)
            implementation(projects.client.shared)
            implementation(projects.client.aichat.public)
            implementation(projects.client.browser.public)
            implementation(projects.client.auth.data.public)
            implementation(projects.client.grocery.data.public)
            implementation(projects.client.meal.data.public)
            implementation(projects.client.recipe.data.public)
            implementation(projects.client.recipebook.data.public)
        }
        commonTest.dependencies {
            implementation(projects.client.auth.data.testing)
            implementation(projects.client.browser.testing)
            implementation(projects.client.grocery.data.testing)
            implementation(projects.client.meal.data.testing)
            implementation(projects.client.recipe.data.testing)
            implementation(projects.client.recipebook.data.testing)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.auth.usecase.impl"
    enableDi = true
    enableTesting = true
}
