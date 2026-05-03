package dev.typetype.android.feature.home

import dev.typetype.android.domain.server.Server

data class HomeState(
    val currentServer: Server? = null,
)
