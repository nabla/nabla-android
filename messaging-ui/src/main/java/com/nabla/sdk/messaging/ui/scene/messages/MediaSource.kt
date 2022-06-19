package com.nabla.sdk.messaging.ui.scene.messages

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
internal enum class MediaSource : Parcelable {
    CAMERA_PICTURE,
    CAMERA_VIDEO,
    GALLERY,
    DOCUMENT,
}
