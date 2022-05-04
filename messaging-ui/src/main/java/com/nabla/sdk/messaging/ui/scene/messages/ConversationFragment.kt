package com.nabla.sdk.messaging.ui.scene.messages

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.core.ui.helpers.OpenPdfReaderResult
import com.nabla.sdk.core.ui.helpers.canScrollDown
import com.nabla.sdk.core.ui.helpers.canScrollUp
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.mediapicker.CaptureImageFromCameraActivityContract
import com.nabla.sdk.core.ui.helpers.mediapicker.MediaPickingResult
import com.nabla.sdk.core.ui.helpers.mediapicker.PickMediasFromLibraryActivityContract
import com.nabla.sdk.core.ui.helpers.openPdfReader
import com.nabla.sdk.core.ui.helpers.scrollToTop
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.core.ui.helpers.toAndroidUri
import com.nabla.sdk.core.ui.helpers.viewLifeCycleScope
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaFragmentConversationBinding
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import com.nabla.sdk.messaging.ui.fullscreenmedia.scene.FullScreenImageActivity
import com.nabla.sdk.messaging.ui.helper.PermissionRational
import com.nabla.sdk.messaging.ui.helper.PermissionRequestLauncher
import com.nabla.sdk.messaging.ui.helper.copyNewPlainText
import com.nabla.sdk.messaging.ui.helper.registerForPermissionResult
import com.nabla.sdk.messaging.ui.scene.messages.ConversationViewModel.ErrorAlert
import com.nabla.sdk.messaging.ui.scene.messages.adapter.ConversationAdapter
import com.nabla.sdk.messaging.ui.scene.messages.editor.MediasToSendAdapter

public open class ConversationFragment : Fragment() {
    public open val nablaMessaging: NablaMessaging = NablaMessaging.getInstance()

    private val viewModel: ConversationViewModel by viewModels {
        object : AbstractSavedStateViewModelFactory(this, arguments) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T {
                return ConversationViewModel(
                    nablaMessaging = nablaMessaging,
                    savedStateHandle = handle,
                ) as T
            }
        }
    }

    private lateinit var pickMediaFromGalleryLauncher: ActivityResultLauncher<Array<MimeType>>
    private lateinit var captureCameraPictureLauncher: ActivityResultLauncher<Unit>
    private lateinit var captureCameraPicturePermissionsLauncher: PermissionRequestLauncher
    private lateinit var mediasToSendAdapter: MediasToSendAdapter
    private var binding: NablaFragmentConversationBinding? = null

    private val conversationAdapter = ConversationAdapter(makeConversationAdapterCallbacks())

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(MediaSourcePickerBottomSheetFragment.REQUEST_KEY) { _, result ->
            when (MediaSourcePickerBottomSheetFragment.getResult(result)) {
                MediaSource.CAMERA_PICTURE -> captureCameraPicturePermissionsLauncher.launch()
                MediaSource.GALLERY -> viewModel.onImageSourceLibrarySelected()
                MediaSource.DOCUMENT -> viewModel.onDocumentSourceLibrarySelected()
            }
        }

        setupMediaCaptureLaunchers()
        setupPermissionsLaunchers()
    }

    final override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater =
        super.onGetLayoutInflater(savedInstanceState)
            .cloneInContext(context?.withNablaMessagingThemeOverlays())

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = NablaFragmentConversationBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return
        setupToolbarNav(binding)
        setupMediasToSendRecyclerView(binding)
        wireViewEvents(binding)
        setupConversationRecyclerView(binding)
        collectAlertEvents()
        collectNavigationEvents()
        collectState(binding)
        collectEditorState(binding)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()

        viewModel.onViewStart()
    }

    @CallSuper
    override fun onStop() {
        viewModel.onViewStop()

        super.onStop()
    }

    @CallSuper
    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setupToolbarNav(binding: NablaFragmentConversationBinding) {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun setupPermissionsLaunchers() {
        captureCameraPicturePermissionsLauncher = registerForPermissionResult(
            permission = Manifest.permission.CAMERA,
            rational = PermissionRational(
                title = R.string.nabla_conversation_camera_picture_permission_rational_title,
                description = R.string.nabla_conversation_camera_picture_permission_rational_description,
            )
        ) { isGranted ->
            if (isGranted) {
                viewModel.onMediaSourceCameraPictureSelectedAndPermissionsGranted()
            }
        }
    }

    private fun setupMediaCaptureLaunchers() {
        val context = context ?: return

        pickMediaFromGalleryLauncher = registerForActivityResult(PickMediasFromLibraryActivityContract(context)) { result ->
            when (result) {
                is MediaPickingResult.Success -> viewModel.onMediasPickedFromGallery(result.data)
                is MediaPickingResult.Failure -> viewModel.onErrorWithMediaPicker(result.exception)
                is MediaPickingResult.Cancelled -> Unit // no-op
            }
        }

        captureCameraPictureLauncher = registerForActivityResult(CaptureImageFromCameraActivityContract()) { result ->
            when (result) {
                is MediaPickingResult.Success -> viewModel.onPictureCaptured(result.data)
                is MediaPickingResult.Failure -> viewModel.onErrorWithPictureCapture(result.exception)
                is MediaPickingResult.Cancelled -> Unit // no-op
            }
        }
    }

    private fun collectAlertEvents() {
        viewLifecycleOwner.launchCollect(viewModel.errorAlertEventFlow) { errorAlert ->
            showErrorAlert(errorAlert)
        }
    }

    private fun collectNavigationEvents() {
        viewLifecycleOwner.launchCollect(viewModel.navigationEventFlow) { event ->
            when (event) {
                ConversationViewModel.NavigationEvent.OpenCameraPictureCapture -> {
                    try {
                        captureCameraPictureLauncher.launch(Unit)
                    } catch (t: Throwable) {
                        viewModel.onErrorLaunchingCameraForImageCapture(t)
                    }
                }
                is ConversationViewModel.NavigationEvent.OpenMediaLibrary -> {
                    try {
                        pickMediaFromGalleryLauncher.launch(event.mimeTypes.toTypedArray())
                    } catch (t: Throwable) {
                        viewModel.onErrorLaunchingLibrary(t)
                    }
                }
                ConversationViewModel.NavigationEvent.OpenMediaSourcePicker -> {
                    parentFragmentManager.commit {
                        add(MediaSourcePickerBottomSheetFragment(), "MediaSourcePicker")
                    }
                }
                is ConversationViewModel.NavigationEvent.OpenWebBrowser -> {
                    openUri(event.url.toAndroidUri())
                }
                is ConversationViewModel.NavigationEvent.OpenUriExternally -> {
                    openUri(event.uri.toAndroidUri())
                }
                is ConversationViewModel.NavigationEvent.OpenFullScreenPdf -> {
                    when (val openPdfResult = openPdfReader(requireActivity(), event.fileUri)) {
                        is OpenPdfReaderResult.NoPdfReader.ErrorOpeningPlayStoreToInstallReaderApp -> {
                            viewModel.onErrorOpeningLink(openPdfResult.error)
                        }
                        else -> Unit /* no-op */
                    }
                }
                is ConversationViewModel.NavigationEvent.OpenFullScreenImage -> {
                    startActivity(FullScreenImageActivity.newIntent(requireActivity(), event.imageUri))
                }
            }
        }
    }

    private fun openUri(uri: Uri) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                .apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            startActivity(browserIntent)
        } catch (t: Throwable) {
            viewModel.onErrorOpeningLink(t)
        }
    }

    private fun collectEditorState(binding: NablaFragmentConversationBinding) {
        viewLifeCycleScope.launchCollect(viewModel.editorStateFlow) { editorState ->
            binding.conversationSendButton.isEnabled = editorState.canSubmit
        }

        viewLifeCycleScope.launchCollect(viewModel.currentMessageFlow) { currentMessage ->
            if (binding.conversationEditText.text?.toString() != currentMessage) {
                binding.conversationEditText.setText(currentMessage, TextView.BufferType.EDITABLE)
                if (currentMessage != "") {
                    binding.conversationEditText.requestFocus()
                }
            }
        }
    }

    private fun collectState(binding: NablaFragmentConversationBinding) {
        viewLifeCycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.nablaIncludedErrorLayout.root.isVisible = state is ConversationViewModel.State.Error
            binding.conversationLoading.isVisible = state is ConversationViewModel.State.Loading
            binding.conversationLoaded.isVisible = state is ConversationViewModel.State.ConversationLoaded

            when (state) {
                is ConversationViewModel.State.ConversationLoaded -> {

                    updateLoadedDisplay(binding, state)
                }
                ConversationViewModel.State.Loading -> {
                    binding.updateToolbar(
                        title = getString(R.string.nabla_conversation_header_loading),
                        subtitle = null,
                        providers = null,
                        displayAvatar = false,
                    )
                }
                is ConversationViewModel.State.Error -> {
                    binding.updateToolbar(
                        title = getString(R.string.nabla_conversation_header_error),
                        subtitle = null,
                        providers = null,
                        displayAvatar = false,
                    )

                    binding.nablaIncludedErrorLayout.bind(state.error, viewModel::onRetryClicked)
                }
            }
        }
    }

    private fun wireViewEvents(binding: NablaFragmentConversationBinding) {
        binding.conversationTextInputLayoutContainer.clipToOutline = true

        binding.conversationAddMediaButton.setOnClickListener {
            viewModel.onAddMediaButtonClicked()
        }

        binding.conversationSendButton.setOnClickListener {
            viewModel.onSendButtonClicked()
        }

        binding.conversationEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.onCurrentMessageChanged(text?.toString() ?: "")
        }
    }

    private fun setupConversationRecyclerView(binding: NablaFragmentConversationBinding) {
        binding.conversationRecyclerView.apply {
            layoutManager = LinearLayoutManager(binding.context).apply {
                reverseLayout = true
            }
            adapter = conversationAdapter
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (!recyclerView.canScrollUp()) {
                            viewModel.onTimelineReachedTop()
                        }
                    }
                }
            )
        }
    }

    private fun setupMediasToSendRecyclerView(binding: NablaFragmentConversationBinding) {
        mediasToSendAdapter = MediasToSendAdapter(
            onMediaClickedListener = { clickedMedia ->
                viewModel.onMediaToSendClicked(clickedMedia)
            },
            onDeleteMediaToSendClickListener = { removedItem ->
                viewModel.onMediaToSendRemoved(removedItem)
            },
        )

        binding.conversationMediasToSendRecyclerView.apply {
            layoutManager = LinearLayoutManager(binding.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mediasToSendAdapter
        }

        viewLifeCycleScope.launchCollect(viewModel.mediasToSendFlow) { mediasToSend ->
            mediasToSendAdapter.submitList(mediasToSend)

            binding.conversationMediasToSendRecyclerView.visibility =
                if (mediasToSend.isEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }
    }

    private fun makeConversationAdapterCallbacks() = object : ConversationAdapter.Callbacks {
        override fun onItemClicked(item: TimelineItem) {
            viewModel.onItemClicked(item)
        }

        override fun onProviderClicked(providerId: Uuid) {
            /* not supported for the moment */
        }

        override fun onDeleteMessage(item: TimelineItem.Message) {
            viewModel.onDeleteMessage(item)
        }

        override fun onCopyMessage(item: TimelineItem.Message.Text) {
            context?.apply {
                copyNewPlainText(
                    label = getString(R.string.nabla_conversation_message_copy_label),
                    text = item.text
                )
            }
        }

        override fun onUrlClicked(url: String, isFromPatient: Boolean) {
            viewModel.onUrlClicked(url)
        }
    }

    private fun updateLoadedDisplay(binding: NablaFragmentConversationBinding, state: ConversationViewModel.State.ConversationLoaded) {
        binding.updateToolbar(
            title = state.conversation.inboxPreviewTitle,
            subtitle = state.conversation.lastMessagePreview,
            providers = state.conversation.providersInConversation.map { it.provider },
            displayAvatar = true,
        )

        // Only scroll down automatically if we're at the bottom of the conversation && there are new items OR if the view model tells us to
        val shouldScrollToBottomAfterSubmit =
            (!binding.conversationRecyclerView.canScrollDown() && conversationAdapter.itemCount < state.items.size) ||
                viewModel.shouldScrollToBottomAfterNextUpdate

        conversationAdapter.submitList(state.items) {
            if (shouldScrollToBottomAfterSubmit) {
                binding.conversationRecyclerView.scrollToTop()
            }
        }
    }

    private fun NablaFragmentConversationBinding.updateToolbar(
        title: String?,
        subtitle: String?,
        providers: List<User.Provider>?,
        displayAvatar: Boolean,
    ) {
        conversationToolbarTitle.setTextOrHide(title)
        conversationToolbarSubtitle.setTextOrHide(subtitle)

        val firstProvider = providers?.firstOrNull()
        if (firstProvider != null) {
            conversationToolbarAvatarView.loadAvatar(firstProvider)
        } else {
            conversationToolbarAvatarView.displaySystemAvatar()
        }
        conversationToolbarAvatarView.isVisible = displayAvatar
    }

    private fun showErrorAlert(errorAlert: ErrorAlert) {
        context?.let { context ->
            Toast.makeText(context, errorAlert.defaultMessage(context), Toast.LENGTH_SHORT).show()
        }
    }

    private fun ErrorAlert.defaultMessage(context: Context): String {
        val resId = when (this) {
            is ErrorAlert.LoadingMoreItems -> R.string.nabla_error_message_conversation_loading_more
            is ErrorAlert.AttachmentMediaPicker -> R.string.nabla_error_message_conversation_attachment_media_picker
            is ErrorAlert.AttachmentCameraCapturing -> R.string.nabla_error_message_conversation_attachment_camera_capturing
            is ErrorAlert.AttachmentCameraOpening -> R.string.nabla_error_message_conversation_attachment_camera_opening
            is ErrorAlert.AttachmentLibraryOpening -> R.string.nabla_error_message_conversation_attachment_library_opening
            is ErrorAlert.LinkOpening -> R.string.nabla_error_message_conversation_link_opening
            is ErrorAlert.DeletingMessage -> R.string.nabla_error_message_conversation_deleting_message
            is ErrorAlert.ClickedUrlParsing -> R.string.nabla_error_message_conversation_clicked_url_parsing
        }

        return context.getString(resId)
    }

    @Suppress("UNUSED")
    public class Builder internal constructor(private val conversationId: ConversationId) {
        private var customFragment: ConversationFragment? = null

        public fun setFragment(fragment: ConversationFragment) {
            customFragment = fragment
        }

        internal fun build(): ConversationFragment {
            return (customFragment ?: ConversationFragment()).apply {
                arguments = newArgsBundle(conversationId)
            }
        }

        internal companion object {
            private const val CONVERSATION_ID_ARG_KEY = "conversationId"

            private fun newArgsBundle(conversationId: ConversationId): Bundle = Bundle().apply {
                putSerializable(CONVERSATION_ID_ARG_KEY, conversationId.value)
            }

            internal fun conversationIdFromSavedStateHandleOrThrow(savedStateHandle: SavedStateHandle): ConversationId =
                savedStateHandle.get<Uuid>(CONVERSATION_ID_ARG_KEY)?.toConversationId()
                    ?: throw NablaException.MissingConversationId
        }
    }

    public companion object {
        public fun newInstance(
            conversationId: ConversationId,
            init: (Builder.() -> Unit)? = null,
        ): ConversationFragment {
            val builder = Builder(conversationId)
            init?.invoke(builder)
            return builder.build()
        }
    }
}
