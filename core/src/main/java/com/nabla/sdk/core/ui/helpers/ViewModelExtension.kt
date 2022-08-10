package com.nabla.sdk.core.ui.helpers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public fun factoryFor(
    builder: () -> ViewModel,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return builder() as T
    }
}
