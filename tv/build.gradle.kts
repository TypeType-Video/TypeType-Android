import java.security.MessageDigest
import java.net.URI

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
}

val releaseInstanceUrl = providers.gradleProperty("typeTypeReleaseInstanceUrl")
    .orElse(providers.environmentVariable("TYPETYPE_RELEASE_INSTANCE_URL"))
    .orElse("")
val releaseStoreFile = providers.gradleProperty("typeTypeReleaseStoreFile")
    .orElse(providers.environmentVariable("TYPETYPE_RELEASE_STORE_FILE"))
    .orElse("")
val releaseStorePassword = providers.gradleProperty("typeTypeReleaseStorePassword")
    .orElse(providers.environmentVariable("TYPETYPE_RELEASE_STORE_PASSWORD"))
    .orElse("")
val releaseKeyAlias = providers.gradleProperty("typeTypeReleaseKeyAlias")
    .orElse(providers.environmentVariable("TYPETYPE_RELEASE_KEY_ALIAS"))
    .orElse("")
val releaseKeyPassword = providers.gradleProperty("typeTypeReleaseKeyPassword")
    .orElse(providers.environmentVariable("TYPETYPE_RELEASE_KEY_PASSWORD"))
    .orElse("")
val sdkVersion = providers.gradleProperty("typeTypeSdkVersion")
    .orElse(providers.environmentVariable("TYPETYPE_SDK_VERSION"))
    .orElse("0.1.0-SNAPSHOT")
val useLocalSdk = providers.gradleProperty("useLocalSdk")
    .orElse(providers.environmentVariable("TYPETYPE_USE_LOCAL_SDK"))
    .map { value ->
        when (value.trim().lowercase()) {
            "true" -> true
            "false" -> false
            else -> error("TYPETYPE_USE_LOCAL_SDK must be true or false")
        }
    }
    .orElse(true)

tasks.register("verifyReleaseInstanceConfiguration") {
    doLast {
        val url = releaseInstanceUrl.get().trim()
        require(url.isNotBlank()) {
            "Release builds require -PtypeTypeReleaseInstanceUrl or TYPETYPE_RELEASE_INSTANCE_URL"
        }
        require(url.startsWith("https://", ignoreCase = true)) {
            "Release instance URL must use HTTPS"
        }
        val parsed = runCatching { URI(url) }.getOrNull()
            ?: error("Release instance URL is not a valid URI")
        require(!parsed.host.isNullOrBlank()) { "Release instance URL must include a host" }
        require(parsed?.query == null && parsed.fragment == null) {
            "Release instance URL must not include a query or fragment"
        }
        require(!parsed.host.equals("beta.typetype.video", ignoreCase = true)) {
            "Release instance URL must not point to the beta instance"
        }
        require(!useLocalSdk.get()) {
            "Release builds require -PuseLocalSdk=false and a published SDK"
        }
        val version = sdkVersion.get().trim()
        require(version.matches(Regex("\\d+\\.\\d+\\.\\d+(?:[-.][0-9A-Za-z.-]+)?"))) {
            "Release builds require an immutable semantic typeTypeSdkVersion"
        }
        require(!version.endsWith("-SNAPSHOT", ignoreCase = true)) {
            "Release builds require an immutable typeTypeSdkVersion"
        }
    }
}

tasks.register("verifyReleaseSigningConfiguration") {
    doLast {
        require(releaseStoreFile.get().isNotBlank()) {
            "Release builds require -PtypeTypeReleaseStoreFile or TYPETYPE_RELEASE_STORE_FILE"
        }
        require(file(releaseStoreFile.get()).isFile) { "The configured release keystore does not exist" }
        require(releaseStorePassword.get().isNotBlank()) {
            "Release builds require -PtypeTypeReleaseStorePassword or TYPETYPE_RELEASE_STORE_PASSWORD"
        }
        require(releaseKeyAlias.get().isNotBlank()) {
            "Release builds require -PtypeTypeReleaseKeyAlias or TYPETYPE_RELEASE_KEY_ALIAS"
        }
        require(releaseKeyPassword.get().isNotBlank()) {
            "Release builds require -PtypeTypeReleaseKeyPassword or TYPETYPE_RELEASE_KEY_PASSWORD"
        }
    }
}

android {
    namespace = "video.typetype.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "video.typetype.tv"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile.get().isNotBlank()) storeFile = file(releaseStoreFile.get())
            if (releaseStorePassword.get().isNotBlank()) storePassword = releaseStorePassword.get()
            if (releaseKeyAlias.get().isNotBlank()) keyAlias = releaseKeyAlias.get()
            if (releaseKeyPassword.get().isNotBlank()) keyPassword = releaseKeyPassword.get()
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            val instanceUrl = providers.gradleProperty("typeTypeInstanceUrl")
                .orElse(providers.environmentVariable("TYPETYPE_INSTANCE_URL"))
                .orElse("https://beta.typetype.video/api")
            buildConfigField("String", "TYPETYPE_INSTANCE_URL", instanceUrl.get().toBuildConfigString())
        }
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "TYPETYPE_INSTANCE_URL", releaseInstanceUrl.get().toBuildConfigString())
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

private fun String.toBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn("verifyReleaseInstanceConfiguration")
    dependsOn("verifyReleaseSigningConfiguration")
}

tasks.register("verifyReleaseArtifact") {
    dependsOn("assembleRelease")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        require(apk.isFile) { "The signed release APK was not produced" }
        val sdkRoot = providers.environmentVariable("ANDROID_HOME").orNull
            ?: providers.environmentVariable("ANDROID_SDK_ROOT").orNull
            ?: error("ANDROID_HOME or ANDROID_SDK_ROOT is required")
        val buildTools = file("$sdkRoot/build-tools").listFiles()
            ?.filter { it.isDirectory && File(it, "apksigner").isFile }
            ?.maxByOrNull { it.name }
            ?: error("No Android build-tools installation with apksigner was found")
        val apksigner = ProcessBuilder(
            File(buildTools, "apksigner").absolutePath,
            "verify",
            "--verbose",
            apk.absolutePath,
        ).inheritIO().start()
        require(apksigner.waitFor() == 0) { "apksigner rejected the release APK" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(apk.readBytes())
            .joinToString("") { byte -> "%02x".format(byte) }
        val report = layout.buildDirectory.file("verification/release-sha256.txt").get().asFile
        report.parentFile.mkdirs()
        report.writeText("$digest  ${apk.name}\n")
        println("Verified signed release APK: ${apk.path}")
        println("SHA-256 report: ${report.path}")
    }
}

dependencies {
    implementation("video.typetype:sdk-android:${sdkVersion.get()}")
    implementation("video.typetype:sdk-media3:${sdkVersion.get()}")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.tv:tv-material:1.0.0")

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
}
