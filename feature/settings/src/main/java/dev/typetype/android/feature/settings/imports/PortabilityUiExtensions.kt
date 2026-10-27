package dev.typetype.android.feature.settings.imports

import dev.typetype.android.domain.imports.PortabilityJobState

internal fun PortabilityJobState.isTerminalUiState(): Boolean =
    this == PortabilityJobState.Completed ||
        this == PortabilityJobState.Failed ||
        this == PortabilityJobState.Cancelled
