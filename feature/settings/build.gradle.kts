plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.settings"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:notifications"))
    implementation(libs.aboutlibraries.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.unifiedpush.connector) { exclude(group = "com.google.crypto.tink", module = "tink") }
}
