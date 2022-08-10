package com.nabla.sdk.videocall

import android.app.PictureInPictureParams
import android.os.Build
import androidx.activity.ComponentActivity

internal object PictureInPictureCompat {
    fun enterPictureInPictureMode(
        activity: ComponentActivity,
        pictureInPictureParamsFactory: () -> PictureInPictureParams
    ): Boolean {
        return when {
            Build.VERSION.SDK_INT >= 26 -> {
                activity.enterPictureInPictureMode(pictureInPictureParamsFactory())
            }
            else -> false
        }
    }
}
