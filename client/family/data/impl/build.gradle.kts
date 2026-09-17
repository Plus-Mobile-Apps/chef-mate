plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.family.data.public)
            implementation(projects.client.shared)
            implementation(libs.supabase.client)
            implementation(libs.supabase.postgrest)
            implementation(libs.kotlinx.serialization.json)
        }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        androidMain.dependencies { implementation(libs.ktor.client.cio) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.family.data.impl"
    enableDi = true
    enableTesting = true
}
