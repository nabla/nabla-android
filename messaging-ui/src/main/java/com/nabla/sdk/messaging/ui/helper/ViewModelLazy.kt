package com.nabla.sdk.messaging.ui.helper

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner

fun createWithFactory(
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null,
    create: (SavedStateHandle) -> ViewModel
): AbstractSavedStateViewModelFactory {
    return object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            @Suppress("UNCHECKED_CAST")// Casting T as ViewModel
            return create.invoke(handle) as T
        }
    }
}