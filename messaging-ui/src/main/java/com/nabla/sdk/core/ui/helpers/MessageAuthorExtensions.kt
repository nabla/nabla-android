package com.nabla.sdk.core.ui.helpers

import android.content.Context
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.SystemUser
import com.nabla.sdk.messaging.ui.R

internal fun Provider.initials(context: Context, singleLetter: Boolean = false): String {
    return if (singleLetter) {
        context.getString(R.string.nabla_display_lastname_initials_format, lastName)
    } else {
        context.getString(
            R.string.nabla_display_name_initials_format,
            firstName,
            lastName
        )
    }
}

internal fun SystemUser.initials(): String {
    return name.firstOrNull()?.toString() ?: "?"
}

internal fun Provider.fullName(context: Context): String {
    return context.getString(R.string.nabla_display_name_full_name_format, firstName, lastName)
}

internal fun Provider.fullNameWithPrefix(context: Context): String {
    val fullName = fullName(context)
    val prefix = prefix
    return if (prefix != null && prefix.isNotBlank()) {
        context.getString(R.string.nabla_display_name_full_name_and_prefix, prefix, fullName)
    } else fullName
}

// For a doctor with prefix it's Prefix + Last Name (ie: Dr Cayol)
// For a doctor without prefix it's First Name + Last Name (ie: Véronique Cayol)
// Else full name
internal fun Provider.abbreviatedNameWithPrefix(context: Context): String {
    val prefix = prefix
    return if (prefix != null && prefix.isNotBlank()) {
        context.getString(
            R.string.nabla_display_name_full_name_and_prefix,
            prefix,
            lastName
        )
    } else {
        fullName(context)
    }
}
