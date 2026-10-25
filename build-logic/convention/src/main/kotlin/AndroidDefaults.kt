import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

internal const val COMPILE_SDK = 37
internal const val MIN_SDK = 23

// java.time and other newer APIs are desugared for API 23, in every module that uses them.
internal fun CommonExtension.configureAndroidDefaults(project: Project) {
    compileSdk = COMPILE_SDK
    defaultConfig.minSdk = MIN_SDK
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    compileOptions.targetCompatibility = JavaVersion.VERSION_17
    compileOptions.isCoreLibraryDesugaringEnabled = true
    lint.abortOnError = true
    lint.checkReleaseBuilds = false
    lint.warningsAsErrors = true
    // Newer versions appear on their own schedule; a build should not start failing because a day passed.
    lint.disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    // Verifying the TypeType links needs an assetlinks file on the domain, which is a release decision.
    lint.informational += "AppLinkWarning"
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    project.dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("desugaring").get())
        add("androidTestImplementation", libs.findLibrary("androidx-test-runner").get())
    }
}
