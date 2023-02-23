package com.nabla.sdk.core.ui.helpers

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import com.nabla.sdk.core.annotation.NablaInternal
import java.io.Serializable

@NablaInternal
public object IntentExtension {
    @NablaInternal
    public fun <T : Parcelable> Intent.getParcelableExtraCompat(name: String, clazz: Class<T>): T? {
        if (Build.VERSION.SDK_INT >= 33) {
            return getParcelableExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            return getParcelableExtra(name)
        }
    }

    @NablaInternal
    public fun <T : Parcelable> Intent.getParcelableArrayListExtraCompat(name: String, clazz: Class<T>): ArrayList<T>? {
        if (Build.VERSION.SDK_INT >= 33) {
            return getParcelableArrayListExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            return getParcelableArrayListExtra(name)
        }
    }

    @NablaInternal
    public fun <T : Serializable> Intent.getSerializableExtraCompat(name: String, clazz: Class<T>): T? {
        if (Build.VERSION.SDK_INT >= 33) {
            return getSerializableExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            return getSerializableExtra(name) as? T
        }
    }
}
