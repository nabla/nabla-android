package com.nabla.sdk.playground.scene

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.nabla.sdk.messaging.ui.helper.createWithFactory
import com.nabla.sdk.messaging.ui.scene.ConversationListViewModel
import com.nabla.sdk.playground.databinding.ActivityMainBinding
import com.nabla.sdk.playground.injection.appContainer

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
