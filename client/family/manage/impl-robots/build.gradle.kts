plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(projects.client.family.manage.public) }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.family.manage.robots"
    uiTest = true
}
