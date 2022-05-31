package com.nabla.sdk.messaging.ui.scene.messages

import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.data.helper.toJvmUri
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.helpers.mediapicker.LocalMedia
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems
import com.nabla.sdk.messaging.core.domain.entity.FileLocal
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.scene.messages.ConversationFragment.Builder.Companion.conversationIdFromSavedStateHandleOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.net.URI
import kotlin.time.Duration.Companion.seconds

internal class ConversationViewModel(
    private val messagingClient: NablaMessagingClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private var latestLoadMoreCallback: (@CheckResult suspend () -> Result<Unit>)? = null

    private val retryAfterErrorTriggerFlow = MutableSharedFlow<Unit>()
    private val selectedMessageIdFlow = MutableStateFlow<MessageId?>(null)

    val stateFlow: StateFlow<State>

    val editorStateFlow: StateFlow<EditorState>

    private val currentMessageStateFlow = MutableStateFlow("")
    val currentMessageFlow: Flow<String> = currentMessageStateFlow

    private val navigationEventMutableFlow = MutableLiveFlow<NavigationEvent>()
    val navigationEventFlow: LiveFlow<NavigationEvent> = navigationEventMutableFlow

    private val errorAlertMutableFlow = MutableLiveFlow<ErrorAlert>()
    val errorAlertEventFlow: LiveFlow<ErrorAlert> = errorAlertMutableFlow

    private val mediasToSendMutableFlow = MutableStateFlow<List<LocalMedia>>(emptyList())
    val mediasToSendFlow: Flow<List<LocalMedia>> = mediasToSendMutableFlow

    private val scrollToBottomAfterNextUpdateMutableFlow = MutableStateFlow(false)
    val shouldScrollToBottomAfterNextUpdate get() = scrollToBottomAfterNextUpdateMutableFlow.compareAndSet(expect = true, update = false)

    private val voiceMessagesProgressMutableFlow = MutableStateFlow(emptyMap<Uri, PlaybackProgress>())
    val voiceMessagesProgressFlow: Flow<Map<Uri, PlaybackProgress>> = voiceMessagesProgressMutableFlow

    private val nowPlayingVoiceMessageMutableFlow = MutableStateFlow<Uri?>(null)
    val nowPlayingVoiceMessageFlow: Flow<Uri?> = nowPlayingVoiceMessageMutableFlow

    private val currentlyRecordingVoiceMutableFlow = MutableStateFlow<OngoingVoiceRecording?>(null)
    val currentlyRecordingVoiceFlow: Flow<OngoingVoiceRecording?> = currentlyRecordingVoiceMutableFlow

    private var lastTypingEventSentAt: Instant = Instant.DISTANT_PAST
    private var isViewForeground = false

    private val conversationId: ConversationId = conversationIdFromSavedStateHandleOrThrow(savedStateHandle)

    init {
        stateFlow = makeStateFlow(
            messagingClient.watchConversation(conversationId)
                .handleConversationDataSideEffects(),
            messagingClient
                .watchConversationItems(conversationId)
                .handleConversationMessagesSideEffects()
        )

        editorStateFlow = combine(
            currentMessageStateFlow,
            mediasToSendMutableFlow,
            currentlyRecordingVoiceMutableFlow,
        ) { currentMessage, mediasToSend, ongoingVoiceRecording ->
            when {
                ongoingVoiceRecording != null -> {
                    EditorState.RecordingVoice(recordProgressSeconds = ongoingVoiceRecording.secondsSoFar)
                }
                else -> {
                    EditorState.EditingText(
                        canSubmit = currentMessage.isNotBlank() || mediasToSend.isNotEmpty(),
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = EditorState.EditingText(canSubmit = false))
    }

    private fun makeStateFlow(
        conversationDataFlow: Flow<Conversation>,
        conversationItemsFlow: Flow<WatchPaginatedResponse<ConversationItems>>,
    ): StateFlow<State> {
        return combine(
            conversationDataFlow,
            conversationItemsFlow,
            selectedMessageIdFlow,
            voiceMessagesProgressMutableFlow,
            nowPlayingVoiceMessageMutableFlow,
            StateMapper()::mapToState,
        )
            .retryWhen { throwable, _ ->
                messagingClient.logger.error("Failed to fetch conversation messages", throwable, tag = LOGGING_TAG)
                emit(
                    State.Error(
                        if (throwable is NablaException.Network) ErrorUiModel.Network else ErrorUiModel.Generic
                    )
                )

                retryAfterErrorTriggerFlow.first()
                emit(State.Loading)
                true
            }
            .stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, initialValue = State.Loading)
    }

    private fun Flow<Conversation>.handleConversationDataSideEffects() =
        onEach { conversation ->
            conversation.markConversationAsReadIfNeeded()
        }

    private fun Flow<WatchPaginatedResponse<ConversationItems>>.handleConversationMessagesSideEffects() =
        onEach { response ->
            latestLoadMoreCallback = response.loadMore
        }

    fun onViewStart() {
        isViewForeground = true

        // Mark conversation as read if a new message has been received while in background
        (stateFlow.value as? State.ConversationLoaded)?.conversation?.markConversationAsReadIfNeeded()
    }

    fun onViewStop() {
        isViewForeground = false
    }

    fun onRecordVoiceMessageClicked() {
        navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.RequestVoiceMessagePermissions)
    }

    fun onAddMediaButtonClicked() {
        navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenMediaSourcePicker)
    }

    fun onMediaSourceCameraPictureSelectedAndPermissionsGranted() {
        navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenCameraPictureCapture)
    }

    fun onImageSourceLibrarySelected() {
        navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenMediaLibrary(listOf(MimeType.Image.JPEG, MimeType.Image.PNG)))
    }

    fun onDocumentSourceLibrarySelected() {
        navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenMediaLibrary(listOf(MimeType.Application.PDF)))
    }

    fun onMediasPickedFromGallery(medias: List<LocalMedia>) {
        mediasToSendMutableFlow.emitIn(viewModelScope, mediasToSendMutableFlow.value.plus(medias))
    }

    fun onPictureCaptured(picture: LocalMedia.Image) {
        mediasToSendMutableFlow.emitIn(viewModelScope, mediasToSendMutableFlow.value.plus(picture))
    }

    fun onMediaToSendClicked(media: LocalMedia) {
        when (media) {
            is LocalMedia.Image -> navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenFullScreenImage(media.uri))
            is LocalMedia.Document -> {
                if (media.mimeType == MimeType.Application.PDF) {
                    navigationEventMutableFlow.emitIn(
                        viewModelScope,
                        NavigationEvent.OpenFullScreenPdf(media.uri)
                    )
                } else {
                    navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenUriExternally(media.uri))
                }
            }
        }
    }

    fun onMediaToSendRemoved(removedItem: LocalMedia) {
        mediasToSendMutableFlow.emitIn(viewModelScope, mediasToSendMutableFlow.value.minus(removedItem))
    }

    fun onErrorWithMediaPicker(error: Exception) {
        messagingClient.logger.error("Error in new media attachment picker", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.AttachmentMediaPicker)
    }

    fun onErrorWithPictureCapture(error: Exception) {
        messagingClient.logger.error("Error in camera for new attachment", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.AttachmentCameraCapturing)
    }

    fun onErrorLaunchingCameraForImageCapture(error: Throwable) {
        messagingClient.logger.error("Failed open camera for new attachment", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.AttachmentCameraOpening)
    }

    fun onErrorLaunchingLibrary(error: Throwable) {
        messagingClient.logger.error("Failed open media gallery for new attachment", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.AttachmentLibraryOpening)
    }

    fun onErrorOpeningLink(error: Throwable) {
        messagingClient.logger.error("Failed to open link", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.LinkOpening)
    }

    private var voiceRecordingDurationIncrementJob: Job? = null

    fun onVoiceRecorderFileReadyAndPermissionGranted(recordingTargetUri: Uri) {
        voiceRecordingDurationIncrementJob?.cancel()
        voiceRecordingDurationIncrementJob = viewModelScope.launch {
            while (isActive && currentlyRecordingVoiceMutableFlow.value?.isPaused != true) {
                // increment previous or start at zero if null
                val secondsSoFar = (currentlyRecordingVoiceMutableFlow.value?.secondsSoFar ?: -1) + 1

                if (secondsSoFar < VOICE_MESSAGE_MAX_LENGTH_SECONDS) {
                    currentlyRecordingVoiceMutableFlow.value = OngoingVoiceRecording(recordingTargetUri, secondsSoFar, isPaused = false)
                    delay(1_000) // 1 sec ticking
                } else {
                    currentlyRecordingVoiceMutableFlow.value = OngoingVoiceRecording(recordingTargetUri, secondsSoFar, isPaused = true)
                }
            }
        }
    }

    fun onCancelVoiceMessageClicked() {
        closeVoiceMessageRecord()
    }

    private fun closeVoiceMessageRecord() {
        voiceRecordingDurationIncrementJob?.cancel()
        voiceRecordingDurationIncrementJob = null
        currentlyRecordingVoiceMutableFlow.value = null
    }

    fun onSendButtonClicked() {
        viewModelScope.launch(Dispatchers.Default) {
            val voiceMessage = currentlyRecordingVoiceMutableFlow.value
            val mediaMessages = mediasToSendMutableFlow.value
            val textMessage = currentMessageStateFlow.value

            if (voiceMessage != null) {
                closeVoiceMessageRecord()

                messagingClient.sendMessage(
                    MessageInput.Media.Audio(
                        FileSource.Local(
                            FileLocal.Audio(
                                uri = voiceMessage.targetUri,
                                fileName = "voice_message_${Clock.System.now()}",
                                mimeType = MimeType.Audio.MP3,
                                estimatedDurationMs = voiceMessage.secondsSoFar * 1_000L,
                            )
                        )
                    ),
                    conversationId,
                ).getOrNull()
            } else {

                mediasToSendMutableFlow.value = emptyList()

                mediaMessages.map { mediaToSend ->
                    async {
                        messagingClient.sendMessage(
                            input = when (mediaToSend) {
                                is LocalMedia.Image -> MessageInput.Media.Image(
                                    mediaSource = FileSource.Local(
                                        FileLocal.Image(
                                            uri = Uri(mediaToSend.uri.toString()),
                                            fileName = mediaToSend.name,
                                            mimeType = mediaToSend.mimeType,
                                        )
                                    )
                                )
                                is LocalMedia.Document -> MessageInput.Media.Document(
                                    mediaSource = FileSource.Local(
                                        FileLocal.Document(
                                            Uri(mediaToSend.uri.toString()),
                                            mediaToSend.name,
                                            mediaToSend.mimeType
                                        )
                                    )
                                )
                            },
                            conversationId = conversationId,
                        ).getOrNull()
                    }
                }.forEach { it.join() }

                if (textMessage.isNotBlank()) {
                    currentMessageStateFlow.value = ""

                    messagingClient.sendMessage(
                        input = MessageInput.Text(text = textMessage),
                        conversationId = conversationId,
                    ).getOrNull()
                }
            }

            scrollToBottomAfterNextUpdateMutableFlow.value = true
        }
    }

    fun onCurrentMessageChanged(currentMessage: String) {
        // 1. An event is always sent when the content becomes empty.
        // 2. An event is always sent when the content becomes non-empty.
        // 3. Otherwise, an event is sent at most once every 20 seconds.
        val state = stateFlow.value
        val lastTypingWasMoreThan20SecondsAgo = Clock.System.now() > lastTypingEventSentAt.plus(20.seconds)
        if (state is State.ConversationLoaded &&
            ((currentMessageStateFlow.value.isEmpty() xor currentMessage.isEmpty()) || lastTypingWasMoreThan20SecondsAgo)
        ) {
            lastTypingEventSentAt = Clock.System.now()
            viewModelScope.launch {
                messagingClient.setTyping(
                    isTyping = currentMessage.isNotEmpty(),
                    conversationId = conversationId,
                ).onFailure {
                    messagingClient.logger.error("failed to set patient as typing", it, tag = LOGGING_TAG)
                    // no UI alert on purpose
                }
            }
        }
        this.currentMessageStateFlow.value = currentMessage
    }

    fun onTimelineReachedTop() {
        val loadMore = latestLoadMoreCallback ?: return

        viewModelScope.launch(Dispatchers.Default) {
            loadMore()
                .onFailure {
                    messagingClient.logger.error("Error while loading more items in conversation", it, tag = LOGGING_TAG)
                    errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.LoadingMoreItems)
                }
        }
    }

    fun onItemClicked(clickedItem: TimelineItem) {
        val item = clickedItem as? TimelineItem.Message ?: return

        // Retry sending a message that previously failed
        if (item.status == SendStatus.ErrorSending && item.id is MessageId.Local) {
            viewModelScope.launch {
                messagingClient.retrySendingMessage(item.id, conversationId)
            }
            return
        }

        // Otherwise handle tap on message
        when (item.content) {
            is TimelineItem.Message.Text -> {
                selectedMessageIdFlow.value = (if (item.id == selectedMessageIdFlow.value) null else item.id)
            }
            is TimelineItem.Message.Image -> {
                navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenFullScreenImage(item.content.uri.toJvmUri()))
            }
            is TimelineItem.Message.File -> {
                val fileContent = item.content
                when (fileContent.mimeType) {
                    MimeType.Application.PDF -> navigationEventMutableFlow.emitIn(
                        viewModelScope, NavigationEvent.OpenFullScreenPdf(fileContent.uri.toJvmUri())
                    )
                    is MimeType.Image -> {
                        navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenFullScreenImage(fileContent.uri.toJvmUri()))
                    }
                    else -> navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenUriExternally(fileContent.uri.toJvmUri()))
                }
            }
            is TimelineItem.Message.Audio -> Unit
            is TimelineItem.Message.Deleted -> Unit // Nothing to do when taping on deleted message
        }
    }

    fun onRetryClicked() {
        retryAfterErrorTriggerFlow.emitIn(viewModelScope, Unit)
    }

    private fun Conversation.markConversationAsReadIfNeeded() {
        if (patientUnreadMessageCount > 0 && isViewForeground) {
            viewModelScope.launch {
                messagingClient.markConversationAsRead(conversationId)
                    .onFailure {
                        messagingClient.logger.error("Failed to mark conversation as read", it, tag = LOGGING_TAG)
                        // no UI alert on purpose
                    }
            }
        }
    }

    fun onDeleteMessage(item: TimelineItem.Message) {
        viewModelScope.launch(Dispatchers.Default) {
            messagingClient.deleteMessage(conversationId, item.id)
                .onFailure { error ->
                    messagingClient.logger.error("Failed to delete message", error, tag = LOGGING_TAG)
                    errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.DeletingMessage)
                }
        }
    }

    fun onUrlClicked(stringUrl: String) {
        runCatching { URI.create(stringUrl) }
            .onFailure {
                messagingClient.logger.error("Failed to parse clicked URL", it, tag = LOGGING_TAG)
                errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.ClickedUrlParsing)
            }
            .onSuccess { url ->
                navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenWebBrowser(url))
            }
    }

    fun onVoiceMessagePlaybackProgress(voiceMessageUri: Uri, position: Long, totalDuration: Long?) {
        voiceMessagesProgressMutableFlow.value = voiceMessagesProgressMutableFlow.value.toMutableMap()
            .apply { put(voiceMessageUri, PlaybackProgress(position, totalDuration)) }
    }

    fun onVoiceMessagePlaybackStarted(uri: Uri) {
        nowPlayingVoiceMessageMutableFlow.value = uri
    }

    fun onVoiceMessagePlaybackStopped(uri: Uri) {
        nowPlayingVoiceMessageMutableFlow.compareAndSet(uri, null)
    }

    fun onToggleVoiceMessagePlay(uri: Uri) {
        if (!nowPlayingVoiceMessageMutableFlow.compareAndSet(uri, null)) {
            nowPlayingVoiceMessageMutableFlow.value = uri
        }
    }

    fun onFailedToRecordVoiceMessage(exception: Exception) {
        messagingClient.logger.error("failed to start/stop voice message recording", exception)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.RecordingVoiceMessage)
        closeVoiceMessageRecord()
    }

    fun onVoiceMessageError(message: String) {
        messagingClient.logger.error("voice message recorder error - $message")
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.RecordingVoiceMessage)
        closeVoiceMessageRecord()
    }

    fun onLostAudioFocus() {
        currentlyRecordingVoiceMutableFlow.value = currentlyRecordingVoiceMutableFlow.value?.copy(isPaused = true)
    }

    sealed interface State {
        object Loading : State
        data class Error(val error: ErrorUiModel) : State

        data class ConversationLoaded(
            val conversation: Conversation,
            val items: List<TimelineItem>,
        ) : State
    }

    sealed interface EditorState {
        data class EditingText(
            val canSubmit: Boolean,
        ) : EditorState

        data class RecordingVoice(val recordProgressSeconds: Int) : EditorState
    }

    sealed class NavigationEvent {
        object OpenMediaSourcePicker : NavigationEvent()
        data class OpenFullScreenImage(val imageUri: URI) : NavigationEvent()
        data class OpenFullScreenPdf(val fileUri: URI) : NavigationEvent()
        data class OpenUriExternally(val uri: URI) : NavigationEvent()
        data class OpenWebBrowser(val url: URI) : NavigationEvent()
        object OpenCameraPictureCapture : NavigationEvent()
        data class OpenMediaLibrary(val mimeTypes: List<MimeType>) : NavigationEvent()
        object RequestVoiceMessagePermissions : NavigationEvent()
    }

    sealed class ErrorAlert(@StringRes val errorMessageRes: Int) {
        object LoadingMoreItems : ErrorAlert(R.string.nabla_error_message_conversation_loading_more)
        object AttachmentMediaPicker : ErrorAlert(R.string.nabla_error_message_conversation_attachment_media_picker)
        object AttachmentCameraCapturing : ErrorAlert(R.string.nabla_error_message_conversation_attachment_camera_capturing)
        object AttachmentCameraOpening : ErrorAlert(R.string.nabla_error_message_conversation_attachment_camera_opening)
        object AttachmentLibraryOpening : ErrorAlert(R.string.nabla_error_message_conversation_attachment_library_opening)
        object LinkOpening : ErrorAlert(R.string.nabla_error_message_conversation_link_opening)
        object DeletingMessage : ErrorAlert(R.string.nabla_error_message_conversation_deleting_message)
        object ClickedUrlParsing : ErrorAlert(R.string.nabla_error_message_conversation_clicked_url_parsing)
        object RecordingVoiceMessage : ErrorAlert(R.string.nabla_conversation_error_recording_voice_message)
    }

    private class StateMapper {
        private val timelineBuilder = TimelineBuilder()

        fun mapToState(
            conversation: Conversation,
            conversationItemsResponse: WatchPaginatedResponse<ConversationItems>,
            selectedMessageId: MessageId?,
            audioPlaybackProgressMap: Map<Uri, PlaybackProgress>,
            nowPlayingAudio: Uri?,
        ): State {
            return State.ConversationLoaded(
                conversation = conversation,
                items = timelineBuilder.buildTimeline(
                    items = conversationItemsResponse.content.items,
                    hasMore = conversationItemsResponse.loadMore != null,
                    providersInConversation = conversation.providersInConversation,
                    selectedMessageId = selectedMessageId,
                    audioPlaybackProgressMap = audioPlaybackProgressMap,
                    nowPlayingAudioUri = nowPlayingAudio,
                ),
            )
        }
    }

    companion object {
        private val LOGGING_TAG = Logger.asSdkTag("UI-Conversation")
        private const val VOICE_MESSAGE_MAX_LENGTH_SECONDS = 10 * 60 // 10 minutes
    }
}
