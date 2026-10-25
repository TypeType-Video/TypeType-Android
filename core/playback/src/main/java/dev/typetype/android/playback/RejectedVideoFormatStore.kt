package dev.typetype.android.playback

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Video formats a decoder failed on, kept across launches so "Smart" does not pick them again and fail the same
// way each time. A system update can bring new decoders, so the list starts over when the build fingerprint changes.
@Singleton
class RejectedVideoFormatStore @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): Set<String> {
        if (preferences.getString(FINGERPRINT, null) != Build.FINGERPRINT) {
            preferences.edit {
                clear()
                putString(FINGERPRINT, Build.FINGERPRINT)
            }
            return emptySet()
        }
        return preferences.getStringSet(KEYS, emptySet()).orEmpty().toSet()
    }

    fun add(key: String) {
        val keys = load() + key
        preferences.edit { putStringSet(KEYS, keys) }
    }

    private companion object {
        const val FILE = "rejected_video_formats"
        const val KEYS = "keys"
        const val FINGERPRINT = "fingerprint"
    }
}
