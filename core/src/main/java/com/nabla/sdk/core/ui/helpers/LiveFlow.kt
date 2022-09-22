package com.nabla.sdk.core.ui.helpers

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.whenStateAtLeast
import com.nabla.sdk.core.annotation.NablaInternal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A [Lifecycle] aware Flow that buffers values until at least one collector is subscribed with a
 * lifecycle that is at least [Lifecycle.State.STARTED] by default.
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
@NablaInternal
public interface LiveFlow<T> {
    public suspend fun collect(liveCollector: BaseLiveFlowCollector<T>)

    public abstract class BaseLiveFlowCollector<T> : FlowCollector<T>

    public class LiveFlowCollector<T>(
        private val lifecycle: Lifecycle,
        private val minState: Lifecycle.State = Lifecycle.State.STARTED,
        private val action: suspend (T) -> Unit,
    ) : BaseLiveFlowCollector<T>() {

        override suspend fun emit(value: T) {
            lifecycle.whenStateAtLeast(minState) {
                action(value)
            }
        }
    }
}

/**
 * Mutable implementation of [LiveFlow]
 */
@NablaInternal
public class MutableLiveFlow<T> : LiveFlow<T>, FlowCollector<T> {
    private val wrapped = Channel<T>(Channel.BUFFERED)

    override suspend fun collect(liveCollector: LiveFlow.BaseLiveFlowCollector<T>) {
        wrapped.receiveAsFlow().collect(liveCollector)
    }

    override suspend fun emit(value: T) {
        wrapped.send(value)
    }
}
