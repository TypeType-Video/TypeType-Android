plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.notifications"
}

dependencies {
    implementation(project(":core:data"))
}
