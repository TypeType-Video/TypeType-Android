package dev.typetype.android.domain.usersettings

fun CaptionStyles.resolvedFontFamily(): String =
    fontFamily.ifBlank { CaptionStyles.DEFAULT_FONT_FAMILY }

fun CaptionStyles.resolvedFontSize(): String =
    fontSize.ifBlank { CaptionStyles.DEFAULT_FONT_SIZE }

fun CaptionStyles.resolvedTextColor(): String =
    textColor.ifBlank { CaptionStyles.DEFAULT_TEXT_COLOR }

fun CaptionStyles.resolvedTextOpacity(): String =
    textOpacity.ifBlank { CaptionStyles.DEFAULT_TEXT_OPACITY }

fun CaptionStyles.resolvedTextShadow(): String =
    textShadow.ifBlank { CaptionStyles.DEFAULT_TEXT_SHADOW }

fun CaptionStyles.resolvedTextBackground(): String =
    textBackground.ifBlank { CaptionStyles.DEFAULT_TEXT_BACKGROUND }

fun CaptionStyles.resolvedTextBackgroundOpacity(): String =
    textBackgroundOpacity.ifBlank { CaptionStyles.DEFAULT_TEXT_BACKGROUND_OPACITY }

fun CaptionStyles.resolvedDisplayBackground(): String =
    displayBackground.ifBlank { CaptionStyles.DEFAULT_DISPLAY_BACKGROUND }

fun CaptionStyles.resolvedDisplayBackgroundOpacity(): String =
    displayBackgroundOpacity.ifBlank { CaptionStyles.DEFAULT_DISPLAY_BACKGROUND_OPACITY }
