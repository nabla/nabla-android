package com.nabla.sdk.core.ui.helpers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal fun factoryFor(
    builder: () -> ViewModel,
) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return builder() as T
    }
}
