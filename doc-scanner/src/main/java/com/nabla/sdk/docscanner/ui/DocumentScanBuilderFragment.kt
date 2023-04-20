package com.nabla.sdk.docscanner.ui

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nabla.sdk.core.data.helper.UrlExt.toKtUri
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.core.ui.helpers.CoroutineScopeExtension.launchCollect
import com.nabla.sdk.core.ui.helpers.FragmentCoroutinesExtension.viewLifeCycleScope
import com.nabla.sdk.core.ui.helpers.PermissionExtension.registerForPermissionResult
import com.nabla.sdk.core.ui.helpers.PermissionRational
import com.nabla.sdk.core.ui.helpers.PermissionRequestLauncher
import com.nabla.sdk.core.ui.helpers.SceneHelpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.SceneHelpers.setSdkName
import com.nabla.sdk.core.ui.helpers.TextViewExtension.setTextOrHide
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.viewBinding
import com.nabla.sdk.core.ui.helpers.ViewModelExtension.factoryFor
import com.nabla.sdk.core.ui.helpers.mediapicker.CaptureImageFromCameraActivityContract
import com.nabla.sdk.core.ui.helpers.mediapicker.MediaPickingResult
import com.nabla.sdk.docscanner.R
import com.nabla.sdk.docscanner.core.DocumentScanClient
import com.nabla.sdk.docscanner.core.components.impl.AndroidDocumentDetector
import com.nabla.sdk.docscanner.core.components.impl.AndroidPdfGenerator
import com.nabla.sdk.docscanner.databinding.NablaFragmentDocumentScanBuilderBinding
import com.nabla.sdk.docscanner.ui.DocumentEdgePickerFragment.Companion.getLocalImageUri
import com.nabla.sdk.docscanner.ui.DocumentEdgePickerFragment.Companion.normalizedCorners
import com.nabla.sdk.core.R as CoreR

internal class DocumentScanBuilderFragment : DocumentScanBaseFragment() {

    private val viewModel: DocumentScanBuilderViewModel by viewModels {
        factoryFor {
            val client = getNablaInstanceByName()
            DocumentScanBuilderViewModel(
                client.coreContainer.logger,
                documentScanClient = DocumentScanClient(
                    AndroidPdfGenerator(requireContext().applicationContext, clock = client.coreContainer.clock),
                    AndroidDocumentDetector(requireContext().applicationContext),
                ),
            )
        }
    }

    private val binding by viewBinding(NablaFragmentDocumentScanBuilderBinding::bind)

    private lateinit var captureCameraPictureLauncher: ActivityResultLauncher<Unit>
    private lateinit var captureCameraPicturePermissionsLauncher: PermissionRequestLauncher

    private val adapter by lazy { DocumentAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCameraCapturePictureLauncher()
        setupCameraCapturePicturePermissionsLauncher()
        setFragmentResultListener(DocumentEdgePickerFragment.REQUEST_KEY) { _, bundle ->
            viewModel.onLocalImageUpdated(
                bundle.getLocalImageUri() ?: throwNablaInternalException("DocumentEdgePickerFragment returned no uri"),
                bundle.normalizedCorners() ?: throwNablaInternalException("DocumentEdgePickerFragment returned no corners"),
            )
        }
    }

    private fun setupCameraCapturePicturePermissionsLauncher() {
        captureCameraPicturePermissionsLauncher = registerForPermissionResult(
            permission = Manifest.permission.CAMERA,
            rational = PermissionRational(
                title = R.string.nabla_document_scan_camera_picture_permission_rational_title,
                description = R.string.nabla_document_scan_camera_picture_permission_rational_description,
            ),
        ) { isGranted ->
            if (isGranted) {
                try {
                    captureCameraPictureLauncher.launch(Unit)
                } catch (throwable: Throwable) {
                    viewModel.onErrorLaunchingCameraForImageCapture(throwable)
                }
            }
        }
    }

    private fun setupCameraCapturePictureLauncher() {
        captureCameraPictureLauncher = registerForActivityResult(CaptureImageFromCameraActivityContract(requireContext())) { result ->
            when (result) {
                is MediaPickingResult.Success -> viewModel.onPictureCaptured(result.data)
                is MediaPickingResult.Failure -> viewModel.onErrorWithPictureCapture(result.exception)
                is MediaPickingResult.Cancelled -> viewModel.onImageCaptureCancelled()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return NablaFragmentDocumentScanBuilderBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { hostActivity().onBackPressedDispatcher.onBackPressed() }
        setupRecyclerView()
        binding.buttonCrop.setOnClickListener {
            viewModel.onCropClicked()
        }
        binding.buttonAdd.setOnClickListener {
            viewModel.onAddClicked()
        }
        binding.buttonNext.setOnClickListener {
            viewModel.onSendClicked()
        }
        binding.buttonDelete.setOnClickListener {
            showAskConfirmDeleteDialog()
        }
        collectNavigationEvents()
        collectState()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = adapter
        PagerSnapHelper().attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var lastPosition = RecyclerView.NO_POSITION

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val newPosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                if (newPosition != RecyclerView.NO_POSITION && newPosition != lastPosition) {
                    lastPosition = newPosition
                    viewModel.onScrolledTo(lastPosition)
                }
            }
        })
    }

    private fun collectNavigationEvents() {
        viewLifecycleOwner.launchCollect(viewModel.eventFlow) { event ->
            when (event) {
                DocumentScanBuilderViewModel.Event.OpenCamera -> {
                    captureCameraPicturePermissionsLauncher.launch()
                }
                is DocumentScanBuilderViewModel.Event.Finish -> {
                    hostActivity().finishWithResult(event.resultUri)
                }
                is DocumentScanBuilderViewModel.Event.OpenEdgePicker -> {
                    hostActivity().goToEdgePicker(event.processedImage.image.uri.toKtUri(), event.processedImage.documentCorners)
                }
                is DocumentScanBuilderViewModel.Event.GenericError -> {
                    context?.let { context ->
                        Toast.makeText(context, context.getString(CoreR.string.nabla_error_message_generic_title), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun collectState() {
        viewLifeCycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.progressBar.isVisible = state is DocumentScanBuilderViewModel.State.Loading
            val counterText: String?
            val isNextVisible: Boolean
            if (state is DocumentScanBuilderViewModel.State.Content) {
                val currentImageIndex = state.images.map { it.image.uri }.indexOf(state.currentImage?.image?.uri)
                adapter.submitList(state.images) {
                    binding.recyclerView.scrollToPosition(currentImageIndex)
                }
                counterText = if (state.images.isNotEmpty()) {
                    "${currentImageIndex + 1}/${state.images.size}"
                } else {
                    null
                }
                isNextVisible = state.images.isNotEmpty()
            } else {
                counterText = null
                isNextVisible = false
            }

            binding.counter.setTextOrHide(counterText)
            binding.buttonNext.isVisible = isNextVisible
        }
    }

    private fun showAskConfirmDeleteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.nabla_document_scan_builder_confirm_delete_info)
            .setNegativeButton(R.string.nabla_document_scan_builder_confirm_delete_no_cta) { _, _ -> /* no-op */ }
            .setPositiveButton(R.string.nabla_document_scan_builder_confirm_delete_yes_cta) { _, _ -> viewModel.onDeleteClicked() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
    }

    companion object {
        internal fun newInstance(
            sdkName: String,
        ) = DocumentScanBuilderFragment().apply {
            setSdkName(sdkName)
        }
    }
}
