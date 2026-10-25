package dev.typetype.android.data.feed

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.server.Server

internal fun AccountScope.canUseRecommendations(server: Server?): Boolean =
    !accountId.startsWith("guest:") && server?.recommender?.available == true
