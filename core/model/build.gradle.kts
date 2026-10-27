plugins {
    id("typetype.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.typetype.android.core.model"
}

dependencies {
    api(libs.androidx.paging.common)
    implementation(libs.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.javax.inject)
    testImplementation(libs.junit)
}
