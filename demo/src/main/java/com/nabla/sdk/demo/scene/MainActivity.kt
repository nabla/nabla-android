package com.nabla.sdk.demo.scene

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nabla.sdk.demo.databinding.ActivityMainBinding
import com.nabla.sdk.demo.scene.ConversationActivity.Companion.CONVERSATION_ID_EXTRA
import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.ui.helper.ConversationListViewModelFactory
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel
import com.nabla.sdk.messaging.ui.scene.conversations.bindViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: ConversationListViewModel by viewModels {
        ConversationListViewModelFactory(owner = this)
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            binding.createConversation.setOnClickListener {
                launch {
                    NablaMessaging.getInstance().createConversation()
                }
            }
            binding.conversationListView.bindViewModel(
                viewModel,
                onConversationClicked = { id ->
                    startActivity(
                        Intent(applicationContext, ConversationActivity::class.java)
                            .apply { putExtra(CONVERSATION_ID_EXTRA, id.value) }
                    )
                }
            )
        }
    }
}
