package video.typetype.tv.data

import android.content.Context
import video.typetype.sdk.core.SessionSnapshot

public class TvDownloadStateStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "typetype_tv_download_jobs",
        Context.MODE_PRIVATE,
    )

    public fun read(session: SessionSnapshot): String? = preferences.getString(session.storageKey(), null)

    public fun write(session: SessionSnapshot, jobId: String) {
        preferences.edit().putString(session.storageKey(), jobId).apply()
    }

    public fun clear(session: SessionSnapshot) {
        preferences.edit().remove(session.storageKey()).apply()
    }
}

private fun SessionSnapshot.storageKey(): String = buildString {
    append(instanceId.value)
    append('|')
    append(accountId?.value ?: if (isGuest) "guest" else "default")
}
