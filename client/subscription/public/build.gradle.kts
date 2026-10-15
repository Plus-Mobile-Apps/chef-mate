plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.shared)
            api(projects.client.text.public)
            implementation(projects.client.ui.public)
            implementation(compose.components.resources)
        }
        // RevenueCat's server-driven Paywall is Android/iOS only; jvmMain renders an "unavailable"
        // dialog instead. See PremiumPaywall.
        androidMain.dependencies {
            implementation(libs.purchases.core)
            implementation(libs.purchases.ui)
        }
        getByName("iosMain").dependencies {
            implementation(libs.purchases.core)
            implementation(libs.purchases.ui)
        }
    }
}

compose { resources { publicResClass = true } }

plusLibrary { namespace = "com.plusmobileapps.chefmate.subscription" }
