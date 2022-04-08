package com.nabla.sdk.core.ui.helpers

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope

val Fragment.viewLifeCycleScope: LifecycleCoroutineScope get() = viewLifecycleOwner.lifecycleScope
