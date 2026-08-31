package video.typetype.tv.data

import android.content.Context
import java.net.URI
import video.typetype.sdk.android.TypeTypeAndroid
import video.typetype.sdk.core.InstanceId
import video.typetype.sdk.core.TypeTypeClient
import video.typetype.sdk.core.TypeTypeInstance

public object TypeTypeTvClient {
    public fun create(context: Context, baseUrl: String): TypeTypeClient {
        val normalizedBaseUrl = baseUrl.trim()
        require(normalizedBaseUrl.isNotEmpty()) { "TypeType instance URL is not configured" }
        val uri = URI.create(normalizedBaseUrl)
        val instanceKey = instanceKeyFor(normalizedBaseUrl)
        val instanceId = requireNotNull(InstanceId.parse(instanceKey))
        return TypeTypeAndroid.createClient(
            context = context,
            instance = TypeTypeInstance(instanceId, normalizedBaseUrl, name = uri.host ?: "TypeType instance"),
        )
    }
}

internal fun instanceKeyFor(baseUrl: String): String = baseUrl.trim()
    .removePrefix("https://")
    .removePrefix("http://")
    .trimEnd('/')
    .lowercase()
    .replace(Regex("[^a-z0-9_-]"), "_")
