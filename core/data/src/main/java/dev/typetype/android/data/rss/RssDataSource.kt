package dev.typetype.android.data.rss

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.rss.RssFeed
import dev.typetype.android.domain.rss.RssFeedDraft
import dev.typetype.android.domain.rss.RssFeedSecret

interface RssDataSource {
    suspend fun list(scope: AccountScope): List<RssFeed>
    suspend fun create(scope: AccountScope, draft: RssFeedDraft): RssFeedSecret
    suspend fun update(scope: AccountScope, id: String, draft: RssFeedDraft): RssFeed
    suspend fun setEnabled(scope: AccountScope, id: String, enabled: Boolean): RssFeed
    suspend fun regenerate(scope: AccountScope, id: String): RssFeedSecret
    suspend fun delete(scope: AccountScope, id: String)
}
