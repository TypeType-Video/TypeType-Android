plugins {
    id("typetype.android.library")
}

android {
    namespace = "dev.typetype.player"
}

dependencies {
    api(libs.androidx.media3.exoplayer)
    testImplementation(libs.junit)
}
