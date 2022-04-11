package com.nabla.sdk.messaging.ui.fullscreenmedia.helper

import android.net.Uri

// This makes sure the URI part is recomputed so that url encoding happens correctly after deserializing it from a string
// content://com.android.providers.media.documents/document/image:60 becomes content://com.android.providers.media.documents/document/image%3A60
// otherwise it doesn't work properly as we get a security exception
internal fun Uri.sanitize() = buildUpon().path(path).build()
