package com.nabla.sdk.core.ui.helpers

import android.content.Context
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.ui.R

fun User?.initials(context: Context, singleLetter: Boolean = false): String {
    return when (this) {
        is User.Patient -> context.getString(R.string.display_username_initials_format, username)
        is User.Provider -> {
            if (singleLetter) {
                context.getString(R.string.display_lastname_initials_format, lastName)
            } else {
                context.getString(R.string.display_name_initials_format, firstName, lastName)
            }
        }
        null -> {
            context.getString(R.string.display_name_unknown_user_initials)
        }
    }
}
