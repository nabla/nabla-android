package com.nabla.sdk.core.ui.helpers

import android.content.Context
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.ui.R

internal fun User.initials(context: Context, singleLetter: Boolean = false): String {
    return when (this) {
        is User.Patient -> context.getString(R.string.nabla_display_username_initials_format, username)
        is User.Provider -> {
            if (singleLetter) {
                context.getString(R.string.nabla_display_lastname_initials_format, lastName)
            } else {
                context.getString(R.string.nabla_display_name_initials_format, firstName, lastName)
            }
        }
        User.Unknown -> {
            context.getString(R.string.nabla_display_name_unknown_user_initials)
        }
    }
}

internal fun User.fullName(context: Context): String = when (this) {
    is User.Provider -> context.getString(R.string.nabla_display_name_full_name_format, firstName, lastName)
    is User.Patient -> username
    is User.Unknown -> context.getString(R.string.nabla_display_name_unknown_user)
}

internal fun User.lastNameOrUserName(context: Context): String = when (this) {
    is User.Provider -> lastName
    is User.Patient -> username
    is User.Unknown -> context.getString(R.string.nabla_display_name_unknown_user)
}

internal fun User.fullNameWithPrefix(context: Context): String = when (this) {
    is User.Provider -> {
        val fullName = fullName(context)
        val prefix = prefix
        if (prefix != null && prefix.isNotBlank()) {
            context.getString(R.string.nabla_display_name_full_name_and_prefix, prefix, fullName)
        } else fullName
    }
    else -> fullName(context)
}

// For a doctor with prefix it's Prefix + Last Name (ie: Dr Cayol)
// For a doctor without prefix it's First Name + Last Name (ie: Véronique Cayol)
// Else full name
internal fun User.abbreviatedNameWithPrefix(context: Context): String = when (this) {
    is User.Provider -> {
        val prefix = prefix
        if (prefix != null && prefix.isNotBlank()) {
            context.getString(R.string.nabla_display_name_full_name_and_prefix, prefix, lastNameOrUserName(context))
        } else {
            fullName(context)
        }
    }
    else -> fullName(context)
}

internal fun User.fullNameWithPrefixAndTitle(context: Context): String = when (this) {
    is User.Provider -> {
        val fullNameWithPrefix = fullNameWithPrefix(context)
        if (title.isNullOrBlank()) {
            fullNameWithPrefix
        } else {
            context.getString(R.string.nabla_display_name_with_title, fullNameWithPrefix, title)
        }
    }
    else -> fullNameWithPrefix(context)
}

internal val User.prefix: String?
    get() = (this as? User.Provider)?.prefix
