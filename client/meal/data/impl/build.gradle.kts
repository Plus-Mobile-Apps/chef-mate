plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.database.core)
            implementation(projects.client.meal.data.public)
            implementation(projects.client.recipe.data.public)
            implementation(projects.client.shared)
            implementation(projects.client.auth.data.public)
            implementation(projects.client.family.data.public)
            implementation(projects.client.featureflag.public)
            api(projects.client.util.public)
            implementation(libs.supabase.client)
            implementation(libs.supabase.postgrest)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(projects.client.auth.data.testing)
            implementation(projects.client.family.data.testing)
            implementation(projects.client.featureflag.testing)
            implementation(libs.multiplatform.settings.test)
            implementation(projects.client.recipe.data.testing)
            implementation(projects.client.util.testing)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        androidMain.dependencies { implementation(libs.ktor.client.cio) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.meal.data.impl"
    enableDi = true
    enableTesting = true
    enableDatabaseTesting = true
}
