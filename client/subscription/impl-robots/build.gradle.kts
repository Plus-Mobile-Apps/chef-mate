plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets { commonMain.dependencies { implementation(projects.client.subscription.public) } }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.subscription.robots"
    uiTest = true
}
