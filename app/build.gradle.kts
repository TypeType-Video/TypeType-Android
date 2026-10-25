import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("typetype.android.application")
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    id("typetype.android.hilt")
    alias(libs.plugins.aboutlibraries)
}

val keystoreProperties = Properties()
val keystoreFileExists = rootProject.file("keystore.properties").exists()
if (keystoreFileExists) {
    keystoreProperties.load(rootProject.file("keystore.properties").inputStream())
}

android {
    namespace = "dev.typetype.android.app"

    defaultConfig {
        applicationId = "dev.typetype.android"
        versionCode = 11104
        versionName = "1.11.0-beta.4"
    }

    signingConfigs {
        if (keystoreFileExists) {
            create("release") {
                storeFile = keystoreProperties["storeFile"]?.let { file(it as String) }
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")?.takeIf { it.storeFile != null }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "TypeType Debug")
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            javaParameters = true
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
        compose = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        generateLocaleConfig = true
    }
}

androidComponents.onVariants { variant ->
    if (variant.name == "benchmarkRelease" || variant.name == "nonMinifiedRelease") {
        variant.applicationId.set("dev.typetype.android.benchmark")
    }
    if (variant.name == "benchmarkRelease") {
        variant.sources.manifests.addStaticManifestFile(
            "src/benchmarkRelease/AndroidManifest.xml",
        )
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:notifications"))
    implementation(project(":core:playback"))
    implementation(project(":core:resources"))
    implementation(project(":core:ui"))
    implementation(project(":feature:menu"))
    implementation(project(":feature:home"))
    implementation(project(":feature:search"))
    implementation(project(":feature:podcast"))
    implementation(project(":feature:publicplaylist"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:setup"))
    implementation(project(":feature:channel"))
    implementation(project(":feature:library"))
    implementation(project(":feature:subscriptions"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:player"))
    implementation(project(":feature:shorts"))
    implementation(project(":player"))
    baselineProfile(project(":baseline-profile"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime)
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espressoCore)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.okhttp.mockwebserver)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.aboutlibraries.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.jsoup)
    implementation(libs.unifiedpush.connector) {
        exclude(group = "com.google.crypto.tink", module = "tink")
    }
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.coil.network.okhttp)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization)

    implementation(libs.androidx.paging)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.session)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
