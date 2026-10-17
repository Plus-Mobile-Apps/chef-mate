plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.grocery.categoryRules.public)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.grocery.categoryrules.robots"
    uiTest = true
}
