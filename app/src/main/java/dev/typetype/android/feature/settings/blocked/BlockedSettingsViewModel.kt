package dev.typetype.android.feature.settings.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.actions.BlockedItem
import dev.typetype.android.domain.actions.BlockedKeyword
import dev.typetype.android.domain.actions.VideoActionsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedState(
    val channels: List<BlockedItem> = emptyList(),
    val videos: List<BlockedItem> = emptyList(),
    val keywords: List<BlockedKeyword> = emptyList(),
    val keywordInput: String = "",
    val isAddingKeyword: Boolean = false,
    val keywordError: String? = null,
)

private data class KeywordOperationState(
    val input: String = "",
    val isAdding: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class BlockedSettingsViewModel @Inject constructor(
    private val videoActionsRepository: VideoActionsRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {

    private val keywordOperation = MutableStateFlow(KeywordOperationState())

    val state = combine(
        videoActionsRepository.observeBlockedChannels(),
        videoActionsRepository.observeBlockedVideos(),
        videoActionsRepository.observeBlockedKeywords(),
        keywordOperation,
    ) { channels, videos, keywords, operation ->
        BlockedState(
            channels = channels,
            videos = videos,
            keywords = keywords,
            keywordInput = operation.input,
            isAddingKeyword = operation.isAdding,
            keywordError = operation.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockedState())

    init {
        viewModelScope.launch { videoActionsRepository.refreshBlocked() }
    }

    fun unblockChannel(url: String) {
        viewModelScope.launch { videoActionsRepository.unblockChannel(url) }
    }

    fun unblockVideo(url: String) {
        viewModelScope.launch { videoActionsRepository.unblockVideo(url) }
    }

    fun onKeywordChange(value: String) {
        keywordOperation.update { it.copy(input = value.take(100), error = null) }
    }

    fun addKeyword() {
        val keyword = keywordOperation.value.input.trim()
        if (keyword.isEmpty() || keywordOperation.value.isAdding) return
        viewModelScope.launch {
            keywordOperation.update { it.copy(isAdding = true, error = null) }
            videoActionsRepository.blockKeyword(keyword).fold(
                onSuccess = {
                    keywordOperation.value = KeywordOperationState()
                },
                onFailure = { failure ->
                    keywordOperation.update {
                        it.copy(
                            isAdding = false,
                            error = errorMapper.message(
                                failure = failure,
                                fallbackRes = R.string.settings_blocked_keyword_add_failed,
                            ),
                        )
                    }
                },
            )
        }
    }

    fun unblockKeyword(keyword: String) {
        viewModelScope.launch {
            videoActionsRepository.unblockKeyword(keyword).onFailure { failure ->
                keywordOperation.update {
                    it.copy(
                        error = errorMapper.message(
                            failure = failure,
                            fallbackRes = R.string.settings_blocked_keyword_remove_failed,
                        ),
                    )
                }
            }
        }
    }
}
