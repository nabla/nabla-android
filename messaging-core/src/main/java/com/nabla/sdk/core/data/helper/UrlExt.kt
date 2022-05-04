package com.nabla.sdk.core.data.helper

import com.nabla.sdk.core.domain.entity.Uri
import java.net.MalformedURLException
import java.net.URI

@Throws(MalformedURLException::class)
public fun Uri.toJvmUri(): URI {
    return URI(uri)
}

public fun Uri.toAndroidUri(): android.net.Uri {
    return android.net.Uri.parse(uri)
}

public fun URI.toKtUri(): Uri {
    return Uri(toString())
}

internal fun android.net.Uri.toJvmURI(): URI {
    return URI.create(toString())
}

public fun android.net.Uri.toKtUri(): Uri {
    return toJvmURI().toKtUri()
}
