plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.shorts"
}

dependencies {
    implementation(project(":core:playback"))
    implementation(project(":feature:menu"))
    implementation(project(":feature:player"))
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(project(":core:testing"))
}
