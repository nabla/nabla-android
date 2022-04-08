package com.nabla.sdk.core.ui.helpers.mediapicker

sealed class MediaPickingResult<T> {
    class Success<T>(val data: T) : MediaPickingResult<T>()
    class Cancelled<T> : MediaPickingResult<T>()
    class Failure<T>(val exception: Exception) : MediaPickingResult<T>()
}
