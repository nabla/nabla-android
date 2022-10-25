package com.nabla.sdk.docscanner.ui

import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.docscanner.ui.extensions.withNablaDocScannerThemeOverlays

internal abstract class DocumentScanBaseFragment : Fragment() {

    final override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater =
        super.onGetLayoutInflater(savedInstanceState).cloneInContext(context?.withNablaDocScannerThemeOverlays())

    internal fun hostActivity() = activity as? DocumentScanActivity
        ?: throwNablaInternalException("Host activity $activity is not a ${DocumentScanBaseFragment::class.simpleName}")
}
