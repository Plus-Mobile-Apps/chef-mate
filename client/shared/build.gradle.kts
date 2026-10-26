import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties

plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.arkivanov.decompose.core)
            api(libs.kotlin.coroutines.core)
            api(libs.kermit)
            api(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.noarg)
            implementation(libs.essenty.lifecycle.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.shared"
    enableDi = true
    enableWatch = true
}

// Load properties from local.properties
val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }

// Read Supabase credentials from Gradle properties (e.g. ~/.gradle/gradle.properties) or env vars
val supabaseUrl =
    (findProperty("supabase.url") as? String)
        ?: System.getenv("SUPABASE_URL")
        ?: "https://your-project-id.supabase.co"

val supabaseKey =
    (findProperty("supabase.key") as? String)
        ?: System.getenv("SUPABASE_KEY")
        ?: "your-anon-public-key"

val supabaseTestingUrl =
    (findProperty("supabase.testing.url") as? String)
        ?: System.getenv("SUPABASE_TESTING_URL")
        ?: supabaseUrl

val supabaseTestingKey =
    (findProperty("supabase.testing.key") as? String)
        ?: System.getenv("SUPABASE_TESTING_KEY")
        ?: supabaseKey

val bugsnagApiKey =
    (findProperty("bugsnag.apiKey") as? String) ?: System.getenv("BUGSNAG_API_KEY") ?: ""

val revenueCatAndroidApiKey =
    (findProperty("revenuecat.android.apiKey") as? String)
        ?: System.getenv("REVENUECAT_ANDROID_API_KEY")
        ?: ""

val revenueCatIosApiKey =
    (findProperty("revenuecat.ios.apiKey") as? String)
        ?: System.getenv("REVENUECAT_IOS_API_KEY")
        ?: ""

// Collect test users by incrementing n until a pair is missing. Looked up in order:
// 1. local.properties at the project root (chefmate.user.<n> / chefmate.user.password.<n>)
// 2. ~/.gradle/gradle.properties or any other Gradle property source (same keys)
// 3. Environment variables (CHEF_MATE_USER_<n> / CHEF_MATE_USER_PASSWORD_<n>)
// Serialize as "email1|password1;email2|password2".
val testUsersSerialized = buildString {
    var index = 1
    while (true) {
        val email =
            localProperties.getProperty("chefmate.user.$index")
                ?: (findProperty("chefmate.user.$index") as? String)
                ?: System.getenv("CHEF_MATE_USER_$index")
        val password =
            localProperties.getProperty("chefmate.user.password.$index")
                ?: (findProperty("chefmate.user.password.$index") as? String)
                ?: System.getenv("CHEF_MATE_USER_PASSWORD_$index")
        if (email.isNullOrBlank() || password.isNullOrBlank()) break
        if (index > 1) append(';')
        append(email).append('|').append(password)
        index++
    }
}

// Detect debug builds by inspecting requested Gradle task names. Defaults to debug when no
// release-flavoured task is requested (e.g. during IDE sync), which is the safe choice for
// developer-only UI gating.
//
// iOS is a special case: Xcode invokes `embedAndSignAppleFrameworkForXcode`, whose task name
// never contains "Release". The real build type for an Xcode-driven build comes from the
// CONFIGURATION env var Xcode sets ("Debug" / "Release"), so honour that too — otherwise
// production App Store builds are misdetected as debug and leak developer-only UI.
val xcodeConfiguration: String? = System.getenv("CONFIGURATION")
val isReleaseBuild: Boolean =
    gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("Release", ignoreCase = true)
    } || xcodeConfiguration?.equals("Release", ignoreCase = true) == true
val isDebugBuildFlag: Boolean = !isReleaseBuild

// Single source of truth lives in client/composeApp/build.gradle.kts (versionName = "..").
// The bump-version scripts already update that file via sed; we mirror its value into
// BuildConfig so common code (e.g. feature-flag version targeting) can read it.
val composeAppGradleFile = rootProject.file("client/composeApp/build.gradle.kts")
val appVersionName =
    Regex("""versionName\s*=\s*"([^"]+)"""")
        .find(composeAppGradleFile.readText())
        ?.groupValues
        ?.get(1) ?: "0.0.0"

buildkonfig {
    packageName = "com.plusmobileapps.chefmate.buildconfig"
    objectName = "BuildConfig"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        buildConfigField(STRING, "SUPABASE_URL", supabaseUrl)
        buildConfigField(STRING, "SUPABASE_KEY", supabaseKey)
        buildConfigField(STRING, "SUPABASE_PROD_URL", supabaseUrl)
        buildConfigField(STRING, "SUPABASE_PROD_KEY", supabaseKey)
        buildConfigField(STRING, "SUPABASE_TESTING_URL", supabaseTestingUrl)
        buildConfigField(STRING, "SUPABASE_TESTING_KEY", supabaseTestingKey)
        buildConfigField(STRING, "BUGSNAG_API_KEY", bugsnagApiKey)
        buildConfigField(STRING, "REVENUECAT_ANDROID_API_KEY", revenueCatAndroidApiKey)
        buildConfigField(STRING, "REVENUECAT_IOS_API_KEY", revenueCatIosApiKey)
        buildConfigField(STRING, "TEST_USERS", testUsersSerialized)
        buildConfigField(BOOLEAN, "IS_DEBUG", isDebugBuildFlag.toString())
        buildConfigField(STRING, "VERSION_NAME", appVersionName)
    }
}
