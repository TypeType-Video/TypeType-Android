package dev.typetype.android.domain.recommendations

interface RecommenderSettingsRepository {
    suspend fun enabled(): Result<Boolean>
    suspend fun setEnabled(enabled: Boolean): Result<Boolean>
}
