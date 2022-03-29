package com.nabla.sdk.core.ui.helpers

import java.util.Locale

fun String.hyphenateUnderscores() = this.replace("_", "\u00ad_")

fun String.nullIfEmpty() = if (this.isEmpty()) null else this

fun String.nullIfBlank() = if (this.isBlank()) null else this

fun String.capitalize(locale: Locale = Locale.getDefault()) = replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
