plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.publicplaylist"
}

dependencies {
    implementation(project(":feature:menu"))
    implementation(libs.androidx.navigation.compose)
}
