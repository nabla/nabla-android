package com.nabla.sdk.core.ui.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> FlowCollector<T>.emitIn(
    scope: CoroutineScope,
    value: T,
    context: CoroutineContext = EmptyCoroutineContext,
) {
    scope.launch(context) {
        this@emitIn.emit(value)
    }
}
