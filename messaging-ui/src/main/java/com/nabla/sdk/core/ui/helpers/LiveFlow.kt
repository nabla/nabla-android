package com.nabla.sdk.core.ui.helpers

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.whenStateAtLeast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * A [Lifecycle] aware Flow that buffers values until at least one collector is subscribed with a
 * lifecycle that is at least [Lifecycle.State.STARTED]
 *
 * Sample usage:
 *
 * ```
 * class MyViewModel : ViewModel() {
 *     private val errorEventsMutableFlow = LiveMutableSharedFlow<String>()
 *     val errorEventsFlow: LiveFlow<String> = errorEventsMutableFlow
 * }
 *
 * class MyFragment : Fragment() {
 *     private val viewModel: ViewModel by viewModels()
 *
 *     override fun onViewCreated(binding: FragmentBinding, savedInstanceState: Bundle?) {
 *         viewLifecycleOwner.launchCollect(viewModel.errorEventsFlow) { value ->
 *             // Guaranteed to be called only when view is started
 *         }
 *     }
 * }
 * ```
 *
 * NB: If multiple subscribers are collecting this flow at the same time, only the first one will get
 * the events buffered while there was nobody subscribed
 */
interface LiveFlow<T> {
    suspend fun collect(liveCollector: BaseLiveFlowCollector<T>)

    abstract class BaseLiveFlowCollector<T> : FlowCollector<T>

    class LiveFlowCollector<T>(
        private val lifecycle: Lifecycle,
        private val action: suspend (T) -> Unit,
    ) : BaseLiveFlowCollector<T>() {

        override suspend fun emit(value: T) {
            lifecycle.whenStateAtLeast(Lifecycle.State.STARTED) {
                action(value)
            }
        }
    }
}

/**
 * Mutable implementation of [LiveFlow]
 */
class MutableLiveFlow<T> : LiveFlow<T>, FlowCollector<T> {
    private val wrapped = Channel<T>(Channel.BUFFERED)

    @OptIn(InternalCoroutinesApi::class)
    override suspend fun collect(liveCollector: LiveFlow.BaseLiveFlowCollector<T>) {
        wrapped.receiveAsFlow().collect(liveCollector)
    }

    override suspend fun emit(value: T) {
        wrapped.send(value)
    }
}

fun <T> Flow<T>.liveFlowIn(scope: CoroutineScope): LiveFlow<T> {
    val upstream = this
    val downstream = MutableLiveFlow<T>()
    scope.launch { upstream.collect { downstream.emit(it) } }
    return downstream
}
