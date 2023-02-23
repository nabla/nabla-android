package com.nabla.sdk.core.ui.helpers

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.NablaClient.Companion.DEFAULT_NAME
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException

@NablaInternal
public object SceneHelpers {
    @NablaInternal
    public fun Fragment.setSdkName(sdkName: String) {
        val bundle = arguments ?: Bundle()
        bundle.putString(ARG_SDK_NAME, sdkName)
        arguments = bundle
    }

    @NablaInternal
    public fun Fragment.requireSdkName(): String {
        return requireSdkName(arguments?.getString(ARG_SDK_NAME))
    }

    @NablaInternal
    public fun Fragment.sdkNameOrDefault(): String = arguments?.getString(ARG_SDK_NAME) ?: DEFAULT_NAME

    @NablaInternal
    public fun Fragment.getNablaInstanceByName(): NablaClient = NablaClient.getInstance(sdkNameOrDefault())

    @NablaInternal
    public fun Intent.setSdkName(sdkName: String) {
        putExtra(ARG_SDK_NAME, sdkName)
    }

    @NablaInternal
    public fun Intent.requireSdkName(): String {
        return requireSdkName(extras?.getString(ARG_SDK_NAME))
    }

    @NablaInternal
    public fun Intent.sdkNameOrDefault(): String = extras?.getString(ARG_SDK_NAME) ?: DEFAULT_NAME

    private fun requireSdkName(sdkName: String?): String {
        return sdkName ?: throwNablaInternalException("Missing SDK name")
    }
}

private const val ARG_SDK_NAME = "sdkName"
