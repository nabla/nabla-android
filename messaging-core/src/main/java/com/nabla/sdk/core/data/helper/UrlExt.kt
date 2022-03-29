package com.nabla.sdk.core.data

import com.nabla.sdk.core.domain.entity.Url
import java.net.MalformedURLException
import java.net.URL

@Throws(MalformedURLException::class)
fun Url.toJvmUrl(): URL {
    return URL(this)
}

fun URL.toKtUrl(): Url {
    return this.toString()
}
