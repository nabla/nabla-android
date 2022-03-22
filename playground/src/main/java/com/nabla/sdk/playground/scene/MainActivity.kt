package com.nabla.sdk.playground.scene

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nabla.sdk.messaging.core.Nabla
import com.nabla.sdk.messaging.ui.helper.createWithFactory
import com.nabla.sdk.messaging.ui.messagingUiContainer
import com.nabla.sdk.messaging.ui.scene.ConversationListViewModel
import com.nabla.sdk.playground.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: ConversationListViewModel by viewModels {
        createWithFactory(this) {
            Nabla.getInstance().messagingUiContainer.createConversationListViewModel()
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
