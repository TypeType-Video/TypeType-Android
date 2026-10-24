import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.project

// A screen family: Compose and Hilt, the shared core modules, and the libraries every screen uses.
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("typetype.android.compose")
        pluginManager.apply("typetype.android.hilt")
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        fun lib(alias: String) = libs.findLibrary(alias).get()
        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:resources"))
            add("implementation", project(":core:ui"))
            add("implementation", lib("androidx-compose-material3"))
            add("implementation", lib("androidx-compose-material-icons-core"))
            add("implementation", lib("androidx-compose-material-icons-extended"))
            add("implementation", lib("androidx-lifecycle-runtime-compose"))
            add("implementation", lib("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", lib("coil-compose"))
            add("testImplementation", lib("junit"))
            add("androidTestImplementation", lib("androidx-compose-ui-test-junit4"))
            add("androidTestImplementation", lib("androidx-test-junit"))
            add("androidTestImplementation", lib("androidx-activity-compose"))
            add("debugImplementation", lib("androidx-compose-ui-test-manifest"))
        }
    }
}
