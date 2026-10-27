plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.channel"
}

dependencies {
    implementation(project(":feature:menu"))
    implementation(project(":feature:search"))
    implementation(project(":core:data"))
    implementation(libs.androidx.navigation.compose)
}
