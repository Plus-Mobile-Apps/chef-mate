package com.plusmobileapps.chefmate.convention

import com.plusmobileapps.chefmate.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class TestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyTesting()
    }
}

fun Project.applyTesting(enableDatabaseTesting: Boolean = false) {
    pluginManager.apply(libs.plugins.ksp.get().pluginId)
    pluginManager.apply(libs.plugins.mokkery.get().pluginId)
    linkSwiftCompatibilityLibsIntoIosTests()

    extensions.configure<KotlinMultiplatformExtension> {
        if (enableDatabaseTesting) {
            targets.withType(
                org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java
            ) {
                binaries.all { linkerOpts("-lsqlite3") }
            }
        }

        sourceSets.apply {
            val commonTest = getByName("commonTest")

            commonTest.dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.kotest.assertions)
                implementation(project(":client:testing"))
                if (enableDatabaseTesting) {
                    implementation(project(":client:database:testing"))
                }
            }
        }

        tasks.withType<Test>().configureEach { useJUnitPlatform() }
    }
}
