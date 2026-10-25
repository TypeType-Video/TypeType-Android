package dev.typetype.android.data.network.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class PushDeviceRegistrationRequestDtoTest {
    @Test
    fun registrationJsonIncludesWebPushSubscriptionKeys() {
        val request = PushDeviceRegistrationRequestDto(
            deviceId = "device-1",
            endpoint = "https://push.example/subscription",
            p256dh = "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4",
            auth = "BTBZMqHH6r4Tts7J_aSIgg",
        )

        val json = Json.parseToJsonElement(Json.encodeToString(request)).jsonObject

        assertEquals(request.endpoint, json["endpoint"]?.jsonPrimitive?.content)
        assertEquals(request.p256dh, json["p256dh"]?.jsonPrimitive?.content)
        assertEquals(request.auth, json["auth"]?.jsonPrimitive?.content)
    }
}
