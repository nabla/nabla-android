package com.nabla.sdk.core.ui.components

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.ViewCompat
import coil.clear
import coil.load
import coil.size.Scale
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.SystemUser
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_NONE
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_OVAL
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_ROUND_RECT
import com.nabla.sdk.core.ui.helpers.initials
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaComponentAvatarViewBinding
import kotlin.math.absoluteValue
import kotlin.math.min

internal class NablaAvatarView : ConstraintLayout {
    private lateinit var binding: NablaComponentAvatarViewBinding

    private var useSingleLetterInPlaceHolder: Boolean = false

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs, defStyle)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    private fun init(
        context: Context,
        attrs: AttributeSet?,
        @StyleRes defStyleRes: Int = R.style.Nabla_Widget_AvatarView,
        @AttrRes defAttrRes: Int = R.attr.nablaAvatarViewStyle,
    ) {
        binding = NablaComponentAvatarViewBinding.inflate(LayoutInflater.from(context), this, true)

        context.obtainStyledAttributes(attrs, R.styleable.NablaAvatarView, defAttrRes, defStyleRes).use { typedArray ->
            val shape = typedArray.getInt(R.styleable.NablaAvatarView_nabla_clipShape, CLIP_SHAPE_NONE)
            if (shape != CLIP_SHAPE_NONE) {
                clipToOutline = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        when (shape) {
                            CLIP_SHAPE_OVAL -> outline.setOval(0, 0, view.width, view.height)
                            CLIP_SHAPE_ROUND_RECT -> outline.setRoundRect(0, 0, view.width, view.height, min(view.width, view.height) * .45f)
                            else -> Unit /* no-op */
                        }
                    }
                }
            }
            useSingleLetterInPlaceHolder = typedArray.getBoolean(R.styleable.NablaAvatarView_nabla_useSingleLetterInPlaceHolder, false)
        }
    }

    fun loadAvatar(provider: Provider) {
        val initials = provider.initials(context = context, singleLetter = useSingleLetterInPlaceHolder)
        loadAvatar(provider.avatar?.url, initials, provider.id)
    }

    fun loadAvatar(systemUser: SystemUser) {
        val initials = systemUser.initials()
        loadAvatar(systemUser.avatar?.url, initials, userId = null)
    }

    fun loadAvatar(avatarUrl: Uri?, placeholderText: String?, userId: Uuid?) {
        val placeholderTextComputed = placeholderText ?: ""
        val indexBackground = userId?.let { (it.hashCode() % backgroundColors.size).absoluteValue } ?: 0
        val placeholderBackgroundColor = backgroundColors[indexBackground]
        val placeholderBackground = ColorDrawable(context.getColor(placeholderBackgroundColor))
        if (avatarUrl == null) {
            showPlaceholder(placeholderTextComputed, placeholderBackground)
            binding.componentAvatarImageView.clear()
            return
        }

        hidePlaceholder()
        binding.componentAvatarImageView.load(avatarUrl.uri) {
            scale(Scale.FIT)
            listener(
                onSuccess = { _, _ ->
                    hidePlaceholder()
                },
                onError = { _, _ ->
                    showPlaceholder(placeholderTextComputed, placeholderBackground)
                }
            )
        }
    }

    private fun showPlaceholder(text: String, background: Drawable) {
        binding.componentAvatarRoot.background = background
        binding.componentAvatarImageView.visibility = View.INVISIBLE
        binding.componentAvatarPlaceholderTextView.text = text
        binding.componentAvatarPlaceholderTextView.visibility = View.VISIBLE

        val stateDescription = context.getString(R.string.nabla_avatar_state_description_placeholder)
        ViewCompat.setStateDescription(this, stateDescription)
    }

    private fun hidePlaceholder() {
        binding.componentAvatarRoot.background = null
        binding.componentAvatarImageView.visibility = View.VISIBLE
        binding.componentAvatarPlaceholderTextView.text = ""
        binding.componentAvatarPlaceholderTextView.visibility = View.INVISIBLE

        val stateDescription = context.getString(R.string.nabla_avatar_state_description_no_placeholder)
        ViewCompat.setStateDescription(this, stateDescription)
    }

    fun displayUnicolorPlaceholder() {
        loadAvatar(avatarUrl = null, placeholderText = null, userId = null)
    }

    @ColorRes
    private val backgroundColors: List<Int> = listOf(
        R.color.nabla_turquoise,
        R.color.nabla_blue,
        R.color.nabla_red,
        R.color.nabla_orange,
        R.color.nabla_indigo,
        R.color.nabla_stone,
    )
}
