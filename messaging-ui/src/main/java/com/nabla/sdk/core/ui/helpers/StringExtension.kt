package com.nabla.sdk.core.ui.helpers

import java.util.Locale

internal fun String.hyphenateUnderscores() = this.replace("_", "\u00ad_")

internal fun String.nullIfEmpty() = if (this.isEmpty()) null else this

internal fun String.nullIfBlank() = if (this.isBlank()) null else this

internal fun String.capitalize(locale: Locale = Locale.getDefault()) = replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
