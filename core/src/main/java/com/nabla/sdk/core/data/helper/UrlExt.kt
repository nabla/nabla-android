package com.nabla.sdk.core.data.helper

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.Uri
import java.net.MalformedURLException
import java.net.URI

@NablaInternal
@Throws(MalformedURLException::class)
public fun Uri.toJvmUri(): URI {
    return URI(uri)
}

@NablaInternal
public fun Uri.toAndroidUri(): android.net.Uri {
    return android.net.Uri.parse(uri)
}

@NablaInternal
public fun URI.toKtUri(): Uri {
    return Uri(toString())
}

@NablaInternal
public fun android.net.Uri.toJvmURI(): URI {
    return URI.create(toString())
}

@NablaInternal
public fun android.net.Uri.toKtUri(): Uri {
    return toJvmURI().toKtUri()
}

@NablaInternal
public fun URI.toAndroidUri(): android.net.Uri {
    return android.net.Uri.parse(toString())
}
