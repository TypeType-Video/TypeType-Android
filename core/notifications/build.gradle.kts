plugins {
    id("typetype.android.library")
    id("typetype.android.hilt")
}

android {
    namespace = "dev.typetype.android.core.notifications"
}

dependencies {
    api(project(":core:data"))
    implementation(project(":core:resources"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.unifiedpush.connector) {
        exclude(group = "com.google.crypto.tink", module = "tink")
    }

    testImplementation(libs.junit)
}
