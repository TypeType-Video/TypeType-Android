plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.player"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:playback"))
    implementation(project(":feature:menu"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.paging.compose)
    implementation(libs.jsoup)
    androidTestImplementation(project(":core:testing"))
}
