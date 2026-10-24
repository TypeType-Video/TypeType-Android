import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

internal const val COMPILE_SDK = 37
internal const val MIN_SDK = 23

internal fun CommonExtension.configureAndroidDefaults() {
    compileSdk = COMPILE_SDK
    defaultConfig.minSdk = MIN_SDK
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    compileOptions.targetCompatibility = JavaVersion.VERSION_17
    lint.abortOnError = true
    lint.checkReleaseBuilds = false
}
