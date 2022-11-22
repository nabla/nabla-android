package com.nabla.sdk.core.ui.helpers

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public fun <T : Parcelable> Bundle.getParcelableCompat(name: String, clazz: Class<T>): T? {
    if (Build.VERSION.SDK_INT >= 33) {
        return getParcelable(name, clazz)
    } else {
        @Suppress("DEPRECATION")
        return getParcelable(name)
    }
}

@NablaInternal
public fun <T : Parcelable> Bundle.getParcelableArrayListCompat(name: String, clazz: Class<T>): ArrayList<T>? {
    if (Build.VERSION.SDK_INT >= 33) {
        return getParcelableArrayList(name, clazz)
    } else {
        @Suppress("DEPRECATION")
        return getParcelableArrayList(name)
    }
}
