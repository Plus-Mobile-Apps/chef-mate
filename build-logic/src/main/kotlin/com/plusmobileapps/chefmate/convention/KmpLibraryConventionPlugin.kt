package com.plusmobileapps.chefmate.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.plusmobileapps.chefmate.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Create plusMobile extension for configuration. The `onWatchEnabled` callback fires
            // synchronously when a module sets `plusLibrary { enableWatch = true }` — i.e. during
            // configuration, the valid window to register extra KMP targets. By then the Kotlin
            // multiplatform plugin (applied below) is present, so the extension is available.
            val plusLibraryExtension =
                extensions.create<PlusLibraryExtension>("plusLibrary", { registerWatchTargets() })

            // Add afterEvaluate hook to catch property changes
            afterEvaluate {
                if (plusLibraryExtension.enableDi) {
                    applyMetro()
                }

                if (plusLibraryExtension.enableTesting) {
                    applyTesting(enableDatabaseTesting = plusLibraryExtension.enableDatabaseTesting)
                }

                if (plusLibraryExtension.uiTest) {
                    applyUiTest()
                }
            }

            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                // AGP 9: the new KMP-aware Android library plugin replaces
                // `com.android.library` for multiplatform modules. It registers the
                // Android target itself and exposes its DSL inside `kotlin { }`
                // under the `android` extension.
                apply("com.android.kotlin.multiplatform.library")
            }

            target.applyKtfmt()

            // Defer namespace resolution until after the module's build.gradle.kts has
            // evaluated. compileSdk, minSdk, withHostTest, and jvmTarget don't depend on
            // user input and are safe to set eagerly, but `namespace` requires the
            // per-module value from `plusLibrary { ... }`, which isn't available yet.
            val androidComponents =
                extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
            androidComponents.finalizeDsl { extension ->
                extension.namespace =
                    plusLibraryExtension.namespace
                        ?: throw IllegalStateException(
                            """
                    Please set the namespace for the module $name in the plusMobile extension in the module's build.gradle.kts file.
                    Example:
                    plusLibrary {
                        namespace = "com.plusmobileapps.chefmate.${project.name}"
                    }
                """
                                .trimIndent()
                        )
            }

            extensions.configure<KotlinMultiplatformExtension> {
                // Configure the Android library target via the new plugin's DSL,
                // which is exposed on the Kotlin multiplatform extension as `android`.
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                    compileSdk = libs.versions.android.compileSdk.get().toInt()
                    minSdk = libs.versions.android.minSdk.get().toInt()

                    // Pin Android JVM bytecode level. iOS targets are unaffected.
                    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)

                    // Compose Multiplatform's resources system (strings.xml in
                    // commonMain/composeResources/) bundles its compiled `.cvr` files into
                    // the Android AAR via the Android asset/resource pipeline. The new
                    // KMP plugin defaults `androidResources.enable` to `false` (vs `true`
                    // for plain `com.android.library`), which silently drops those
                    // resources and makes `stringResource(...)` throw MissingResourceException
                    // at runtime. Re-enable it so consumers can resolve common strings.
                    androidResources.enable = true

                    // Tests that previously lived in `androidUnitTest` now live in
                    // `androidHostTest` (host-side JVM tests); instrumented tests live in
                    // `androidDeviceTest`. Opt into the host-test compilation so existing
                    // test source sets continue to compile.
                    //
                    // isReturnDefaultValues: Molecule presenters run the Compose runtime in
                    // host tests, which calls android.os.Trace / android.util.Log. Those are
                    // throwing stubs in the host android.jar; return defaults instead.
                    withHostTest { isReturnDefaultValues = true }
                }

                // Configure iOS targets with explicit source set assignment
                val iosArm64Target = iosArm64()
                val iosSimulatorArm64Target = iosSimulatorArm64()

                // Configure framework for iOS
                listOf(iosArm64Target, iosSimulatorArm64Target).forEach { iosTarget ->
                    iosTarget.binaries.framework {
                        baseName = target.name.replaceFirstChar { it.uppercase() }
                        isStatic = true
                    }
                }

                // Configure JVM target
                jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

                // Remove once kotlin.time is stable in 2.3.0
                compilerOptions.optIn.add("kotlin.time.ExperimentalTime")

                // Acknowledge the still-Beta expect/actual classes feature (KT-61573) so
                // modules with `expect`/`actual` classes (e.g. DriverFactory) don't emit a
                // warning on every compile. Drop once the feature is stabilized.
                compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")

                // Default source sets configuration
                sourceSets.apply {
                    val commonMain = getByName("commonMain")
                    val androidMain = getByName("androidMain")
                    val jvmMain = getByName("jvmMain")
                    val iosMain = create("iosMain") {}

                    // Add dependencies to source sets
                    commonMain.dependencies {
                        // Common dependencies can be added here if needed
                        implementation(libs.kermit)
                    }

                    androidMain.dependencies {
                        // Android-specific dependencies can be added here
                    }

                    jvmMain.dependencies {
                        // JVM-specific dependencies can be added here
                    }

                    iosMain.dependencies {}

                    getByName("commonTest").dependencies { implementation(libs.kotlin.test) }
                }
            }
        }
    }
}

/**
 * Registers the watchOS targets on a module. Invoked from [PlusLibraryExtension.enableWatch]'s
 * setter, which runs during configuration — the valid window to add KMP targets. The default Kotlin
 * hierarchy template links both watch targets to `watchosMain` → `appleMain`, so shared
 * Darwin/native code placed in `appleMain` is picked up by iOS and watchOS alike.
 */
private fun Project.registerWatchTargets() {
    val frameworkName = name.replaceFirstChar { it.uppercase() }
    extensions.configure<KotlinMultiplatformExtension> {
        val watchArm64 = watchosArm64()
        val watchSimulatorArm64 = watchosSimulatorArm64()
        listOf(watchArm64, watchSimulatorArm64).forEach { watchTarget ->
            watchTarget.binaries.framework {
                baseName = frameworkName
                isStatic = true
            }
        }
    }
}

@OptIn(ExperimentalComposeLibrary::class)
private fun Project.applyUiTest() {
    extensions.configure<KotlinMultiplatformExtension> {
        val compose = ComposePlugin.Dependencies(project)
        sourceSets.getByName("commonMain").dependencies { api(compose.uiTest) }
    }
}
