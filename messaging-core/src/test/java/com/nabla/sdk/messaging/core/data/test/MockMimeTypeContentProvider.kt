package com.nabla.sdk.messaging.core.data.test

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor

class MockMimeTypeContentProvider(private val mimeType: String?) : ContentProvider() {
    override fun onCreate(): Boolean = false

    override fun query(
        p0: android.net.Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? = null

    override fun getType(p0: android.net.Uri): String? = mimeType

    override fun insert(p0: android.net.Uri, p1: ContentValues?): android.net.Uri? = null

    override fun delete(
        p0: android.net.Uri,
        p1: String?,
        p2: Array<out String>?
    ): Int = 0

    override fun update(
        p0: android.net.Uri,
        p1: ContentValues?,
        p2: String?,
        p3: Array<out String>?
    ): Int = 0
}
