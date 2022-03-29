package com.nabla.sdk.playground.scene

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nabla.sdk.messaging.ui.helper.ConversationListViewModelFactory
import com.nabla.sdk.messaging.ui.scene.ConversationListViewModel
import com.nabla.sdk.messaging.ui.scene.bindViewModel
import com.nabla.sdk.playground.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: ConversationListViewModel by viewModels {
        ConversationListViewModelFactory(
            owner = this,
            onConversationClicked = { println("conversation clicked!") },
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
        binding.createConversation.setOnClickListener {
            viewModel.createConversation()
        }
        binding.conversationListView.bindViewModel(viewModel)
    }
}
