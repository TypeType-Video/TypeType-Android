plugins {
    id("typetype.android.compose")
    id("typetype.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.typetype.android.core.ui"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:resources"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.kotlinx.serialization)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
