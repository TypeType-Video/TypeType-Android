package dev.typetype.android.domain.appupdate

internal object AppUpdatePolicy {
    fun check(
        channel: AppUpdateChannel,
        installedVersionName: String,
        installedVersionCode: Long,
        release: AppUpdateRelease,
    ): AppUpdateCheck {
        release.versionCode?.let { candidateCode ->
            return when {
                candidateCode > installedVersionCode -> AppUpdateCheck.Available(release)
                candidateCode == installedVersionCode && release.versionName == installedVersionName ->
                    AppUpdateCheck.UpToDate
                else -> AppUpdateCheck.NewerBuildInstalled(release)
            }
        }
        if (
            channel == AppUpdateChannel.GITHUB_STABLE &&
            installedVersionName.endsWithBetaOf(release.versionName)
        ) {
            return AppUpdateCheck.NewerBuildInstalled(release)
        }
        val candidate = AppVersion.parse(release.versionName)
        val installed = AppVersion.parse(installedVersionName)
        return if (candidate == null || installed == null || candidate > installed) {
            AppUpdateCheck.Available(release)
        } else {
            AppUpdateCheck.UpToDate
        }
    }

    private fun String.endsWithBetaOf(stableVersion: String): Boolean =
        startsWith("$stableVersion-beta.")

    private data class AppVersion(
        val major: Long,
        val minor: Long,
        val patch: Long,
        val beta: Long?,
    ) : Comparable<AppVersion> {
        override fun compareTo(other: AppVersion): Int {
            compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch)
                .takeIf { it != 0 }?.let { return it }
            return when {
                beta == null && other.beta == null -> 0
                beta == null -> 1
                other.beta == null -> -1
                else -> beta.compareTo(other.beta)
            }
        }

        companion object {
            private val versionPattern = Regex("""^(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?$""")

            fun parse(value: String): AppVersion? {
                val parts = versionPattern.matchEntire(value)?.groupValues ?: return null
                return AppVersion(
                    major = parts[1].toLongOrNull() ?: return null,
                    minor = parts[2].toLongOrNull() ?: return null,
                    patch = parts[3].toLongOrNull() ?: return null,
                    beta = parts[4].takeIf(String::isNotEmpty)?.toLongOrNull(),
                )
            }
        }
    }
}
