import java.util.Properties
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

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

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.compose)
    alias(libs.plugins.plusKtfmt)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)

            // commonTest is shared with the Android instrumented test variant
            // (connectedDebugAndroidTest), not the unit test variant (testDebugUnitTest).
            // unitTestVariant gets its own (empty) tree so commonTest UI tests don't get
            // dragged into testDebugUnitTest, where Robolectric isn't initialised.
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.unitTest)
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts.add("-lsqlite3")
            export(libs.arkivanov.decompose.core)
            export(libs.essenty.lifecycle)
            export(libs.essenty.backhandler)
            export(projects.client.root.public)
            export(projects.client.shared)
        }
        iosTarget.binaries.withType(
            org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable::class.java
        ) {
            linkerOpts.add("-lsqlite3")
        }
    }

    jvm()

    // Acknowledge the still-Beta expect/actual classes feature (KT-61573) so modules with
    // `expect`/`actual` classes (e.g. BugsnagStartup) don't warn on every compile. Drop once
    // the feature is stabilized. Mirrors the same flag in KmpLibraryConventionPlugin.
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    sourceSets {
        commonMain.dependencies {
            api(libs.arkivanov.decompose.core)
            api(libs.arkivanov.decompose.compose.extensions)
            api(libs.essenty.lifecycle)
            api(libs.essenty.backhandler)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            api(projects.client.shared)
            api(projects.client.aichat.impl)
            api(projects.client.aichat.public)
            api(projects.client.auth.data.impl)
            api(projects.client.auth.data.public)
            api(projects.client.auth.ui.impl)
            api(projects.client.auth.usecase.impl)
            api(projects.client.cook.impl)
            api(projects.client.cook.public)
            api(projects.client.featureflag.impl)
            api(projects.client.featureflag.public)
            api(projects.client.grocery.autocomplete.impl)
            api(projects.client.grocery.autocomplete.public)
            api(projects.client.grocery.categoryRules.impl)
            api(projects.client.grocery.categoryRules.public)
            api(projects.client.grocery.data.impl)
            api(projects.client.grocery.core.impl)
            api(projects.client.grocery.core.public)
            api(projects.client.meal.data.impl)
            api(projects.client.meal.core.impl)
            api(projects.client.notifications.data.impl)
            api(projects.client.notifications.impl)
            api(projects.client.onboarding.impl)
            api(projects.client.onboarding.public)
            implementation(libs.kotlinx.serialization.json)
            api(projects.client.database.core)
            api(projects.client.root.public)
            api(projects.client.root.impl)
            api(projects.client.bottomnav.impl)
            api(projects.client.browser.impl)
            api(projects.client.recipe.categories.impl)
            api(projects.client.recipe.data.impl)
            api(projects.client.recipe.list.impl)
            api(projects.client.recipe.core.impl)
            api(projects.client.recipe.exporter.impl)
            api(projects.client.recipe.importer.impl)
            api(projects.client.recipebook.data.impl)
            api(projects.client.recipebook.edit.impl)
            api(projects.client.profile.impl)
            api(projects.client.util.impl)
            api(projects.client.settings.impl)
            api(projects.client.settings.root.impl)
            api(projects.client.developerSettings.impl)
            api(projects.client.subscription.impl)
            api(projects.client.subscription.public)
            api(projects.client.toast.impl)
            api(projects.client.toast.public)
            api(projects.client.ui.public)
            api(libs.kermit)
            implementation(libs.supabase.client)
            implementation(libs.supabase.auth)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.bugsnag.kmp)
            implementation(libs.kermit.bugsnag)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.logback)
            implementation(libs.bugsnag.java)
            val fxClassifier = osClassifier()
            val fxVersion = libs.versions.openjfx.get()
            implementation("org.openjfx:javafx-base:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-controls:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-graphics:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-media:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-swing:$fxVersion:$fxClassifier")
            implementation("org.openjfx:javafx-web:$fxVersion:$fxClassifier")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.bugsnag.kmp)
            implementation(libs.kermit.bugsnag)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(projects.client.testing)
            implementation(projects.client.database.testing)
            implementation(projects.client.auth.data.testing)
            implementation(projects.client.aichat.implRobots)
            implementation(projects.client.bottomnav.implRobots)
            implementation(projects.client.browser.implRobots)
            implementation(projects.client.featureflag.testing)
            implementation(projects.client.subscription.testing)
            implementation(projects.client.recipe.categories.implRobots)
            implementation(projects.client.grocery.autocomplete.implRobots)
            implementation(projects.client.grocery.categoryRules.implRobots)
            implementation(projects.client.grocery.core.implRobots)
            implementation(projects.client.recipe.core.implRobots)
            implementation(projects.client.recipe.list.implRobots)
            implementation(projects.client.settings.implRobots)
            implementation(projects.client.settings.root.implRobots)
            implementation(projects.client.profile.implRobots)
            implementation(projects.client.notifications.implRobots)
            implementation(projects.client.onboarding.implRobots)
        }
        val jvmTest by getting {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                implementation(compose.desktop.currentOs)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.sqldelight.drivers.android)
                implementation(libs.androidx.test.core)
            }
        }
    }
}

android {
    namespace = "com.plusmobileapps.chefmate"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties =
        Properties().also { props ->
            if (keystorePropertiesFile.exists()) {
                keystorePropertiesFile.reader().use { props.load(it) }
            }
        }

    signingConfigs {
        create("release") {
            storeFile =
                file(
                    System.getenv("ANDROID_KEYSTORE_FILE")
                        ?: keystoreProperties.getProperty("releaseKeyStore")
                        ?: "release.keystore"
                )
            storePassword =
                System.getenv("ANDROID_KEYSTORE_PASSWORD")
                    ?: keystoreProperties.getProperty("releaseStorePassword")
                    ?: ""
            keyAlias =
                System.getenv("ANDROID_KEY_ALIAS")
                    ?: keystoreProperties.getProperty("releaseKeyAlias")
                    ?: ""
            keyPassword =
                System.getenv("ANDROID_KEY_PASSWORD")
                    ?: keystoreProperties.getProperty("releaseKeyPassword")
                    ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.plusmobileapps.chefmate"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 127
        versionName = "1.9.41"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.android)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

compose.desktop {
    application {
        mainClass = "com.plusmobileapps.chefmate.MainKt"

        // Pass deep link URI as argument when app is launched via URL scheme
        args += listOf()

        // Ship distributables via the release packaging tasks (packageReleasePkg/Msi/Deb) so the
        // requested task name contains "Release". That marker is what flips BuildConfig.IS_DEBUG to
        // false and hides the developer-only UI — see client/shared/build.gradle.kts. Keep
        // R8/ProGuard
        // OFF: this app relies on reflection (Ktor, Supabase, kotlinx.serialization, SQLDelight,
        // Bugsnag, Decompose) and minification would strip classes those libraries look up at
        // runtime.
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            // macOS ships through the Mac App Store as a signed .pkg (Pkg, not Dmg — the direct
            // Developer ID DMG was retired in favor of Store distribution). Msi/Deb are ignored on
            // the OSes they don't apply to, so this one list is correct for all three legs.
            targetFormats(TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Chef Mate"
            packageVersion = "1.9.41"
            description = "Chef Mate - Your AI Cooking Assistant"
            vendor = "Plus Mobile Apps"

            // Staged app resources. The macOS-arm64 subdir gets the sqlite-jdbc native lib
            // (see `extractSqliteJdbcMacDylib` at the bottom of this file) so it ships inside the
            // signed .app instead of being extracted to an unsigned temp file at runtime.
            appResourcesRootDir.set(layout.buildDirectory.dir("appResources"))

            // jdk.unsupported.desktop provides jdk.swing.interop.SwingInterOpUtils, which
            // JavaFX's Swing interop (JFXPanel, used by the desktop browser's WebView) loads at
            // runtime. Without it the jlink/jpackage runtime image omits the class and the browser
            // crashes with NoClassDefFoundError the first time the cursor updates over the WebView
            // (see issue #400). Not needed for `./gradlew run`, which uses the full JDK.
            //
            // java.net.http provides java.net.http.HttpClient, which JavaFX WebView's WebKit
            // network stack (com.sun.webkit.network.NetworkContext.fwkLoad) uses to fetch every
            // page. Without it in the jlink runtime image every page load throws
            // NoClassDefFoundError: java/net/http/HttpRequest$BodyPublisher and the WebView stays a
            // blank white screen (see issue #432). Like #400, only packaged builds are affected —
            // ./gradlew run uses the full JDK, which already has the module.
            //
            // jdk.unsupported exports sun.misc, where sun.misc.Unsafe lives. JavaFX's Marlin
            // rasterizer (com.sun.marlin.OffHeapArray) uses Unsafe for its off-heap buffers.
            // Without it DMarlinRenderingEngine fails to initialize and every shape fill in the
            // WebView throws NoClassDefFoundError from the QuantumRenderer thread, so page content
            // never rasterizes. Note this is a *different* module from jdk.unsupported.desktop
            // above — that one only exports jdk.swing.interop and does not pull this one in.
            //
            // jdk.xml.dom exports the org.w3c.dom.{html,css,stylesheets,xpath} interfaces that
            // javafx.web's com.sun.webkit.dom.* classes implement, so Java-side DOM access
            // resolves. No code here calls WebEngine.getDocument() today, so this one is
            // precautionary rather than a demonstrated fix — it is kept because it was part of the
            // module set the #432 repro was verified against.
            modules(
                "java.sql",
                "java.naming",
                "java.net.http",
                "jdk.jsobject",
                "jdk.unsupported",
                "jdk.unsupported.desktop",
                "jdk.xml.dom",
            )

            // macOS configuration — Mac App Store distribution
            macOS {
                packageVersion = "1.9.41"
                // Matches the iOS app id so macOS joins the same App Store record (Universal
                // Purchase / one "Chef Mate" listing), NOT the Android-style id used above.
                bundleID = "com.plusmobileapps.chefmate.ChefMate"
                dockName = "Chef Mate"
                // Mac App Store build: emits a Store-ready .pkg and makes jpackage apply the
                // App Store signing/sandbox conventions (--mac-app-store).
                // LSApplicationCategoryType
                // is required for Store submission.
                appStore = true
                appCategory = "public.app-category.food-and-drink"
                // The App Store build is arm64-only (jpackage produces a single-arch app with a
                // bundled arm64 JRE). Apple accepts an arm64-only Mac app only if it declares a
                // minimum macOS of 12.0+ (LSMinimumSystemVersion) — otherwise it demands a
                // universal arm64+x86_64 binary. This drops Intel Mac support.
                minimumSystemVersion = "12.0"

                iconFile.set(project.file("src/jvmMain/resources/app-icon.icns"))

                // Declares no non-exempt encryption (matches iosApp/iosApp/Info.plist), so App
                // Store Connect skips the manual export-compliance prompt before each TestFlight
                // build is available to testers.
                //
                // CFBundleURLTypes registers the `chefmate://` custom scheme so LaunchServices
                // routes `chefmate://…` opens to this app (the invite-email links land on the web
                // page, which bounces the browser to this scheme — see the chefmate-site
                // /notifications redirect). macOS delivers the URL as an Apple Event, caught by the
                // `Desktop.setOpenURIHandler` registered in main.kt, not as a command-line arg.
                // Windows/Linux register the same scheme at runtime instead (SchemeRegistrar),
                // since
                // jpackage has no equivalent for them.
                infoPlist {
                    extraKeysRawXml =
                        """
                        <key>ITSAppUsesNonExemptEncryption</key>
                        <false/>
                        <key>CFBundleURLTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleURLName</key>
                                <string>com.plusmobileapps.chefmate.ChefMate</string>
                                <key>CFBundleURLSchemes</key>
                                <array>
                                    <string>chefmate</string>
                                </array>
                            </dict>
                        </array>
                        """
                            .trimIndent()
                }

                // App Sandbox is mandatory for the Mac App Store. The app and the bundled JRE are
                // signed as separate nested bundles, so each gets its own entitlements: the app
                // opts into the sandbox + the capabilities it actually uses (network client, user-
                // selected files); the runtime inherits the sandbox so the JVM stays contained.
                entitlementsFile.set(rootProject.file("packaging/macos/entitlements.plist"))
                runtimeEntitlementsFile.set(
                    rootProject.file("packaging/macos/runtime-entitlements.plist")
                )

                // Mac App Store provisioning profiles. Fetched by Fastlane match in CI and passed
                // by
                // path via env vars; unset for local/unsigned builds. Both are registered against
                // the same App ID — the runtime profile signs the nested JRE bundle.
                System.getenv("MACOS_PROVISIONING_PROFILE")?.let {
                    provisioningProfile.set(rootProject.file(it))
                }
                System.getenv("MACOS_RUNTIME_PROVISIONING_PROFILE")?.let {
                    runtimeProvisioningProfile.set(rootProject.file(it))
                }

                // Sign with the App Store distribution identity ("Apple Distribution: …") when one
                // is
                // provided via env var (resolved by the Fastlane mac lane in CI). Left unset for
                // local/unsigned builds. jpackage derives the matching "3rd Party Mac Developer
                // Installer" identity from the keychain to sign the .pkg installer.
                signing {
                    sign.set(System.getenv("MACOS_SIGN_IDENTITY") != null)
                    identity.set(System.getenv("MACOS_SIGN_IDENTITY"))
                }
            }

            // Linux configuration
            linux {
                packageName = "chef-mate"
                shortcut = true
                debMaintainer = "support@plusmobileapps.com"
                menuGroup = "Utility"
                appCategory = "Utility"

                iconFile.set(project.file("src/jvmMain/resources/app-icon.png"))
            }

            // Windows configuration
            windows {
                shortcut = true
                menu = true
                menuGroup = "Chef Mate"
                upgradeUuid = "18159995-d967-4CD2-8885-77BFE3B59F98"
            }
        }
    }
}

// --- Bundle the sqlite-jdbc macOS native library into the signed app image ---
// The Mac App Store build is sandboxed, which forbids loading code that isn't part of the signed
// bundle. sqlite-jdbc (pulled by SQLDelight's JVM driver) otherwise extracts its native .dylib to a
// temp dir at runtime and dlopen()s it, which the sandbox blocks ("could not verify … free of
// malware"). Extract the arm64 .dylib from the sqlite-jdbc jar into the Compose app-resources dir
// so
// jpackage code-signs it into the bundle; DriverFactory.jvm.kt then points sqlite-jdbc at this copy
// (org.sqlite.lib.path) so it loads the signed lib instead of extracting an unsigned one.
val sqliteJdbcNative: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    // sqldelight-drivers-jvm (app.cash.sqldelight:sqlite-driver) pulls org.xerial:sqlite-jdbc.
    sqliteJdbcNative(libs.sqldelight.drivers.jvm)
}

val extractSqliteJdbcMacDylib by
    tasks.registering(Sync::class) {
        val sqliteJdbcJars =
            sqliteJdbcNative.incoming
                .artifactView {
                    componentFilter {
                        it is ModuleComponentIdentifier && it.moduleIdentifier.name == "sqlite-jdbc"
                    }
                }
                .files
        from(sqliteJdbcJars.map { zipTree(it) }) {
            include("org/sqlite/native/Mac/aarch64/libsqlitejdbc.dylib")
            // Flatten into the Compose macos-arm64 resources dir (staged into the app for that
            // target).
            eachFile { path = "macos-arm64/libsqlitejdbc.dylib" }
            includeEmptyDirs = false
        }
        into(layout.buildDirectory.dir("appResources"))
    }

// Ensure the native lib is staged before Compose copies app resources into the image.
tasks
    .matching { it.name == "prepareAppResources" }
    .configureEach {
        dependsOn(extractSqliteJdbcMacDylib)
    }

// --- Bundle JavaFX's macOS native libraries next to the app jars in the signed app image ---
// The desktop browser is a JavaFX WebView (see browser/public .../PlatformWebView.jvm.kt), whose
// native code ships *inside* the `org.openjfx:javafx-*:mac-aarch64` jars. When the WebView opens,
// JavaFX's NativeLibLoader loads each `.dylib`: it first looks next to the JavaFX jars
// (loadLibraryFullPath); only if that misses does it extract the lib from the jar to an unsigned
// cache dir and dlopen() it. The Mac App Store sandbox forbids loading code that isn't part of the
// signed bundle, so that extracted copy is blocked ("could not verify … free of malware") and
// opening the browser fails on the Store/TestFlight build (libprism_es2.dylib).
//
// Fix (same idea as the sqlite-jdbc block above, but JavaFX searches next to its jars rather than a
// resources dir): extract the JavaFX dylibs and hand them to jpackage's `files` input so Compose
// copies them — signed via MacJarSignFileCopyingProcessor, exactly like the bundled skiko native —
// into the app-image libs dir ($APPDIR) next to the JavaFX jars. loadLibraryFullPath then loads the
// signed copies and JavaFX never extracts. macOS-only; Linux/Windows builds aren't sandboxed and
// load JavaFX natives normally, and `./gradlew run` uses the full JDK/JavaFX off the module path.
val isMacBuildHost =
    System.getProperty("os.name").orEmpty().lowercase().let {
        it.contains("mac") || it.contains("darwin")
    }

if (isMacBuildHost) {
    val javafxMacNatives: Configuration by configurations.creating {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    val fxClassifier = osClassifier()
    val fxVersion = libs.versions.openjfx.get()
    dependencies {
        // The same set of JavaFX modules pulled by jvmMain; only some ship dylibs (graphics: prism/
        // glass/font, web: jfxwebkit, media: jfxmedia) but resolving all keeps this in lockstep.
        listOf("base", "controls", "graphics", "media", "swing", "web").forEach { module ->
            javafxMacNatives("org.openjfx:javafx-$module:$fxVersion:$fxClassifier")
        }
    }

    val extractJavaFxMacDylibs by
        tasks.registering(Sync::class) {
            from(javafxMacNatives.map { zipTree(it) }) {
                include("**/*.dylib")
                // The dylibs sit at the jar root; flatten defensively so they land directly in the
                // output dir (and therefore in $APPDIR, next to the jars) regardless of jar layout.
                eachFile { path = name }
                includeEmptyDirs = false
            }
            into(layout.buildDirectory.dir("javafxNatives"))
        }

    // Feed the extracted dylibs into the app-image assembly (createDistributable /
    // createReleaseDistributable). Compose's prepareWorkingDir copies each non-jar `files` entry
    // into the libs dir under its own name and signs it — how the skiko native gets signed in
    // $APPDIR. Not the pkg/dmg packaging tasks: those wrap the already-built app image.
    tasks
        .matching { it.name == "createDistributable" || it.name == "createReleaseDistributable" }
        .withType<AbstractJPackageTask>()
        .configureEach {
            // Add the extracted dylibs as individual files (a file tree), not the task's output
            // directory — prepareWorkingDir copies each `files` entry as-is, so a bare directory
            // would be copied whole instead of the loose dylibs landing next to the jars.
            files.from(extractJavaFxMacDylibs.map { fileTree(it.destinationDir) })
        }
}
