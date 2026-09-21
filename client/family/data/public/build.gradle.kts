plugins { alias(libs.plugins.kmpLibrary) }

kotlin { sourceSets { commonMain.dependencies { implementation(projects.client.shared) } } }

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.family.data"
    enableTesting = true
}
