import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class KspLock : BuildService<BuildServiceParameters.None>

private val hiltEntryPoints = listOf("@HiltViewModel", "@AndroidEntryPoint", "@HiltAndroidApp", "@Module", "@Inject")

// KSP tasks that run at the same time in one Gradle daemon can fail or silently generate nothing; the app
// then builds without those Hilt bindings and crashes at runtime. Run them one at a time, and fail the
// build when a variant with Hilt code produced no Java at all.
internal fun Project.guardHiltKsp() {
    val lock = gradle.sharedServices.registerIfAbsent("kspLock", KspLock::class.java) {
        maxParallelUsages.set(1)
    }
    val mainSources = fileTree("src/main") { include("**/*.kt") }
    val modulePath = path
    tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }.configureEach {
        usesService(lock)
        if (name.contains("Test")) return@configureEach
        val variant = name.removePrefix("ksp").removeSuffix("Kotlin").replaceFirstChar(Char::lowercase)
        val generatedJava = layout.buildDirectory.dir("generated/ksp/$variant/java")
        doLast {
            val usesHilt = mainSources.files.any { file -> file.useLines { lines -> lines.any { line -> hiltEntryPoints.any(line::contains) } } }
            val generated = generatedJava.get().asFileTree.matching { include("**/*.java") }
            if (usesHilt && generated.isEmpty) {
                throw GradleException("KSP generated no Hilt sources for $modulePath ($variant); the bindings would be missing")
            }
        }
    }
}
