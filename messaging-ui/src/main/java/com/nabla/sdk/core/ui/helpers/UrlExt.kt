package com.nabla.sdk.core.ui.helpers

import com.nabla.sdk.core.data.helper.toKtUri
import com.nabla.sdk.core.domain.entity.Uri
import java.net.URI

internal fun URI.toAndroidUri(): android.net.Uri {
    return android.net.Uri.parse(toString())
}

internal fun android.net.Uri.toJvmURI(): URI {
    return URI.create(toString())
}

internal fun android.net.Uri.toKtUri(): Uri {
    return toJvmURI().toKtUri()
}
