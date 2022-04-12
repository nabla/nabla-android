package com.nabla.sdk.demo.scene

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.demo.databinding.ActivityMainBinding
import com.nabla.sdk.messaging.ui.helper.ConversationListViewModelFactory
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel
import com.nabla.sdk.messaging.ui.scene.conversations.bindViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: ConversationListViewModel by viewModels {
        ConversationListViewModelFactory(
            owner = this,
            onConversationClicked = { id ->
                startActivity(Intent(this, ConversationActivity::class.java).apply { putExtra("conversationId", id.value) })
            },
            onErrorRetryWhen = { cause, attempt ->
                println("Error loading conversations - ${cause.stackTraceToString()}")
                if (attempt < 3) {
                    println("retrying in 3 sec")
                    delay(3.seconds)
                    true
                } else {
                    println("giving up")
                    true
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            NablaCore.instance.authenticate(UUID.randomUUID().toString())

            binding.createConversation.setOnClickListener {
                viewModel.createConversation()
            }
            binding.conversationListView.bindViewModel(viewModel)
        }
    }
}
