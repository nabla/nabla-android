package com.nabla.sdk.core.ui.helpers

import com.nabla.sdk.core.annotation.NablaInternal
import java.util.Locale

@NablaInternal
public fun String.hyphenateUnderscores(): String = this.replace("_", "\u00ad_")

@NablaInternal
public fun String.nullIfEmpty(): String? = if (this.isEmpty()) null else this

@NablaInternal
public fun String.nullIfBlank(): String? = if (this.isBlank()) null else this

@NablaInternal
public fun String.capitalize(locale: Locale = Locale.getDefault()): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
