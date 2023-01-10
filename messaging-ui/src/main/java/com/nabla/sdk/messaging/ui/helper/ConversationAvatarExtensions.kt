package com.nabla.sdk.messaging.ui.helper

import androidx.core.view.isVisible
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.components.NablaAvatarView

internal fun NablaAvatarView.bindConversationAvatar(
    maybeConversationPictureUri: Uri?,
    firstProvider: Provider?,
    displayAvatar: Boolean,
) {
    when {
        maybeConversationPictureUri != null -> loadAvatar(maybeConversationPictureUri, null)
        firstProvider != null -> loadAvatar(firstProvider)
        else -> displayUnicolorPlaceholder()
    }
    isVisible = displayAvatar
}
