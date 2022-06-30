package com.nabla.sdk.messaging.ui.scene.conversations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nabla.sdk.core.ui.helpers.factoryFor
import com.nabla.sdk.core.ui.helpers.getThemeDrawable
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.viewLifeCycleScope
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.databinding.NablaFragmentConversationListBinding
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import com.nabla.sdk.messaging.ui.helper.ConversationListViewModelFactory
import com.nabla.sdk.messaging.ui.scene.messages.ConversationActivity

public open class InboxFragment : Fragment() {
    public open val messagingClient: NablaMessagingClient
        get() = NablaMessagingClient.getInstance()

    private var binding: NablaFragmentConversationListBinding? = null

    /**
     * Should the back button been displayed in the toolbar. Defaults to false, you can override this
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
        val binding = NablaFragmentConversationListBinding.inflate(inflater, container, false)
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

        binding.conversationListView.bindViewModel(
            listViewModel,
            onConversationClicked = ::openConversationScreen,
        )

        viewLifeCycleScope.launchCollect(listViewModel.stateFlow) { state ->
            binding.createConversationCta.isVisible = state is ConversationListViewModel.State.Loaded
        }

        viewLifecycleOwner.launchCollect(inboxViewModel.openConversationFlow) { conversationId ->
            openConversationScreen(conversationId)
        }

        viewLifecycleOwner.launchCollect(inboxViewModel.errorAlertEventFlow) { errorAlert ->
            context?.let { context ->
                Toast.makeText(context, errorAlert.errorMessageRes, Toast.LENGTH_SHORT).show()
            }
        }

        viewLifeCycleScope.launchCollect(inboxViewModel.isCreatingConversationFlow) { isCreating ->
            binding.createConversationCta.isEnabled = !isCreating
            binding.createConversationCta.elevation = if (isCreating) 0f else CTA_ELEVATION
            binding.createConversationCtaText.isVisible = !isCreating
            binding.createConversationCtaIcon.isVisible = !isCreating
            binding.createConversationCtaProgressBar.isVisible = isCreating
        }

        binding.createConversationCta.setOnClickListener {
            inboxViewModel.createConversation()
        }
    }

    @CallSuper
    open override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    private companion object {
        private const val CTA_ELEVATION = 8f
    }
}
