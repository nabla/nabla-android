package com.nabla.sdk.core.ui.helpers

import android.content.Context
import androidx.viewbinding.ViewBinding

internal val ViewBinding.context get(): Context = root.context
