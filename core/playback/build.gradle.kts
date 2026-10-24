plugins {
    id("typetype.android.library")
    id("typetype.android.hilt")
}

android {
    namespace = "dev.typetype.android.core.playback"
}

dependencies {
    api(project(":core:data"))
    api(project(":player"))
    implementation(project(":core:resources"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    api(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.datasource.okhttp)
    api(libs.androidx.media3.session)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.uiautomator)
}
