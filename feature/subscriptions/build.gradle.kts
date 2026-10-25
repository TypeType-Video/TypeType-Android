plugins {
    id("typetype.android.feature")
}

android {
    namespace = "dev.typetype.android.feature.subscriptions"
}

dependencies {
    implementation(project(":feature:menu"))
    implementation(project(":feature:library"))
    implementation(project(":core:data"))
}
