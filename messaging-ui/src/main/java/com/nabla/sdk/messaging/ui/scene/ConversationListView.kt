package com.nabla.sdk.messaging.ui.scene

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.messaging.ui.databinding.ConversationListViewBinding

class ConversationListView : FrameLayout {
    private lateinit var binding: ConversationListViewBinding
    val recyclerView: RecyclerView
        get() = binding.conversationsRecyclerView
    val loadingView: View
        get() = binding.conversationsLoadingProgressBar

    constructor(context: Context, attrs: AttributeSet, defAttrRes: Int, defStyleRes: Int) : super(context, attrs, defAttrRes, defStyleRes) {
        init(context, attrs, defAttrRes, defStyleRes)
    }

    constructor(context: Context, attrs: AttributeSet, defAttrRes: Int) : super(context, attrs, defAttrRes) {
        init(context, attrs, defAttrRes)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defAttrRes: Int? = null,
        @StyleRes defStyleRes: Int? = null,
    ) {
        binding = ConversationListViewBinding.inflate(LayoutInflater.from(context), this)
    }
}
