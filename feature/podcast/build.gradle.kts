plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.podcast"
}

dependencies {
    implementation(project(":feature:menu"))
    implementation(libs.androidx.navigation.compose)
}
