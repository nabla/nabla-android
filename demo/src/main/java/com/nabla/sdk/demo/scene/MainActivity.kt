package com.nabla.sdk.demo.scene

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.demo.databinding.ActivityMainBinding
import com.nabla.sdk.demo.scene.ConversationActivity.Companion.CONVERSATION_ID_EXTRA
import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.ui.helper.ConversationListViewModelFactory
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel
import com.nabla.sdk.messaging.ui.scene.conversations.bindViewModel
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: ConversationListViewModel by viewModels {
        ConversationListViewModelFactory(
            owner = this,
            onConversationClicked = { id ->
                startActivity(Intent(this, ConversationActivity::class.java).apply { putExtra(CONVERSATION_ID_EXTRA, id.value) })
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            NablaCore.getInstance().authenticate(UUID.randomUUID().toString())

            binding.createConversation.setOnClickListener {
                launch {
                    NablaMessaging.getInstance().createConversation()
                }
            }
            binding.conversationListView.bindViewModel(viewModel)
        }
    }
}
