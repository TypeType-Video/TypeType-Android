plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.library"
}

dependencies {
    implementation(project(":feature:menu"))
    implementation(project(":feature:search"))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.paging.compose)
}
