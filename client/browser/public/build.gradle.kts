plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

fun osClassifier(): String {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch")
    return when {
        osName.contains("mac") || osName.contains("darwin") -> {
            if (osArch == "aarch64") "mac-aarch64" else "mac"
        }
        osName.contains("win") -> "win"
        osName.contains("linux") -> {
            if (osArch == "aarch64") "linux-aarch64" else "linux"
        }
        else -> error("Unsupported OS: $osName")
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.arkivanov.decompose.core)
            implementation(libs.arkivanov.decompose.compose.extensions)
            implementation(projects.client.shared)
            api(projects.client.text.public)
            api(projects.client.recipe.data.public)
            implementation(projects.client.ui.public)
            implementation(compose.components.resources)
        }
        getByName("androidMain").dependencies {
            implementation(libs.multiplatform.webview)
            implementation(libs.androidx.webkit)
        }
        getByName("iosMain").dependencies { implementation(libs.multiplatform.webview) }
        getByName("jvmMain").dependencies {
            val fxClassifier = osClassifier()
            val fxVersion = libs.versions.openjfx.get()
            implementation("org.openjfx:javafx-base:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-controls:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-graphics:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-media:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-swing:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-web:$fxVersion:$fxClassifier")
        }
    }
}

compose { resources { publicResClass = true } }

plusLibrary { namespace = "com.plusmobileapps.chefmate.browser" }
