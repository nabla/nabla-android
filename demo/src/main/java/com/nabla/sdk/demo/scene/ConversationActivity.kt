package com.nabla.sdk.demo.scene

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.benasher44.uuid.Uuid
import com.nabla.sdk.demo.R
import com.nabla.sdk.demo.databinding.ActivityConversationBinding
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import com.nabla.sdk.messaging.ui.scene.messages.ConversationFragment

class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            val conversationId = (intent.getSerializableExtra(CONVERSATION_ID_EXTRA) as Uuid).toConversationId()

            supportFragmentManager.commit {
                add(R.id.fragmentContainer, ConversationFragment.newInstance(conversationId), "tag")
            }
        }
    }

    companion object {
        const val CONVERSATION_ID_EXTRA = "conversationId"
    }
}
