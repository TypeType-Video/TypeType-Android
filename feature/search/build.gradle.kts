plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.search"
}

dependencies {
    implementation(project(":feature:menu"))
}
