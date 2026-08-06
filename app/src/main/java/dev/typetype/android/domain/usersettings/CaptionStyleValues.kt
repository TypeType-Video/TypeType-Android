package dev.typetype.android.domain.usersettings

internal fun CaptionStyles.resolvedFontFamily(): String =
    fontFamily.ifBlank { CaptionStyles.DEFAULT_FONT_FAMILY }

internal fun CaptionStyles.resolvedFontSize(): String =
    fontSize.ifBlank { CaptionStyles.DEFAULT_FONT_SIZE }

internal fun CaptionStyles.resolvedTextColor(): String =
    textColor.ifBlank { CaptionStyles.DEFAULT_TEXT_COLOR }

internal fun CaptionStyles.resolvedTextOpacity(): String =
    textOpacity.ifBlank { CaptionStyles.DEFAULT_TEXT_OPACITY }

internal fun CaptionStyles.resolvedTextShadow(): String =
    textShadow.ifBlank { CaptionStyles.DEFAULT_TEXT_SHADOW }

internal fun CaptionStyles.resolvedTextBackground(): String =
    textBackground.ifBlank { CaptionStyles.DEFAULT_TEXT_BACKGROUND }

internal fun CaptionStyles.resolvedTextBackgroundOpacity(): String =
    textBackgroundOpacity.ifBlank { CaptionStyles.DEFAULT_TEXT_BACKGROUND_OPACITY }

internal fun CaptionStyles.resolvedDisplayBackground(): String =
    displayBackground.ifBlank { CaptionStyles.DEFAULT_DISPLAY_BACKGROUND }

internal fun CaptionStyles.resolvedDisplayBackgroundOpacity(): String =
    displayBackgroundOpacity.ifBlank { CaptionStyles.DEFAULT_DISPLAY_BACKGROUND_OPACITY }
