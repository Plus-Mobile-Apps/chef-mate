import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.androidBuiltInKotlin)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.screenshot)
    alias(libs.plugins.plusKtfmt)
}

android {
    namespace = "com.plusmobileapps.chefmate.ui.screenshot"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures { compose = true }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

// Absorbs the ~1-pixel-different antialiasing on FAB shadows / elevation edges that Mac arm64
// and Linux x64 (CI) produce for the same Compose preview. Tight enough to still catch real
// regressions (missing text, colour drift, layout shifts).
@Suppress("UnstableApiUsage") screenshotTests { imageDifferenceThreshold = 0.001f }

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

dependencies {
    implementation(project(":client:ui:public"))
    implementation(project(":client:text:public"))
    implementation(project(":client:aichat:public"))
    implementation(project(":client:aichat:impl"))
    implementation(project(":client:auth:ui:public"))
    implementation(project(":client:auth:ui:impl"))
    implementation(project(":client:bottomnav:public"))
    implementation(project(":client:bottomnav:impl"))
    implementation(project(":client:browser:public"))
    implementation(project(":client:cook:public"))
    implementation(project(":client:cook:impl"))
    implementation(project(":client:grocery:core:public"))
    implementation(project(":client:grocery:core:impl"))
    implementation(project(":client:grocery:autocomplete:public"))
    implementation(project(":client:grocery:category-rules:public"))
    implementation(project(":client:meal:core:public"))
    implementation(project(":client:meal:core:impl"))
    implementation(project(":client:meal:data:public"))
    implementation(project(":client:notifications:public"))
    implementation(project(":client:notifications:impl"))
    implementation(project(":client:onboarding:public"))
    implementation(project(":client:onboarding:impl"))
    implementation(project(":client:recipe:categories:public"))
    implementation(project(":client:recipe:categories:impl"))
    implementation(project(":client:recipe:core:public"))
    implementation(project(":client:recipe:core:impl"))
    implementation(project(":client:recipe:list:public"))
    implementation(project(":client:recipe:list:impl"))
    implementation(project(":client:recipe:exporter:public"))
    implementation(project(":client:recipe:importer:public"))
    implementation(project(":client:recipebook:edit:public"))
    implementation(project(":client:recipe:data:public"))
    implementation(project(":client:settings:public"))
    implementation(project(":client:settings:impl"))
    implementation(project(":client:settings:root:public"))
    implementation(project(":client:profile:public"))
    implementation(project(":client:profile:impl"))
    implementation(project(":client:toast:public"))
    implementation(project(":client:toast:testing"))
    implementation(libs.arkivanov.decompose.core)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
