package com.nabla.sdk.core.domain.boundary

import androidx.annotation.StringRes
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public interface StringResolver {
    public fun resolve(@StringRes resId: Int): String
}
