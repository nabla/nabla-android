package com.nabla.sdk.uitests.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.nabla.sdk.messaging.ui.helper.ConversationListViewModelFactory
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel
import com.nabla.sdk.messaging.ui.scene.conversations.bindViewModel
import com.nabla.sdk.messaging.ui.scene.messages.ConversationFragment
import com.nabla.sdk.uitests.databinding.FragmentConversationsBinding
import kotlinx.coroutines.launch

class ConversationsFragment : Fragment() {

    private var _binding: FragmentConversationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConversationListViewModel by viewModels {
        ConversationListViewModelFactory(
            owner = this,
            messagingClient = nablaMessagingClient,
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConversationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.createConversation.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                nablaMessagingClient.createConversation()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            binding.conversationListView.bindViewModel(
                viewModel,
                onConversationClicked = { id ->
                    (activity as MainActivity).pushFragment(
                        ConversationFragment.newInstance(id) {
                            setFragment(StubbedConversionFragment())
                        }
                    )
                }
            )
        }
    }
}
