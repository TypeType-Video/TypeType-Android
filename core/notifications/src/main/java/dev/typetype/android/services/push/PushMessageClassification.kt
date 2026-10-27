package dev.typetype.android.services.push

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.push.scopeFromInstanceName
import dev.typetype.android.domain.push.PushPayload
import dev.typetype.android.domain.push.parsePushPayload

internal sealed interface PushMessageClassification {
    data class Accepted(val scope: AccountScope, val payload: PushPayload) : PushMessageClassification

    data class Rejected(val scope: AccountScope?, val failureCode: String) : PushMessageClassification
}

internal fun classifyPushMessage(instance: String, content: ByteArray): PushMessageClassification {
    val scope = scopeFromInstanceName(instance)
        ?: return PushMessageClassification.Rejected(null, "unknown_instance")
    val payload = parsePushPayload(content)
        ?: return PushMessageClassification.Rejected(scope, "invalid_payload")
    if (payload.version != SUPPORTED_PAYLOAD_VERSION) {
        return PushMessageClassification.Rejected(scope, "unsupported_payload_version")
    }
    if (payload.eventType != NEW_VIDEO_EVENT) {
        return PushMessageClassification.Rejected(scope, "unsupported_event_type")
    }
    if (payload.accountId != scope.accountId) {
        return PushMessageClassification.Rejected(scope, "account_mismatch")
    }
    return PushMessageClassification.Accepted(scope, payload)
}

private const val SUPPORTED_PAYLOAD_VERSION = 1
private const val NEW_VIDEO_EVENT = "subscription_new_video"
