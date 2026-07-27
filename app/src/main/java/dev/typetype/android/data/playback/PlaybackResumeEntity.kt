package dev.typetype.android.data.playback

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.domain.playback.PlaybackResume

@Entity(
    tableName = "playback_resume",
    primaryKeys = ["serverId", "accountId"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["serverId", "accountId"],
            childColumns = ["serverId", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["serverId", "accountId"])],
)
data class PlaybackResumeEntity(
    val serverId: String,
    val accountId: String,
    val videoUrl: String,
    val positionMillis: Long,
    val updatedAtMillis: Long,
) {
    fun toDomain(): PlaybackResume = PlaybackResume(
        serverId = serverId,
        accountId = accountId,
        videoUrl = videoUrl,
        positionMillis = positionMillis,
        updatedAtMillis = updatedAtMillis,
    )

    companion object {
        fun fromDomain(resume: PlaybackResume): PlaybackResumeEntity = PlaybackResumeEntity(
            serverId = resume.serverId,
            accountId = resume.accountId,
            videoUrl = resume.videoUrl,
            positionMillis = resume.positionMillis,
            updatedAtMillis = resume.updatedAtMillis,
        )
    }
}
