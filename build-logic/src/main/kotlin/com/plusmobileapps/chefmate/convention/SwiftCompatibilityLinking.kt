package com.plusmobileapps.chefmate.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

/**
 * Lets iOS test executables find the Swift back-compat static libraries (`swiftCompatibility56`,
 * `swiftCompatibilityConcurrency`, …) that RevenueCat's Swift code auto-links.
 *
 * App frameworks are linked by Xcode, which supplies these. Kotlin/Native test executables are
 * linked by Gradle outside Xcode and otherwise fail with "library 'swiftCompatibility56' not found"
 * in any module whose test binary pulls in purchases-kmp — directly or through
 * `:client:subscription:public`. Harmless for modules that don't. No-op off macOS.
 */
fun Project.linkSwiftCompatibilityLibsIntoIosTests() {
    if (!System.getProperty("os.name").orEmpty().startsWith("Mac")) return
    val developerDir =
        providers.exec { commandLine("xcode-select", "-p") }.standardOutput.asText.map { it.trim() }
    extensions.configure<KotlinMultiplatformExtension> {
        targets.withType(KotlinNativeTarget::class.java).configureEach {
            val sdk =
                when (name) {
                    "iosSimulatorArm64",
                    "iosX64" -> "iphonesimulator"
                    "iosArm64" -> "iphoneos"
                    else -> return@configureEach
                }
            binaries.withType(TestExecutable::class.java).configureEach {
                linkerOpts(
                    "-L${developerDir.get()}/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$sdk"
                )
            }
        }
    }
}
