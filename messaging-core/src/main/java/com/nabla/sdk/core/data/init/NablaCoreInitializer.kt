package com.nabla.sdk.core.data.init

import android.content.Context
import androidx.startup.Initializer
import com.nabla.sdk.core.NablaCore

internal class NablaCoreInitializer: Initializer<NablaCore> {
    override fun create(context: Context): NablaCore {
        return NablaCore.instance.apply { init(context) }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}