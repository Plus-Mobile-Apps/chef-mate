import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Standalone feature-flag admin dashboard. This module deliberately does NOT apply the
// `kmpLibrary`/`compose` convention plugins: those hardcode Android + iOS + JVM targets, and
// none of the shared `client/*` modules expose a wasmJs target, so they can't be consumed from
// the web. The admin app is therefore self-contained — plain Material 3, its own models, its own
// Supabase client — and applies the Compose + KMP plugins directly so the wasmJs target stays
// isolated from the shipping app's build.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.plusKtfmt)
}

kotlin {
    // Deployable web app.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    // Fast local dev + host for unit tests.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.compose.material.expressive)
            implementation(libs.compose.material.icons.extended)
            implementation(compose.ui)
            implementation(libs.supabase.client)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.kotlin.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.assertions)
            implementation(libs.kotlin.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
        }
        wasmJsMain.dependencies { implementation(libs.ktor.client.js) }
    }
}

compose.desktop {
    application {
        mainClass = "com.plusmobileapps.chefmate.admin.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ChefMateAdmin"
            packageVersion = "1.0.0"
        }
    }
}

// Reuse the same Supabase credentials the app build reads (Gradle property `supabase.url` /
// `supabase.key`, e.g. in ~/.gradle/gradle.properties, or the SUPABASE_URL / SUPABASE_KEY env
// vars). Mirrors client/shared/build.gradle.kts. The anon (public) key is safe to embed in the
// web bundle — admin writes are gated by the `admins` RLS policy, not by key secrecy.
val supabaseUrl =
    (findProperty("supabase.url") as? String)
        ?: System.getenv("SUPABASE_URL")
        ?: "https://your-project-id.supabase.co"

val supabaseKey =
    (findProperty("supabase.key") as? String)
        ?: System.getenv("SUPABASE_KEY")
        ?: "your-anon-public-key"

buildkonfig {
    packageName = "com.plusmobileapps.chefmate.admin.buildconfig"
    objectName = "BuildConfig"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        buildConfigField(STRING, "SUPABASE_URL", supabaseUrl)
        buildConfigField(STRING, "SUPABASE_KEY", supabaseKey)
    }
}
