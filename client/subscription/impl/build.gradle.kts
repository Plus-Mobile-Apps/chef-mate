plugins { alias(libs.plugins.kmpLibrary) }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.subscription.public)
            implementation(projects.client.auth.data.public)
            implementation(projects.client.developerSettings.public)
            implementation(projects.client.shared)
        }
        // RevenueCat's purchases-kmp SDK ships only Android + iOS artifacts, and the default
        // source-set hierarchy has no shared Android+iOS parent, so the (identical) RevenueCat
        // gateway and initializer live in both androidMain and iosMain; jvmMain gets no-ops.
        androidMain.dependencies {
            implementation(libs.purchases.core)
            implementation(libs.purchases.result)
        }
        getByName("iosMain").dependencies {
            implementation(libs.purchases.core)
            implementation(libs.purchases.result)
        }
        commonTest.dependencies {
            implementation(projects.client.auth.data.testing)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.subscription.impl"
    enableDi = true
    enableTesting = true
}
