package com.nabla.sdk.docscanner.ui

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import coil.load
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.core.data.helper.UrlExt.toKtUri
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.helpers.BundleExtension.getParcelableArrayListCompat
import com.nabla.sdk.core.ui.helpers.BundleExtension.getParcelableCompat
import com.nabla.sdk.core.ui.helpers.SceneHelpers.sdkNameOrDefault
import com.nabla.sdk.core.ui.helpers.SceneHelpers.setSdkName
import com.nabla.sdk.docscanner.core.models.NormalizedCorners
import com.nabla.sdk.docscanner.databinding.NablaFragmentDocumentEdgePickerBinding
import com.nabla.sdk.docscanner.ui.extensions.toAndroidArrayList
import com.nabla.sdk.docscanner.ui.extensions.toNormalizedCorners

internal class DocumentEdgePickerFragment : DocumentScanBaseFragment() {

    private var binding: NablaFragmentDocumentEdgePickerBinding? = null

    private val localImageUri: Uri
        get() = requireArguments().getLocalImageUri() ?: throwNablaInternalException("Missing Category Id")

    private val normalizedCorners: NormalizedCorners?
        get() = arguments?.normalizedCorners()

    private val logger by lazy {
        NablaClient.getInstance(sdkNameOrDefault()).coreContainer.logger
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = NablaFragmentDocumentEdgePickerBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return
        binding.toolbar.setNavigationOnClickListener { hostActivity().onBackPressedDispatcher.onBackPressed() }
        setupDocumentCropView(binding.documentCropView)
        binding.buttonCrop.setOnClickListener {
            setFragmentResult(
                REQUEST_KEY,
                generateResultBundle(
                    localImageUri,
                    binding.documentCropView.normalizedCorners?.toNormalizedCorners(),
                ),
            )
            hostActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupDocumentCropView(documentCropView: DocumentCropView) {
        documentCropView.load(localImageUri.toAndroidUri()) {
            crossfade(false)
            listener(
                onSuccess = { _, _ ->
                    val minRatio = 0.1f
                    val maxRatio = 0.9f
                    val defaultCorners = listOf(
                        PointF(minRatio, minRatio),
                        PointF(maxRatio, minRatio),
                        PointF(maxRatio, maxRatio),
                        PointF(minRatio, maxRatio),
                    ).toTypedArray()
                    documentCropView.normalizedCorners = normalizedCorners?.toAndroidArrayList()?.toTypedArray() ?: defaultCorners
                },
                onError = { _, error -> logger.error("failed to load image $localImageUri", error.throwable) },
            )
        }
    }

    companion object {
        const val REQUEST_KEY = "DocumentEdgePickerFragment:RequestKey"
        private const val LOCAL_IMAGE_KEY = "DocumentEdgePickerFragment:localImageUri"
        private const val NORMALIZED_CORNERS_KEY = "DocumentEdgePickerFragment:normalizedCorners"

        internal fun newInstance(
            localImageUri: Uri,
            normalizedCorners: NormalizedCorners?,
            sdkName: String,
        ) = DocumentEdgePickerFragment().apply {
            arguments = Bundle().apply {
                putParcelable(LOCAL_IMAGE_KEY, localImageUri.toAndroidUri())
                putParcelableArrayList(NORMALIZED_CORNERS_KEY, normalizedCorners?.toAndroidArrayList())
            }
            setSdkName(sdkName)
        }

        private fun generateResultBundle(localImageUri: Uri, normalizedCorners: NormalizedCorners?): Bundle {
            return Bundle().apply {
                putParcelable(LOCAL_IMAGE_KEY, localImageUri.toAndroidUri())
                putParcelableArrayList(NORMALIZED_CORNERS_KEY, normalizedCorners?.toAndroidArrayList())
            }
        }

        internal fun Bundle.normalizedCorners(): NormalizedCorners? {
            val corners: Array<PointF>? = getParcelableArrayListCompat(NORMALIZED_CORNERS_KEY, PointF::class.java)?.toTypedArray()
            return corners?.toNormalizedCorners()
        }

        internal fun Bundle.getLocalImageUri(): Uri? {
            return getParcelableCompat(LOCAL_IMAGE_KEY, android.net.Uri::class.java)?.toKtUri()
        }
    }
}
