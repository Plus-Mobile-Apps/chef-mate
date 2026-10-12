plugins { alias(libs.plugins.kmpLibrary) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.review.public)
            implementation(projects.client.shared)
            implementation(libs.multiplatform.settings)
        }
        commonTest.dependencies { implementation(libs.multiplatform.settings.test) }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.review.impl"
    enableDi = true
    enableTesting = true
}
