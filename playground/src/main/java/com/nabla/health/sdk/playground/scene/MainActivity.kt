package com.nabla.health.sdk.playground.scene

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.nabla.health.sdk.messaging.ui.helper.createWithFactory
import com.nabla.health.sdk.messaging.ui.scene.ConversationListViewModel
import com.nabla.health.sdk.playground.databinding.ActivityMainBinding
import com.nabla.health.sdk.playground.injection.appContainer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: ConversationListViewModel by viewModels {
        createWithFactory(this) {
            appContainer.messagingUiContainer.createConversationListViewModel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.createConversation.setOnClickListener {
            viewModel.createConversation()
        }
    }
}
