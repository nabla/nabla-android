package com.nabla.sdk.uitests.scene

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import com.nabla.sdk.messaging.ui.scene.messages.ConversationFragment
import com.nabla.sdk.uitests.R
import com.nabla.sdk.uitests.databinding.ActivityConversationBinding

class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            val conversationId = (intent.getSerializableExtra(CONVERSATION_ID_EXTRA) as Uuid).toConversationId()

            supportFragmentManager.commit {
                add(
                    R.id.fragmentContainer,
                    ConversationFragment.newInstance(conversationId) {
                        setFragment(MyConvFragment())
                    },
                    "tag",
                )
            }
        }
    }

    companion object {
        const val CONVERSATION_ID_EXTRA = "conversationId"
    }
}

class MyConvFragment : ConversationFragment() {
    override val messagingClient = nablaMessagingClient
}
