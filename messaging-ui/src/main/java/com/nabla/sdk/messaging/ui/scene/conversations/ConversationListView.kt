package com.nabla.sdk.messaging.ui.scene.conversations

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.messaging.ui.databinding.NablaConversationListViewBinding
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays

class ConversationListView : FrameLayout {
    private lateinit var binding: NablaConversationListViewBinding
    val recyclerView: RecyclerView
        get() = binding.conversationsRecyclerView
    val loadingView: View
        get() = binding.conversationsLoadingProgressBar

    constructor(
        context: Context,
        attrs: AttributeSet,
        defAttrRes: Int,
    ) : super(
        context.withNablaMessagingThemeOverlays(attrs),
        attrs,
        defAttrRes
    ) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context.withNablaMessagingThemeOverlays(attrs), attrs) {
        init(attrs)
    }

    constructor(context: Context) : super(context.withNablaMessagingThemeOverlays()) {
        init(attrs = null)
    }

    private fun init(attrs: AttributeSet?) {
        binding = NablaConversationListViewBinding.inflate(LayoutInflater.from(context.withNablaMessagingThemeOverlays(attrs)), this)
    }
}
