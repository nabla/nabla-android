package com.nabla.sdk.core.ui.components

import android.content.Context
import android.graphics.Color
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
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.core.domain.entity.User.Patient
import com.nabla.sdk.core.domain.entity.User.Provider
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_NONE
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_OVAL
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_ROUND_RECT
import com.nabla.sdk.core.ui.helpers.getThemeDrawable
import com.nabla.sdk.core.ui.helpers.initials
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaComponentAvatarViewBinding
import kotlin.math.absoluteValue
import kotlin.math.min

class AvatarView : ConstraintLayout {
    private lateinit var binding: NablaComponentAvatarViewBinding

    private var useSingleLetterInPlaceHolder: Boolean = false

    constructor(context: Context, attrs: AttributeSet, defStyleRes: Int, defAttrRes: Int) : super(context, attrs, defStyleRes, defAttrRes) {
        init(context, attrs, defStyleRes, defAttrRes)
    }

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
        @StyleRes defStyleRes: Int = R.style.Widget_Nabla_AvatarView,
        @AttrRes defAttrRes: Int = R.attr.avatarViewStyle,
    ) {
        binding = NablaComponentAvatarViewBinding.inflate(LayoutInflater.from(context), this, true)

        context.obtainStyledAttributes(attrs, R.styleable.AvatarView, defAttrRes, defStyleRes).use { typedArray ->
            val shape = typedArray.getInt(R.styleable.AvatarView_clipShape, CLIP_SHAPE_NONE)
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
            useSingleLetterInPlaceHolder = typedArray.getBoolean(R.styleable.AvatarView_useSingleLetterInPlaceHolder, false)
        }
    }

    fun loadAvatar(user: User) {
        val initials = user.initials(context = context, singleLetter = useSingleLetterInPlaceHolder)
        when (user) {
            is Provider -> loadAvatar(user.avatar?.url, initials, user.id)
            is Patient -> loadAvatar(user.avatar?.url, initials, user.id)
            is User.Unknown -> loadAvatar(null, null, null)
        }
    }

    fun loadAvatar(avatarUrl: Uri?, placeholderText: String?, userId: Uuid?, grayOut: Boolean = false) {
        val placeholderTextComputed = placeholderText ?: ""
        val indexBackground = userId?.let { (it.hashCode() % backgroundColors.size).absoluteValue } ?: 0
        val placeholderBackgroundColor = if (grayOut) deactivatedBackground else backgroundColors[indexBackground]
        val placeholderBackground = ColorDrawable(context.getColor(placeholderBackgroundColor))
        if (avatarUrl == null) {
            showPlaceholder(placeholderTextComputed, placeholderBackground, grayOut)
            binding.componentAvatarImageView.clear()
            return
        }

        if (grayOut) {
            binding.componentAvatarImageView.setColorFilter(Color.argb(128, 255, 255, 255))
        } else {
            binding.componentAvatarImageView.clearColorFilter()
        }

        hidePlaceholder(grayOut)
        binding.componentAvatarImageView.load(avatarUrl.uri) {
            scale(Scale.FIT)
            listener(
                onSuccess = { _, _ ->
                    hidePlaceholder(grayOut)
                },
                onError = { _, _ ->
                    showPlaceholder(placeholderTextComputed, placeholderBackground, grayOut)
                }
            )
        }
    }

    private fun showPlaceholder(text: String, background: Drawable, grayOut: Boolean) {
        binding.componentAvatarRoot.background = background
        binding.componentAvatarImageView.visibility = View.INVISIBLE
        binding.componentAvatarPlaceholderTextView.text = text
        binding.componentAvatarPlaceholderTextView.visibility = View.VISIBLE

        val stateDescription = context.getString(
            R.string.avatar_state_description_placeholder,
            if (grayOut) context.getString(R.string.avatar_state_description_gray_out) else ""
        )
        ViewCompat.setStateDescription(this, stateDescription)
    }

    private fun hidePlaceholder(grayOut: Boolean) {
        binding.componentAvatarRoot.background = null
        binding.componentAvatarImageView.visibility = View.VISIBLE
        binding.componentAvatarPlaceholderTextView.text = ""
        binding.componentAvatarPlaceholderTextView.visibility = View.INVISIBLE

        val stateDescription = context.getString(
            R.string.avatar_state_description_no_placeholder,
            if (grayOut) context.getString(R.string.avatar_state_description_gray_out) else ""
        )
        ViewCompat.setStateDescription(this, stateDescription)
    }

    fun displaySystemAvatar() {
        binding.componentAvatarImageView.setImageResource(requireNotNull(context.getThemeDrawable(R.attr.systemAvatar)))
    }

    @ColorRes
    private val backgroundColors: List<Int> = listOf(
        R.color.turquoise,
        R.color.blue,
        R.color.red,
        R.color.orange,
        R.color.indigo,
        R.color.stone,
    )

    @ColorRes
    private val deactivatedBackground = R.color.grey
}
