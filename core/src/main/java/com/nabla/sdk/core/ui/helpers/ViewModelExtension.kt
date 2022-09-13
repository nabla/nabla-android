package com.nabla.sdk.core.ui.helpers

import androidx.fragment.app.Fragment
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.InternalException

@NablaInternal
public fun factoryFor(
    builder: () -> ViewModel,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return builder() as? T ?: throw InternalException(IllegalStateException("ViewModel factory is not of type $modelClass"))
    }
}

@NablaInternal
public fun <VM : ViewModel> Fragment.savedStateFactoryFor(
    builder: (SavedStateHandle) -> VM,
): AbstractSavedStateViewModelFactory {
    return object : AbstractSavedStateViewModelFactory(this, arguments) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle,
        ): T {
            return builder(handle) as? T ?: throw InternalException(IllegalStateException("ViewModel factory is not of type $modelClass"))
        }
    }
}
