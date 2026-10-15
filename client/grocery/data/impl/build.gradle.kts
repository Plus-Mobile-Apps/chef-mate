plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.database.core)
            implementation(projects.client.grocery.data.public)
            implementation(projects.client.shared)
            // Only the Compose-free DateTimeUtil is needed here; depend on util:core:public
            // rather than the Compose-coupled util:public so the data layer stays Compose-free.
            implementation(projects.client.util.core.public)
            implementation(projects.client.auth.data.public)
            implementation(libs.supabase.client)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.realtime)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(projects.client.auth.data.testing)
            implementation(projects.client.grocery.data.testing)
            implementation(projects.client.util.testing)
            implementation(libs.multiplatform.settings.test)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        androidMain.dependencies { implementation(libs.ktor.client.cio) }
        appleMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.grocery.data.impl"
    enableDi = true
    enableTesting = true
    enableDatabaseTesting = true
    enableWatch = true
}
