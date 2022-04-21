package com.nabla.sdk.messaging.ui.scene.messages

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.domain.entity.MimeType
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
import com.nabla.sdk.core.ui.helpers.viewLifeCycleScope
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
import com.nabla.sdk.messaging.ui.scene.messages.adapter.ChatAdapter
import com.nabla.sdk.messaging.ui.scene.messages.editor.MediasToSendAdapter

open class ConversationFragment private constructor() : Fragment() {
    open val nablaMessaging: NablaMessaging = NablaMessaging.instance

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

    private val chatAdapter = ChatAdapter(makeChatAdapterCallbacks())

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
        setupChatRecyclerView(binding)
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
                title = R.string.chat_message_copy_label, // R.string.media_camera_picture_permission_rational_title,
                description = R.string.chat_message_copy_label, // R.string.media_camera_picture_permission_rational_description
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
            binding.chatSendButton.isEnabled = editorState.canSubmit
        }

        viewLifeCycleScope.launchCollect(viewModel.currentMessageFlow) { currentMessage ->
            if (binding.chatEditText.text?.toString() != currentMessage) {
                binding.chatEditText.setText(currentMessage, TextView.BufferType.EDITABLE)
                if (currentMessage != "") {
                    binding.chatEditText.requestFocus()
                }
            }
        }
    }

    private fun collectState(binding: NablaFragmentConversationBinding) {
        viewLifeCycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.errorContainer.isVisible = state is ConversationViewModel.State.Error
            binding.chatLoading.isVisible = state is ConversationViewModel.State.Loading
            binding.chatLoaded.isVisible = state is ConversationViewModel.State.ConversationLoaded

            when (state) {
                is ConversationViewModel.State.ConversationLoaded -> {
                    binding.chatLoaded.visibility = View.VISIBLE

                    updateLoadedDisplay(binding, state)
                }
                ConversationViewModel.State.Loading -> {
                    binding.updateToolbar(
                        title = "loading",
                        subtitle = null,
                        providers = null,
                        displayAvatar = false,
                    )
                }
                is ConversationViewModel.State.Error -> {
                    binding.chatLoaded.visibility = View.GONE
                    binding.updateToolbar(
                        title = "error",
                        subtitle = null,
                        providers = null,
                        displayAvatar = false,
                    )

                    binding.errorRetryButton.setOnClickListener {
                        viewModel.onRetryClicked()
                    }
                }
            }
        }
    }

    private fun wireViewEvents(binding: NablaFragmentConversationBinding) {
        binding.chatTextInputLayoutContainer.clipToOutline = true

        binding.chatToolbarContentContainer.setOnClickListener {
            viewModel.onParticipantsHeaderClicked()
        }

        binding.chatAddMediaButton.setOnClickListener {
            viewModel.onAddMediaButtonClicked()
        }

        binding.chatSendButton.setOnClickListener {
            viewModel.onSendButtonClicked()
        }

        binding.chatEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.onCurrentMessageChanged(text?.toString() ?: "")
        }
    }

    private fun setupChatRecyclerView(binding: NablaFragmentConversationBinding) {
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(binding.context).apply {
                reverseLayout = true
            }
            adapter = chatAdapter
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

        binding.chatMediasToSendRecyclerView.apply {
            layoutManager = LinearLayoutManager(binding.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mediasToSendAdapter
        }

        viewLifeCycleScope.launchCollect(viewModel.mediasToSendFlow) { mediasToSend ->
            mediasToSendAdapter.submitList(mediasToSend)

            binding.chatMediasToSendRecyclerView.visibility =
                if (mediasToSend.isEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }
    }

    private fun makeChatAdapterCallbacks() = object : ChatAdapter.Callbacks {
        override fun onItemClicked(item: TimelineItem) {
            viewModel.onItemClicked(item)
        }

        override fun onProviderClicked(providerId: Uuid) {
            viewModel.onProviderClicked(providerId)
        }

        override fun onDeleteMessage(item: TimelineItem.Message) {
            viewModel.onDeleteMessage(item)
        }

        override fun onCopyMessage(item: TimelineItem.Message.Text) {
            context?.apply {
                copyNewPlainText(
                    label = getString(R.string.chat_message_copy_label),
                    text = item.text
                )
            }
        }

        override fun onUrlClicked(url: String, isFromPatient: Boolean) {
            viewModel.onUrlClicked(url, isFromPatient)
        }
    }

    private fun updateLoadedDisplay(binding: NablaFragmentConversationBinding, state: ConversationViewModel.State.ConversationLoaded) {
        binding.updateToolbar(
            title = state.conversation.title,
            subtitle = state.conversation.lastMessagePreview,
            providers = state.conversation.providersInConversation.map { it.provider },
            displayAvatar = true,
        )

        // Only scroll down automatically if we're at the bottom of the chat && there are new items OR if the view model tells us to
        val shouldScrollToBottomAfterSubmit = (!binding.chatRecyclerView.canScrollDown() && chatAdapter.itemCount < state.items.size) ||
            viewModel.shouldScrollToBottomAfterNextUpdate

        chatAdapter.submitList(state.items) {
            if (shouldScrollToBottomAfterSubmit) {
                binding.chatRecyclerView.scrollToTop()
            }
        }
    }

    private fun NablaFragmentConversationBinding.updateToolbar(
        title: String?,
        subtitle: String?,
        providers: List<User.Provider>?,
        displayAvatar: Boolean,
    ) {
        chatToolbarTitle.setTextOrHide(title)
        chatToolbarSubtitle.setTextOrHide(subtitle)

        val firstProvider = providers?.firstOrNull()
        if (firstProvider != null) {
            chatToolbarAvatarView.loadAvatar(firstProvider)
        } else {
            chatToolbarAvatarView.displaySystemAvatar()
        }
        chatToolbarAvatarView.isVisible = displayAvatar
    }

    @Suppress("UNUSED")
    class Builder internal constructor(private val conversationId: ConversationId) {
        private var customFragment: ConversationFragment? = null

        fun setFragment(fragment: ConversationFragment) {
            customFragment = fragment
        }

        internal fun build(): ConversationFragment {
            return (customFragment ?: ConversationFragment()).apply {
                arguments = newArgsBundle(conversationId)
            }
        }

        companion object {
            private const val CONVERSATION_ID_ARG_KEY = "conversationId"

            private fun newArgsBundle(conversationId: ConversationId): Bundle = Bundle().apply {
                putSerializable(CONVERSATION_ID_ARG_KEY, conversationId.value)
            }

            internal fun conversationIdFromSavedStateHandleOrThrow(savedStateHandle: SavedStateHandle): ConversationId =
                savedStateHandle.get<Uuid>(CONVERSATION_ID_ARG_KEY)?.toConversationId()
                    ?: error("Missing conversationId parameter, make sure you follow the documentation to integrate ConversationFragment.")
        }
    }

    companion object {
        fun newInstance(
            conversationId: ConversationId,
            init: (Builder.() -> Unit)? = null,
        ): ConversationFragment {
            val builder = Builder(conversationId)
            init?.invoke(builder)
            return builder.build()
        }
    }
}
