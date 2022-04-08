package com.nabla.sdk.core.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaComponentCardRowBinding

class CardRow : ConstraintLayout {
    private lateinit var binding: NablaComponentCardRowBinding
    private var noLeftView: Boolean = false

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0, DEFAULT_STYLE_RES) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet) {
        binding = NablaComponentCardRowBinding.inflate(LayoutInflater.from(context), this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CardRow)

        val title = attributes.getString(R.styleable.CardRow_title)
        val subtitle = attributes.getString(R.styleable.CardRow_subtitle)
        val trailingTitle = attributes.getString(R.styleable.CardRow_trailingTitle)
        this.noLeftView = attributes.getBoolean(R.styleable.CardRow_trailingTitle, false)

        loadTextContent(title, subtitle, trailingTitle)
        attributes.recycle()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams) {
        if (::binding.isInitialized) {
            if (noLeftView || binding.cardRowLeftContainer.childCount > 0) {
                binding.cardRowRightContainer.addView(child, -1, params)
            } else {
                binding.cardRowLeftContainer.addView(child, -1)

                // Adds a 12dp left and right padding in case of item in start position
                val paddingInPixels = resources.dpToPx(12)
                binding.cardRowTextContainer.setPadding(
                    paddingInPixels,
                    0,
                    paddingInPixels,
                    0
                )
            }
        } else {
            super.addView(child, index, params)
        }
    }

    private fun loadTextContent(
        title: String?,
        subtitle: String?,
        trailingTitle: String?,
    ) {
        title?.let {
            binding.cardRowTitle.text = title
        }

        binding.cardRowSubtitle.setTextOrHide(subtitle)
        binding.cardRowSubtitle.setTextOrHide(trailingTitle)
    }

    companion object {
        private val DEFAULT_STYLE_RES = R.style.CardRowStyle
    }
}
