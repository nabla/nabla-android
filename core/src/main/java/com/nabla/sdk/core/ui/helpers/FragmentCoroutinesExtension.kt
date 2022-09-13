package com.nabla.sdk.core.ui.helpers

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public val Fragment.viewLifeCycleScope: LifecycleCoroutineScope get() = viewLifecycleOwner.lifecycleScope
