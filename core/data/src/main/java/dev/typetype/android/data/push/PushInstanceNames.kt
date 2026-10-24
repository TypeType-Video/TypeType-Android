package dev.typetype.android.data.push

import dev.typetype.android.data.account.AccountScope

fun pushInstanceName(scope: AccountScope): String = "${scope.serverId}:${scope.accountId}"

fun scopeFromInstanceName(instance: String): AccountScope? {
    val parts = instance.split(":", limit = 2)
    if (parts.size != 2 || parts.any(String::isBlank)) return null
    return AccountScope(serverId = parts[0], accountId = parts[1])
}
