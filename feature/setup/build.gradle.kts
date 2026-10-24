plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.setup"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activity.compose)
}
