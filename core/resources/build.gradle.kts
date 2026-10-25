plugins {
    id("typetype.android.library")
}

android {
    namespace = "dev.typetype.android"
}

dependencies {
    // Shared drawables tint with Material theme attributes such as ?attr/colorOnSurface.
    api(libs.material)
}
