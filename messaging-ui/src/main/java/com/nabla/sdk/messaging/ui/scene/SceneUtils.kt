package com.nabla.sdk.messaging.ui.scene

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.InternalException

internal fun Fragment.setSdkName(sdkName: String) {
    val bundle = arguments ?: Bundle()
    bundle.putString(ARG_SDK_NAME, sdkName)
    arguments = bundle
}

internal fun Fragment.requireSdkName(): String {
    return requireSdkName(arguments?.getString(ARG_SDK_NAME))
}

internal fun Fragment.sdkNameOrNull() = arguments?.getString(ARG_SDK_NAME)

internal fun Fragment.getNablaInstanceByName(): NablaClient {
    val sdkName = sdkNameOrNull()
    return if (sdkName != null) {
        NablaClient.getInstance(sdkName)
    } else {
        NablaClient.getInstance()
    }
}

internal fun Intent.setSdkName(sdkName: String) {
    putExtra(ARG_SDK_NAME, sdkName)
}

internal fun Activity.requireSdkName(): String {
    return requireSdkName(intent.extras?.getString(ARG_SDK_NAME))
}

private const val ARG_SDK_NAME = "sdkName"

private fun throwMissingSdkName() {
    throw InternalException(IllegalStateException("Missing SDK name"))
}

private fun requireSdkName(sdkName: String?): String {
    return requireNotNull(sdkName) {
        throwMissingSdkName()
    }
}
