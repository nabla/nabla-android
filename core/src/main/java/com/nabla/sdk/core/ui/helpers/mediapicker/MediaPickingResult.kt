package com.nabla.sdk.core.ui.helpers.mediapicker

import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public sealed class MediaPickingResult<T> {
    @NablaInternal
    public class Success<T>(public val data: T) : MediaPickingResult<T>()

    @NablaInternal
    public class Cancelled<T> : MediaPickingResult<T>()

    @NablaInternal
    public class Failure<T>(public val exception: Exception) : MediaPickingResult<T>()
}
