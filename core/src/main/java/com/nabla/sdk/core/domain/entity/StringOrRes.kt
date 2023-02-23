package com.nabla.sdk.core.domain.entity

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context

public sealed interface StringOrRes {
    public data class String(val stringValue: kotlin.String) : StringOrRes
    public data class Res(@StringRes val resValue: Int) : StringOrRes

    public companion object {
        public fun StringOrRes.evaluate(context: Context): kotlin.String = when (this) {
            is String -> stringValue
            is Res -> context.getString(resValue)
        }

        public fun StringOrRes.evaluate(fragment: Fragment): kotlin.String = evaluate(fragment.requireContext())

        public fun StringOrRes.evaluate(viewBinding: ViewBinding): kotlin.String = evaluate(viewBinding.context)

        @NablaInternal
        public fun kotlin.String.asStringOrRes(): StringOrRes = String(this)

        @NablaInternal
        public fun @receiver:StringRes Int.asStringOrRes(): StringOrRes = Res(this)
    }
}
