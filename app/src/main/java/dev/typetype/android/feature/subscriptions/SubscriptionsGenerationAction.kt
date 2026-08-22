package dev.typetype.android.feature.subscriptions

internal enum class SubscriptionsGenerationAction { Continue, Replace, Stop }

internal fun subscriptionsGenerationAction(
    currentGeneration: Long?,
    observedGeneration: Long,
    receivedGeneration: Long,
): SubscriptionsGenerationAction = when {
    currentGeneration != observedGeneration -> SubscriptionsGenerationAction.Stop
    receivedGeneration != observedGeneration -> SubscriptionsGenerationAction.Replace
    else -> SubscriptionsGenerationAction.Continue
}
