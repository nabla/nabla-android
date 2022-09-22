package com.nabla.sdk.messaging.ui.scene.messages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.InternalException.Companion.asNablaInternal
import com.nabla.sdk.core.ui.helpers.requireSdkName
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaActivityConversationBinding

public class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: NablaActivityConversationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NablaActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            val conversationId = (intent.getParcelableExtra<ConversationId>(CONVERSATION_ID_EXTRA))
                ?: throw RuntimeException("Failed to find/parse conversationId in ConversationActivity: ${intent.extras}").asNablaInternal()

            val showComposer = intent.getBooleanExtra(SHOW_COMPOSER_EXTRA, true)

            supportFragmentManager.commit {
                replace(
                    R.id.fragmentContainer,
                    ConversationFragment.newInstance(conversationId, intent.requireSdkName()) {
                        setShowComposer(showComposer)
                    }
                )
            }
        }
    }

    public companion object {
        public fun newIntent(
            context: Context,
            conversationId: ConversationId,
            showComposer: Boolean = true,
        ): Intent =
            Intent(context, ConversationActivity::class.java)
                .apply {
                    putExtra(CONVERSATION_ID_EXTRA, conversationId)
                    putExtra(SHOW_COMPOSER_EXTRA, showComposer)
                    setSdkName(NablaClient.DEFAULT_NAME)
                }

        public fun newIntent(
            context: Context,
            conversationId: ConversationId,
            sdkName: String,
            showComposer: Boolean = true,
        ): Intent =
            Intent(context, ConversationActivity::class.java)
                .apply {
                    putExtra(CONVERSATION_ID_EXTRA, conversationId)
                    putExtra(SHOW_COMPOSER_EXTRA, showComposer)
                    setSdkName(sdkName)
                }

        private const val CONVERSATION_ID_EXTRA = "conversationId"
        private const val SHOW_COMPOSER_EXTRA = "showComposer"
    }
}
