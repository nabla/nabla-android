package com.nabla.sdk.core.domain.entity

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.ui.helpers.context

public sealed interface StringOrRes {
    public data class String(val stringValue: kotlin.String) : StringOrRes
    public data class Res(@StringRes val resValue: Int) : StringOrRes
}

public fun StringOrRes.evaluate(context: Context): String = when (this) {
    is StringOrRes.String -> stringValue
    is StringOrRes.Res -> context.getString(resValue)
}

public fun StringOrRes.evaluate(fragment: Fragment): String = evaluate(fragment.requireContext())

public fun StringOrRes.evaluate(viewBinding: ViewBinding): String = evaluate(viewBinding.context)

@NablaInternal
public fun String.asStringOrRes(): StringOrRes = StringOrRes.String(this)

@NablaInternal
public fun @receiver:StringRes Int.asStringOrRes(): StringOrRes = StringOrRes.Res(this)
