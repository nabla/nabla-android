package com.nabla.sdk.core.ui.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.nabla.sdk.core.ui.helpers.getThemeColor
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaComponentIconBadgeBinding
import com.google.android.material.R as MaterialR

internal class IconBadge : FrameLayout {
    private lateinit var binding: NablaComponentIconBadgeBinding

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet) {
        binding = NablaComponentIconBadgeBinding.inflate(LayoutInflater.from(context), this, true)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.IconBadge)

        val color = attributes.getColor(R.styleable.IconBadge_badgeTint, context.getThemeColor(MaterialR.attr.colorPrimaryDark))
        val icon = attributes.getResourceId(R.styleable.IconBadge_badgeIcon, R.drawable.ic_nabla_add)

        loadTintAndIcon(color, icon)
        attributes.recycle()
    }

    private fun loadTintAndIcon(@ColorInt colorInt: Int, @DrawableRes icon: Int) {

        // The color of the background is just the same color but with 10% opacity.
        val backgroundColor = Color.argb(
            255 / 10,
            Color.red(colorInt),
            Color.green(colorInt),
            Color.blue(colorInt),
        )

        binding.iconBadgeIcon.setImageResource(icon)
        binding.iconBadgeIcon.setColorFilter(colorInt)
        binding.iconBadgeCard.setCardBackgroundColor(backgroundColor)
    }
}
