package com.nabla.sdk.messaging.ui.scene.messages

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.CycleInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.CallSuper
import androidx.core.net.toFile
import androidx.core.net.toUri
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
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_READY
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.ui.helpers.OpenPdfReaderResult
import com.nabla.sdk.core.ui.helpers.canScrollDown
import com.nabla.sdk.core.ui.helpers.canScrollUp
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.mediapicker.CaptureImageFromCameraActivityContract
import com.nabla.sdk.core.ui.helpers.mediapicker.CaptureVideoFromCameraActivityContract
import com.nabla.sdk.core.ui.helpers.mediapicker.MediaPickingResult
import com.nabla.sdk.core.ui.helpers.mediapicker.PickMediasFromLibraryActivityContract
import com.nabla.sdk.core.ui.helpers.openPdfReader
import com.nabla.sdk.core.ui.helpers.player.createMediaPlayersCoordinator
import com.nabla.sdk.core.ui.helpers.scrollToTop
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.core.ui.helpers.toAndroidUri
import com.nabla.sdk.core.ui.helpers.toKtUri
import com.nabla.sdk.core.ui.helpers.viewLifeCycleScope
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MissingConversationIdException
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaFragmentConversationBinding
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import com.nabla.sdk.messaging.ui.fullscreenmedia.scene.FullScreenImageActivity
import com.nabla.sdk.messaging.ui.fullscreenmedia.scene.FullScreenVideoActivity
import com.nabla.sdk.messaging.ui.helper.PermissionRational
import com.nabla.sdk.messaging.ui.helper.PermissionRequestLauncher
import com.nabla.sdk.messaging.ui.helper.copyNewPlainText
import com.nabla.sdk.messaging.ui.helper.registerForPermissionResult
import com.nabla.sdk.messaging.ui.helper.registerForPermissionsResult
import com.nabla.sdk.messaging.ui.scene.messages.ConversationViewModel.EditorState.EditingText
import com.nabla.sdk.messaging.ui.scene.messages.ConversationViewModel.EditorState.RecordingVoice
import com.nabla.sdk.messaging.ui.scene.messages.ConversationViewModel.ErrorAlert
import com.nabla.sdk.messaging.ui.scene.messages.adapter.ConversationAdapter
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.loadReplyContentThumbnailOrHide
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.repliedToAuthorName
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.repliedToContent
import com.nabla.sdk.messaging.ui.scene.messages.editor.MediaToSendAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.nabla.sdk.core.domain.entity.Uri as KtUri

public open class ConversationFragment : Fragment() {
    public open val messagingClient: NablaMessagingClient
        get() = NablaMessagingClient.getInstance()

    private val viewModel: ConversationViewModel by viewModels {
        object : AbstractSavedStateViewModelFactory(this, arguments) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T {
                return ConversationViewModel(
                    messagingClient = messagingClient,
                    savedStateHandle = handle,
                ) as T
            }
        }
    }

    private lateinit var pickMediaFromGalleryLauncher: ActivityResultLauncher<Array<MimeType>>
    private lateinit var captureCameraPictureLauncher: ActivityResultLauncher<Unit>
    private lateinit var captureCameraVideoLauncher: ActivityResultLauncher<Unit>
    private lateinit var captureCameraPicturePermissionsLauncher: PermissionRequestLauncher
    private lateinit var captureCameraVideoPermissionsLauncher: PermissionRequestLauncher
    private lateinit var captureAudioPermissionsLauncher: PermissionRequestLauncher
    private lateinit var mediasToSendAdapter: MediaToSendAdapter
    private var mediaRecorder: MediaRecorder? = null
    private var binding: NablaFragmentConversationBinding? = null

    private val conversationAdapter = ConversationAdapter(makeConversationAdapterCallbacks())
    private val voiceMessagesCoordinator = createMediaPlayersCoordinator(C.CONTENT_TYPE_SPEECH, pauseOnPause = false)

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(MediaSourcePickerBottomSheetFragment.REQUEST_KEY) { _, result ->
            when (MediaSourcePickerBottomSheetFragment.getResult(result)) {
                MediaSource.CAMERA_PICTURE -> captureCameraPicturePermissionsLauncher.launch()
                MediaSource.CAMERA_VIDEO -> captureCameraVideoPermissionsLauncher.launch()
                MediaSource.GALLERY -> viewModel.onImageAndVideoSourceLibrarySelected()
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
        collectVoicePlayersEvents()
        collectVoiceMessageRecordingEvents(binding)
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

        captureCameraVideoPermissionsLauncher = registerForPermissionsResult(
            permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            rational = PermissionRational(
                title = R.string.nabla_conversation_camera_video_permission_rational_title,
                description = R.string.nabla_conversation_camera_video_permission_rational_description,
            )
        ) { grants ->
            if (grants.values.all { it }) {
                viewModel.onMediaSourceCameraVideoSelectedAndPermissionsGranted()
            }
        }

        captureAudioPermissionsLauncher = registerForPermissionsResult(
            permissions = arrayOf(Manifest.permission.RECORD_AUDIO),
            rational = PermissionRational(
                title = R.string.nabla_conversation_voice_message_audio_permission_rational_title,
                description = R.string.nabla_conversation_voice_message_audio_permission_rational_description,
            )
        ) { permissionsGranted ->
            val context = context ?: return@registerForPermissionsResult

            if (permissionsGranted.all { it.value }) {
                viewLifeCycleScope.launch {
                    try {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        val targetFile = withContext(Dispatchers.IO) {
                            File(context.cacheDir, "voice_message_${System.currentTimeMillis()}")
                                .apply { createNewFile() }
                        }
                        viewModel.onVoiceRecorderFileReadyAndPermissionGranted(targetFile.toUri().toKtUri())
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        viewModel.onFailedToRecordVoiceMessage(e)
                    }
                }
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
                is MediaPickingResult.Failure -> viewModel.onErrorWithCameraCapture(result.exception)
                is MediaPickingResult.Cancelled -> Unit // no-op
            }
        }

        captureCameraVideoLauncher = registerForActivityResult(CaptureVideoFromCameraActivityContract()) { result ->
            when (result) {
                is MediaPickingResult.Success -> viewModel.onVideoCaptured(result.data)
                is MediaPickingResult.Failure -> viewModel.onErrorWithCameraCapture(result.exception)
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
                        viewModel.onErrorLaunchingCameraCapture(t)
                    }
                }
                ConversationViewModel.NavigationEvent.OpenCameraVideoCapture -> {
                    try {
                        captureCameraVideoLauncher.launch(Unit)
                    } catch (t: Throwable) {
                        viewModel.onErrorLaunchingCameraCapture(t)
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
                is ConversationViewModel.NavigationEvent.OpenFullScreenVideo -> {
                    startActivity(FullScreenVideoActivity.newIntent(requireActivity(), event.videoUri))
                }
                ConversationViewModel.NavigationEvent.RequestVoiceMessagePermissions -> captureAudioPermissionsLauncher.launch()
                is ConversationViewModel.NavigationEvent.ScrollToItem -> {
                    binding?.conversationRecyclerView?.smoothScrollToPosition(event.position)
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
            binding.conversationSendButton.isEnabled = editorState is RecordingVoice || (editorState is EditingText && editorState.canSubmit)
            binding.conversationSendButton.isVisible = editorState is RecordingVoice || editorState is EditingText

            binding.currentlyReplyingToLayout.isVisible = editorState is EditingText && editorState.replyingTo != null
            binding.conversationTextInputLayoutContainer.isVisible = editorState is EditingText
            binding.conversationAddMediaButton.isVisible = editorState is EditingText
            binding.conversationRecordVoiceButton.isVisible = editorState is EditingText

            binding.conversationCancelRecordingButton.isVisible = editorState is RecordingVoice
            binding.conversationRecordingVoiceProgress.isVisible = editorState is RecordingVoice

            binding.conversationComposerLayout.isVisible = editorState != ConversationViewModel.EditorState.Hidden

            when (editorState) {
                ConversationViewModel.EditorState.Hidden -> Unit /* no-op */
                is RecordingVoice -> {
                    val minutes = editorState.recordProgressSeconds / 60
                    val seconds = editorState.recordProgressSeconds % 60
                    binding.conversationRecordingVoiceProgressText.text = binding.context
                        .getString(R.string.nabla_conversation_audio_message_seconds_format, minutes, seconds)

                    // half-cycle sinusoidal blinking between 0 and 1
                    ObjectAnimator
                        .ofFloat(binding.conversationRecordingVoiceProgressDot, "alpha", 0f, 1f)
                        .apply {
                            duration = 1_000
                            interpolator = CycleInterpolator(/* cycles = */ 0.5f)
                            setAutoCancel(true)
                            start()
                        }
                }
                is EditingText -> {
                    if (editorState.replyingTo != null) {
                        binding.currentlyReplyingToBody.text = editorState.replyingTo.repliedToContent(binding.context)
                        binding.currentlyReplyingToTitle.text = getString(
                            R.string.nabla_conversation_composer_replying_to_title_author,
                            editorState.replyingTo.repliedToAuthorName(binding.context),
                        )
                        binding.currentlyReplyingToThumbnail.loadReplyContentThumbnailOrHide(editorState.replyingTo.content)
                    }
                }
            }
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

        binding.conversationAddMediaButton.setOnClickListener { viewModel.onAddMediaButtonClicked() }
        binding.conversationSendButton.setOnClickListener { viewModel.onSendButtonClicked() }
        binding.conversationRecordVoiceButton.setOnClickListener { viewModel.onRecordVoiceMessageClicked() }
        binding.conversationCancelRecordingButton.setOnClickListener { viewModel.onCancelVoiceMessageClicked() }
        binding.currentlyReplyingToCancel.setOnClickListener { viewModel.onCancelReplyToMessage() }

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
        mediasToSendAdapter = MediaToSendAdapter(
            onMediaClickedListener = { clickedMedia ->
                viewModel.onMediaToSendClicked(clickedMedia)
            },
            onDeleteMediaToSendClickListener = { removedItem ->
                viewModel.onMediaToSendRemoved(removedItem)
            },
            onErrorLoadingVideoThumbnail = { error ->
                viewModel.onErrorFetchingVideoThumbnail(error)
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

        override fun onReplyToMessage(item: TimelineItem.Message) {
            viewModel.onReplyToMessage(item)
        }

        override fun onUrlClicked(url: String, isFromPatient: Boolean) {
            viewModel.onUrlClicked(url)
        }

        override fun onToggleAudioMessagePlay(audioMessageUri: KtUri) {
            viewModel.onToggleVoiceMessagePlay(audioMessageUri)
        }

        override fun onRepliedMessageClicked(messageId: MessageId) {
            viewModel.onRepliedMessageClicked(messageId)
        }

        override fun onErrorFetchingVideoThumbnail(error: Throwable) {
            viewModel.onErrorFetchingVideoThumbnail(error)
        }
    }

    private fun updateLoadedDisplay(binding: NablaFragmentConversationBinding, state: ConversationViewModel.State.ConversationLoaded) {
        binding.updateToolbar(
            title = state.conversation.title ?: state.conversation.inboxPreviewTitle,
            subtitle = state.conversation.subtitle,
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

    private fun collectVoicePlayersEvents() {
        // prepare players
        viewLifeCycleScope.launchCollect(viewModel.voiceMessagesProgressFlow) { voiceMessages ->
            voiceMessages.forEach { (uri, _) -> getOrSetupPlayerForVoiceMessage(uri) }
        }

        // forward play/pause commands
        viewLifeCycleScope.launchCollect(viewModel.nowPlayingVoiceMessageFlow) { uri ->
            if (uri != null) {
                getOrSetupPlayerForVoiceMessage(uri)?.play()
                startPlaybackProgressPolling()
            } else {
                voiceMessagesCoordinator.getAll().forEach { (_, player) -> player.pause() }
                stopPlaybackProgressPolling()
            }
        }
    }

    private fun collectVoiceMessageRecordingEvents(binding: NablaFragmentConversationBinding) {
        viewLifeCycleScope.launchCollect(viewModel.currentlyRecordingVoiceFlow) { ongoingRecord ->
            try {
                withContext(Dispatchers.IO) {
                    if (ongoingRecord != null) {
                        @Suppress("DEPRECATION")
                        val newOrExistingRecorder = mediaRecorder ?: MediaRecorder().apply {
                            requestVoiceAudioFocus(binding)
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFile(ongoingRecord.targetUri.toAndroidUri().toFile().absolutePath)
                            // if you ever change format or encoder make sure "stop after pause" doesn't freeze the app
                            // see https://issuetracker.google.com/issues/178630865
                            setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                            setOnErrorListener { _, what, extra ->
                                viewModel.onVoiceMessageError("error code= $what, extra info: $extra")
                            }
                            @Suppress("BlockingMethodInNonBlockingContext")
                            prepare()
                            start()
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            if (ongoingRecord.isPaused) {
                                newOrExistingRecorder.pause()
                            } else {
                                newOrExistingRecorder.resume()
                            }
                        } // else: on Android < N, we don't have the pause/resume API

                        mediaRecorder = newOrExistingRecorder
                    } else {
                        stopAndReleaseMediaRecorder()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    private fun stopAndReleaseMediaRecorder() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun requestVoiceAudioFocus(binding: NablaFragmentConversationBinding) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (binding.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setOnAudioFocusChangeListener { if (it == AudioManager.AUDIOFOCUS_LOSS) viewModel.onLostAudioFocus() }
                    .build()
            )
        } // else no-op
    }

    private var playbackProgressPollingJob: Job? = null

    private fun startPlaybackProgressPolling() {
        stopPlaybackProgressPolling()
        playbackProgressPollingJob = viewLifeCycleScope.launch {
            while (isActive) {
                voiceMessagesCoordinator.getAll().forEach { (uri, player) ->
                    if (player.isPlaying) {
                        reportProgress(uri.toKtUri(), player)
                    }
                }
                delay(AUDIO_PLAYBACK_PROGRESS_POLLING_MS)
            }
        }
    }

    private fun stopPlaybackProgressPolling() {
        playbackProgressPollingJob?.cancel()
    }

    private fun reportProgress(uri: KtUri, player: Player) {
        viewModel.onVoiceMessagePlaybackProgress(
            voiceMessageUri = uri,
            position = player.currentPosition,
            totalDuration = player.duration
                .let { if (it == C.TIME_UNSET) null else it }
        )
    }

    private fun getOrSetupPlayerForVoiceMessage(uri: KtUri): Player? {
        return voiceMessagesCoordinator.getOrCreatePlayerForUri(context ?: return null, uri.toAndroidUri()) {
            playWhenReady = false
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        viewModel.onVoiceMessagePlaybackStarted(uri)
                    } else {
                        viewModel.onVoiceMessagePlaybackStopped(uri)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        STATE_READY -> {
                            // now we know the total length of media, report it
                            reportProgress(uri, player = this@getOrCreatePlayerForUri)
                        }
                        STATE_ENDED -> {
                            // on end of playback, reset position to be ready to replay
                            seekTo(0)
                        }
                        else -> Unit /* no-op */
                    }
                }
            })
        }
    }

    private fun NablaFragmentConversationBinding.updateToolbar(
        title: String?,
        subtitle: String?,
        providers: List<Provider>?,
        displayAvatar: Boolean,
    ) {
        conversationToolbarTitle.setTextOrHide(title)
        conversationToolbarSubtitle.setTextOrHide(subtitle)

        val firstProvider = providers?.firstOrNull()
        if (firstProvider != null) {
            conversationToolbarAvatarView.loadAvatar(firstProvider)
        } else {
            conversationToolbarAvatarView.displayUnicolorPlaceholder()
        }
        conversationToolbarAvatarView.isVisible = displayAvatar
    }

    private fun showErrorAlert(errorAlert: ErrorAlert) {
        context?.let { context ->
            Toast.makeText(context, context.getString(errorAlert.errorMessageRes), Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("UNUSED")
    public class Builder internal constructor(private val conversationId: ConversationId) {
        private var customFragment: ConversationFragment? = null
        private var showComposer = true

        /**
         * Call this to pass a custom child class of [ConversationFragment] you want to use
         * instead of the base one. This is useful if you want to override the default [NablaMessagingClient]
         * used by the fragment.
         */
        public fun setFragment(fragment: ConversationFragment) {
            customFragment = fragment
        }

        /**
         * Call this method if you want to hide the message composer for the patient. By doing so,
         * they won't be able to send a message in the conversation. By default, the composer is shown.
         */
        public fun setShowComposer(showComposer: Boolean) {
            this.showComposer = showComposer
        }

        internal fun build(): ConversationFragment {
            return (customFragment ?: ConversationFragment()).apply {
                arguments = newArgsBundle(conversationId, showComposer)
            }
        }

        internal companion object {
            private const val CONVERSATION_ID_ARG_KEY = "conversationId"
            private const val SHOW_COMPOSER_ARG_KEY = "showComposer"

            private fun newArgsBundle(
                conversationId: ConversationId,
                showComposer: Boolean,
            ): Bundle = Bundle().apply {
                putSerializable(CONVERSATION_ID_ARG_KEY, conversationId.value)
                putBoolean(SHOW_COMPOSER_ARG_KEY, showComposer)
            }

            internal fun conversationIdFromSavedStateHandleOrThrow(savedStateHandle: SavedStateHandle): ConversationId =
                savedStateHandle.get<Uuid>(CONVERSATION_ID_ARG_KEY)?.toConversationId()
                    ?: throw MissingConversationIdException

            internal fun showComposerFromSavedStateHandle(savedStateHandle: SavedStateHandle): Boolean =
                savedStateHandle.get<Boolean>(SHOW_COMPOSER_ARG_KEY) ?: true
        }
    }

    public companion object {
        internal const val AUDIO_PLAYBACK_PROGRESS_POLLING_MS = 200L

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
