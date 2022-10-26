package com.nabla.sdk.messaging.ui.scene.conversations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.core.ui.helpers.factoryFor
import com.nabla.sdk.core.ui.helpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.getThemeDrawable
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.viewLifeCycleScope
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.messagingClient
import com.nabla.sdk.messaging.ui.databinding.NablaFragmentInboxBinding
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import com.nabla.sdk.messaging.ui.helper.ConversationListViewModelFactory
import com.nabla.sdk.messaging.ui.scene.messages.ConversationActivity

public open class InboxFragment : Fragment() {
    private val messagingClient: NablaMessagingClient
        get() = getNablaInstanceByName().messagingClient

    private var binding: NablaFragmentInboxBinding? = null

    /**
     * Should the back button be displayed in the toolbar. Defaults to false, you can override this
     * to change the behavior
     */
    public open val shouldShowBackButton: Boolean = false

    private val listViewModel: ConversationListViewModel by viewModels {
        ConversationListViewModelFactory(owner = this, messagingClient = messagingClient)
    }

    private val inboxViewModel: InboxViewModel by viewModels {
        factoryFor { InboxViewModel(messagingClient = messagingClient) }
    }

    /**
     * Opens the conversation screen.
     * Typically triggered when user clicks a conversation or creates a new one.
     *
     * Override to define your own navigation. Default opens [ConversationActivity].
     */
    protected open fun openConversationScreen(conversationId: ConversationId) {
        startActivity(ConversationActivity.newIntent(requireContext(), conversationId))
    }

    final override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater =
        super.onGetLayoutInflater(savedInstanceState)
            .cloneInContext(context?.withNablaMessagingThemeOverlays())

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = NablaFragmentInboxBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return

        if (shouldShowBackButton) {
            view.context.getThemeDrawable(android.R.attr.homeAsUpIndicator)
                ?.let { binding.toolbar.setNavigationIcon(it) }
            binding.toolbar.setNavigationOnClickListener {
                activity?.onBackPressed()
            }
        } else {
            binding.toolbar.navigationIcon = null
        }

        viewLifeCycleScope.launchCollect(listViewModel.stateFlow) { state ->
            binding.createConversationCta.isVisible = state is ConversationListViewModel.State.Loaded
        }

        viewLifecycleOwner.launchCollect(inboxViewModel.openConversationFlow) { conversationId ->
            openConversationScreen(conversationId)
        }

        binding.createConversationCta.setOnClickListener {
            inboxViewModel.createConversation()
        }

        binding.createConversationCta.doOnNextLayout {
            binding.conversationListView.bindViewModel(
                listViewModel,
                onConversationClicked = ::openConversationScreen,
                itemDecoration = DefaultOffsetsItemDecoration(
                    spacingBetweenItemsPx = binding.context.dpToPx(0),
                    firstItemTopPaddingPx = binding.context.dpToPx(12),
                    lastItemBottomPaddingPx = it.height + it.marginBottom + binding.context.dpToPx(8),
                ),
            )
        }
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    public companion object {
        public fun newInstance(): InboxFragment {
            return newInstance(NablaClient.DEFAULT_NAME)
        }

        public fun newInstance(sdkName: String): InboxFragment {
            return InboxFragment().apply { setSdkName(sdkName) }
        }
    }
}
