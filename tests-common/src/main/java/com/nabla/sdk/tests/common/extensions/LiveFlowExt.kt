package com.nabla.sdk.tests.common.extensions

import com.nabla.sdk.core.ui.helpers.LiveFlow
import kotlinx.coroutines.flow.MutableSharedFlow

suspend fun <T> LiveFlow<T>.collectToFlow(collectorFlow: MutableSharedFlow<T>) {
    collect(
        object : LiveFlow.BaseLiveFlowCollector<T>() {
            override suspend fun emit(value: T) = collectorFlow.emit(value)
        },
    )
}
