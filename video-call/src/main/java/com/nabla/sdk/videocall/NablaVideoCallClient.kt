package com.nabla.sdk.videocall

import android.content.Context

public interface NablaVideoCallClient {
    public fun openVideoCall(context: Context, url: String, roomId: String, token: String)
}
