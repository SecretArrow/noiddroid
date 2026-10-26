import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.hilt)
    // Built-in Gradle plugin — no version catalog entry needed.
    jacoco
}

android {
    namespace = "org.secretarrow.noiddroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.secretarrow.noiddroid"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha01"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        ndk {
            // armeabi-v7a omitted — v1 is arm64-only to keep APK small
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-fexceptions", "-frtti")
                arguments +=
                    listOf(
                        "-DANDROID_PLATFORM=android-26",
                        "-DANDROID_STL=c++_shared",
                        "-DNOID_BUILD_MINER=ON",
                    )
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Faster debug builds — skip R8 for subsecond iteration
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            // Crashlytics-free: we ship Sentry in v2
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xjvm-default=all",
                "-Xcontext-receivers",
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes +=
                setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "/META-INF/INDEX.LIST",
                    "/META-INF/io.netty.versions.properties",
                    "/META-INF/DEPENDENCIES",
                    "DebugProbesKt.bin",
                    "kotlin/**",
                )
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            // Per testing-setup skill Step 14: enable Jacoco coverage on
            // unit tests so ./gradlew jacocoTestReport generates HTML at
            // app/build/reports/jacoco/jacocoTestReport/html/index.html
            all {
                it.jacoco {
                    includeNoLocationClasses = true
                    excludes += listOf("jdk.internal.*")
                }
            }
        }
        // Disable animations on instrumented tests for stability
        // (animations = TestsAnimations.DISABLED removed — needs different import path; not critical)
        // Gradle Managed Devices — virtual ATD emulators for E2E
        managedDevices {
            devices {
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                }
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel8") {
                    device = "Pixel 8"
                    apiLevel = 35
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    // Per testing-setup skill Step 14: Jacoco coverage for local JVM tests.
    // ./gradlew :app:jacocoTestReport -> app/build/reports/jacoco/
    jacoco {
        toolVersion = "0.8.11"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += setOf("MissingTranslation", "ExtraTranslation")
        warningsAsErrors = false
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    // Misc
    implementation(libs.aboutlibraries)
    implementation(libs.sentry.android)

    // Java 8+ API desugaring (for time APIs on minSdk 26)
    coreLibraryDesugaring(libs.core.library.desugaring)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.kotlinx.serialization.json)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

ksp {
    arg("dagger.fastInit", "enabled")
    // 'dagger.hilt.android.internal.useAggregatingComponent' was rejected
    // by Hilt 2.52 as not a recognised option — removed.
}

detekt {
    config.setFrom(file("$rootDir/config/detekt.yml"))
    buildUponDefaultConfig = true
    parallel = true
    autoCorrect = true
    allRules = false
    ignoreFailures = true // v1: warn-only — autofix.yml commits real fixes
}

ktlint {
    version = "1.3.1"
    debug = false
    android = true
    outputToConsole = true
    ignoreFailures = true // v1: warn-only — autofix.yml commits real fixes
    enableExperimentalRules = true
    filter {
        exclude { it.path.contains("/generated/") }
        exclude { it.path.contains("/build/") }
    }
}

aboutLibraries {
    offlineMode = true
}
