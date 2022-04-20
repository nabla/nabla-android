package com.nabla.sdk.demo.scene

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.nabla.sdk.demo.R
import com.nabla.sdk.demo.databinding.ActivityConversationBinding
import com.nabla.sdk.messaging.ui.scene.messages.ConversationFragment

class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.commit { add<ConversationFragment>(R.id.fragmentContainer, "tag", intent.extras) }
    }
}
