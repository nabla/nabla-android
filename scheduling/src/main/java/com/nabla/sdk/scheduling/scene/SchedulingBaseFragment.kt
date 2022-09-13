package com.nabla.sdk.scheduling.scene

import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public abstract class SchedulingBaseFragment : Fragment {

    public constructor() : super()
    public constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    final override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater =
        super.onGetLayoutInflater(savedInstanceState).cloneWithNablaOverlay()
}

internal fun LayoutInflater.cloneWithNablaOverlay(): LayoutInflater =
    cloneInContext(context?.withNablaVideoCallThemeOverlays())
