package com.nabla.sdk.core.ui.helpers

import android.content.Context
import androidx.viewbinding.ViewBinding

val ViewBinding.context get(): Context = root.context
