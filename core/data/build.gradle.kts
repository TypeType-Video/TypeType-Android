plugins {
    id("typetype.android.library")
    id("typetype.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.typetype.android.core.data"
}

// Migration tests read the exported schemas from the test APK assets.
androidComponents.onVariants { variant ->
    variant.androidTest?.sources?.assets?.addStaticSourceDirectory("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:resources"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.paging)
    implementation(libs.google.tink.android)
    implementation(libs.room)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    implementation(libs.square.retrofit)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.room.testing)
}
