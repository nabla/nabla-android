package com.nabla.sdk.core.ui.helpers

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nabla.sdk.core.annotation.NablaInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@NablaInternal
public fun <T> CoroutineScope.launchCollect(
    flowToCollect: Flow<T>,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    collector: FlowCollector<T>,
): Job {
    return this.launch(context, start) {
        flowToCollect.collect(collector)
    }
}

@NablaInternal
public fun <T> LifecycleOwner.launchCollect(
    liveFlowToCollect: LiveFlow<T>,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    minState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit,
): Job {
    return this.lifecycleScope.launch(context, start) {
        liveFlowToCollect.collect(LiveFlow.LiveFlowCollector(lifecycle, minState, collector))
    }
}
