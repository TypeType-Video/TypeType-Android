plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.home"
}

dependencies {
    implementation(project(":feature:menu"))
}
