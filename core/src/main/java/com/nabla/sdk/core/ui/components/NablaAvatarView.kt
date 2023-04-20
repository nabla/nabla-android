package com.nabla.sdk.core.ui.components

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.ViewCompat
import coil.decode.SvgDecoder
import coil.dispose
import coil.load
import coil.size.Scale
import com.nabla.sdk.core.R
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.databinding.NablaComponentAvatarViewBinding
import com.nabla.sdk.core.domain.entity.InternalException.Companion.asNablaInternal
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_NONE
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_OVAL
import com.nabla.sdk.core.ui.components.AvatarClipShape.CLIP_SHAPE_ROUND_RECT
import com.nabla.sdk.core.ui.helpers.ColorExtensions.setBackgroundColor
import com.nabla.sdk.core.ui.helpers.ColorIntOrStateList
import com.nabla.sdk.core.ui.helpers.ColorIntWrapper
import com.nabla.sdk.core.ui.helpers.MessageAuthorExtensions.initials
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeColor
import kotlin.math.min
import com.google.android.material.R as MaterialR

@NablaInternal
public class NablaAvatarView : ConstraintLayout {
    private lateinit var binding: NablaComponentAvatarViewBinding

    private var useSingleLetterInPlaceHolder: Boolean = false

    @ColorInt
    private var defaultBackgroundColor: Int = 0
    private lateinit var defaultAvatarDrawable: Drawable

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
            defaultBackgroundColor = typedArray.getColor(R.styleable.NablaAvatarView_nabla_defaultBackgroundColor, context.getThemeColor(MaterialR.attr.colorSurfaceVariant).asColorStateList(context).defaultColor)
            defaultAvatarDrawable = typedArray.getDrawable(R.styleable.NablaAvatarView_nabla_defaultAvatarDrawable)
                ?: ContextCompat.getDrawable(context, R.drawable.nabla_ic_outline_person)
                ?: throw IllegalStateException("Unable to find nabla_ic_outline_person drawable").asNablaInternal()
        }
    }

    public fun loadAvatar(provider: Provider) {
        val initials = provider.initials(context = context, singleLetter = useSingleLetterInPlaceHolder)
        loadAvatar(provider.avatar?.url, initials)
    }

    public fun loadAvatar(avatarUrl: Uri?, placeholderText: String?) {
        val placeholderBackgroundColor = ColorIntWrapper(defaultBackgroundColor)

        if (avatarUrl == null) {
            showPlaceholder(placeholderText, placeholderBackgroundColor)
            binding.componentAvatarImageView.dispose()
            return
        }

        hidePlaceholder()
        binding.componentAvatarImageView.load(avatarUrl.uri) {
            scale(Scale.FIT)
            decoderFactory(SvgDecoder.Factory())
            listener(
                onSuccess = { _, _ ->
                    hidePlaceholder()
                },
                onError = { _, _ ->
                    showPlaceholder(placeholderText, placeholderBackgroundColor)
                },
            )
        }
    }

    private fun showPlaceholder(text: String?, background: ColorIntOrStateList) {
        binding.componentAvatarRoot.setBackgroundColor(background)
        binding.componentAvatarImageView.visibility = View.INVISIBLE

        if (text != null) {
            binding.componentAvatarPlaceholderTextView.text = text
            binding.componentAvatarPlaceholderTextView.visibility = View.VISIBLE
            binding.componentDefaultAvatarImageView.visibility = View.INVISIBLE
        } else {
            binding.componentAvatarPlaceholderTextView.visibility = View.INVISIBLE
            binding.componentDefaultAvatarImageView.visibility = View.VISIBLE
            binding.componentDefaultAvatarImageView.setImageDrawable(defaultAvatarDrawable)
        }

        val stateDescription = context.getString(R.string.nabla_avatar_state_description_placeholder)
        ViewCompat.setStateDescription(this, stateDescription)
    }

    private fun hidePlaceholder() {
        binding.componentAvatarRoot.background = null
        binding.componentAvatarImageView.visibility = View.VISIBLE
        binding.componentAvatarPlaceholderTextView.text = ""
        binding.componentAvatarPlaceholderTextView.visibility = View.INVISIBLE
        binding.componentDefaultAvatarImageView.visibility = View.INVISIBLE

        val stateDescription = context.getString(R.string.nabla_avatar_state_description_no_placeholder)
        ViewCompat.setStateDescription(this, stateDescription)
    }

    public fun displayUnicolorPlaceholder() {
        loadAvatar(avatarUrl = null, placeholderText = null)
    }
}
