package com.nabla.sdk.core.ui.components

import android.content.Context
import android.graphics.Outline
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
import com.nabla.sdk.core.R
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.databinding.NablaComponentAvatarViewBinding
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.SystemUser
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_NONE
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_OVAL
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_ROUND_RECT
import com.nabla.sdk.core.ui.helpers.ColorIntOrStateList
import com.nabla.sdk.core.ui.helpers.ColorIntWrapper
import com.nabla.sdk.core.ui.helpers.getThemeColor
import com.nabla.sdk.core.ui.helpers.initials
import com.nabla.sdk.core.ui.helpers.setBackgroundColor
import kotlin.math.absoluteValue
import kotlin.math.min
import com.nabla.sdk.core.R as coreR

@NablaInternal
public class NablaAvatarView : ConstraintLayout {
    private lateinit var binding: NablaComponentAvatarViewBinding

    private var useSingleLetterInPlaceHolder: Boolean = false

    public constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs, defStyle)
    }

    public constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    public constructor(context: Context) : super(context) {
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

    public fun loadAvatar(provider: Provider) {
        val initials = provider.initials(context = context, singleLetter = useSingleLetterInPlaceHolder)
        loadAvatar(provider.avatar?.url, initials, provider.id)
    }

    public fun loadAvatar(systemUser: SystemUser) {
        val initials = systemUser.initials()
        loadAvatar(systemUser.avatar?.url, initials, userId = null)
    }

    public fun loadAvatar(avatarUrl: Uri?, placeholderText: String?, userId: Uuid?) {
        val placeholderTextComputed = placeholderText ?: ""
        val placeholderBackgroundColor = if (userId != null) {
            val indexBackground = (userId.hashCode() % backgroundColors.size).absoluteValue
            ColorIntWrapper(context.getColor(backgroundColors[indexBackground]))
        } else {
            context.getThemeColor(androidx.appcompat.R.attr.colorPrimary)
        }

        if (avatarUrl == null) {
            showPlaceholder(placeholderTextComputed, placeholderBackgroundColor)
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
                    showPlaceholder(placeholderTextComputed, placeholderBackgroundColor)
                }
            )
        }
    }

    private fun showPlaceholder(text: String, background: ColorIntOrStateList) {
        binding.componentAvatarRoot.setBackgroundColor(background)
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

    public fun displayUnicolorPlaceholder() {
        loadAvatar(avatarUrl = null, placeholderText = null, userId = null)
    }

    @ColorRes
    private val backgroundColors: List<Int> = listOf(
        R.color.nabla_turquoise,
        R.color.nabla_blue,
        coreR.color.nabla_red,
        R.color.nabla_orange,
        R.color.nabla_indigo,
        R.color.nabla_stone,
    )
}
