package com.nabla.sdk.messaging.ui.scene.messages

import androidx.annotation.CheckResult
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
import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.FileLocal
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.messaging.ui.scene.messages.ConversationFragment.Builder.Companion.conversationIdFromSavedStateHandleOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.net.URI
import kotlin.time.Duration.Companion.seconds

internal class ConversationViewModel(
    private val nablaMessaging: NablaMessaging,
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

    private var lastTypingEventSentAt: Instant = Instant.DISTANT_PAST
    private var isViewForeground = false

    private val conversationId: ConversationId = conversationIdFromSavedStateHandleOrThrow(savedStateHandle)

    init {
        stateFlow = makeStateFlow(
            nablaMessaging
                .watchConversationMessages(conversationId)
                .handleConversationDataSideEffects()
        )

        editorStateFlow = combine(
            currentMessageStateFlow,
            mediasToSendMutableFlow,
        ) { currentMessage, mediasToSend ->
            EditorState(
                canSubmit = currentMessage.isNotBlank() || mediasToSend.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = EditorState(canSubmit = false))
    }

    private fun makeStateFlow(conversationDataFlow: Flow<WatchPaginatedResponse<ConversationWithMessages>>): StateFlow<State> {
        return combine(
            conversationDataFlow,
            selectedMessageIdFlow,
            StateMapper()::mapToState,
        )
            .retryWhen { throwable, _ ->
                nablaMessaging.logger.error("Failed to fetch conversation messages", throwable, tag = LOGGING_TAG)
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

    private fun Flow<WatchPaginatedResponse<ConversationWithMessages>>.handleConversationDataSideEffects() =
        onEach { response ->
            response.items.conversation.markConversationAsReadIfNeeded()
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
        nablaMessaging.logger.error("Error in new media attachment picker", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.AttachmentMediaPicker(error))
    }

    fun onErrorWithPictureCapture(error: Exception) {
        nablaMessaging.logger.error("Error in camera for new attachment", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.AttachmentCameraCapturing(error))
    }

    fun onErrorLaunchingCameraForImageCapture(error: Throwable) {
        nablaMessaging.logger.error("Failed open camera for new attachment", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.AttachmentCameraOpening(error))
    }

    fun onErrorLaunchingLibrary(error: Throwable) {
        nablaMessaging.logger.error("Failed open media gallery for new attachment", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.AttachmentLibraryOpening(error))
    }

    fun onErrorOpeningLink(error: Throwable) {
        nablaMessaging.logger.error("Failed to open link", error, tag = LOGGING_TAG)
        errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.LinkOpening(error))
    }

    fun onSendButtonClicked() {
        viewModelScope.launch(Dispatchers.Default) {
            val mediaMessages = mediasToSendMutableFlow.value
            val textMessage = currentMessageStateFlow.value

            mediasToSendMutableFlow.value = emptyList()

            mediaMessages.map { mediaToSend ->
                async {
                    nablaMessaging.sendMessage(
                        when (mediaToSend) {
                            is LocalMedia.Image -> Message.Media.Image.new(
                                conversationId = conversationId,
                                mediaSource = FileSource.Local(FileLocal.Image(Uri(mediaToSend.uri.toString())))
                            )
                            is LocalMedia.Document -> Message.Media.Document.new(
                                conversationId = conversationId,
                                mediaSource = FileSource.Local(
                                    FileLocal.Document(
                                        Uri(mediaToSend.uri.toString()),
                                        mediaToSend.name,
                                        mediaToSend.mimeType
                                    )
                                )
                            )
                        }
                    ).getOrNull()
                }
            }.forEach { it.join() }

            if (textMessage.isNotBlank()) {
                currentMessageStateFlow.value = ""

                nablaMessaging.sendMessage(
                    Message.Text.new(
                        conversationId = conversationId,
                        text = textMessage,
                    )
                ).getOrNull()
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
                nablaMessaging.setTyping(
                    isTyping = currentMessage.isNotEmpty(),
                    conversationId = conversationId,
                ).onFailure {
                    nablaMessaging.logger.error("failed to set patient as typing", it, tag = LOGGING_TAG)
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
                    nablaMessaging.logger.error("Error while loading more items in conversation", it, tag = LOGGING_TAG)
                    errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.LoadingMoreItems(it))
                }
        }
    }

    fun onItemClicked(clickedItem: TimelineItem) {
        val item = clickedItem as? TimelineItem.Message ?: return

        // Retry sending a message that previously failed
        if (item.status == SendStatus.ErrorSending && item.id is MessageId.Local) {
            viewModelScope.launch {
                nablaMessaging.retrySendingMessage(item.id, conversationId)
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
            is TimelineItem.Message.Deleted -> Unit // Nothing to do when taping on deleted message
        }
    }

    fun onRetryClicked() {
        retryAfterErrorTriggerFlow.emitIn(viewModelScope, Unit)
    }

    private fun Conversation.markConversationAsReadIfNeeded() {
        if (patientUnreadMessageCount > 0 && isViewForeground) {
            viewModelScope.launch {
                nablaMessaging.markConversationAsRead(conversationId)
                    .onFailure {
                        nablaMessaging.logger.error("Failed to mark conversation as read", it, tag = LOGGING_TAG)
                        // no UI alert on purpose
                    }
            }
        }
    }

    fun onDeleteMessage(item: TimelineItem.Message) {
        viewModelScope.launch(Dispatchers.Default) {
            nablaMessaging.deleteMessage(conversationId, item.id)
                .onFailure { error ->
                    nablaMessaging.logger.error("Failed to delete message", error, tag = LOGGING_TAG)
                    errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.DeletingMessage(error))
                }
        }
    }

    fun onUrlClicked(stringUrl: String) {
        runCatching { URI.create(stringUrl) }
            .onFailure {
                nablaMessaging.logger.error("Failed to parse clicked URL", it, tag = LOGGING_TAG)
                errorAlertMutableFlow.emitIn(viewModelScope, ErrorAlert.ClickedUrlParsing(it))
            }
            .onSuccess { url ->
                navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenWebBrowser(url))
            }
    }

    sealed interface State {
        object Loading : State
        data class Error(val error: ErrorUiModel) : State

        data class ConversationLoaded(
            val conversation: Conversation,
            val items: List<TimelineItem>,
        ) : State
    }

    data class EditorState(
        val canSubmit: Boolean,
    )

    sealed class NavigationEvent {
        object OpenMediaSourcePicker : NavigationEvent()
        data class OpenFullScreenImage(val imageUri: URI) : NavigationEvent()
        data class OpenFullScreenPdf(val fileUri: URI) : NavigationEvent()
        data class OpenUriExternally(val uri: URI) : NavigationEvent()
        data class OpenWebBrowser(val url: URI) : NavigationEvent()
        object OpenCameraPictureCapture : NavigationEvent()
        data class OpenMediaLibrary(val mimeTypes: List<MimeType>) : NavigationEvent()
    }

    sealed interface ErrorAlert {
        data class LoadingMoreItems(val throwable: Throwable) : ErrorAlert
        data class AttachmentMediaPicker(val throwable: Throwable) : ErrorAlert
        data class AttachmentCameraCapturing(val throwable: Throwable) : ErrorAlert
        data class AttachmentCameraOpening(val throwable: Throwable) : ErrorAlert
        data class AttachmentLibraryOpening(val throwable: Throwable) : ErrorAlert
        data class LinkOpening(val throwable: Throwable) : ErrorAlert
        data class DeletingMessage(val throwable: Throwable) : ErrorAlert
        data class ClickedUrlParsing(val throwable: Throwable) : ErrorAlert
    }

    private class StateMapper {
        private val timelineBuilder = TimelineBuilder()

        fun mapToState(
            conversationWithMessagesResponse: WatchPaginatedResponse<ConversationWithMessages>,
            selectedMessageId: MessageId?,
        ): State {
            return State.ConversationLoaded(
                conversation = conversationWithMessagesResponse.items.conversation,
                items = timelineBuilder.buildTimeline(
                    messages = conversationWithMessagesResponse.items.messages,
                    hasMore = conversationWithMessagesResponse.loadMore != null,
                    providersInConversation = conversationWithMessagesResponse.items.conversation.providersInConversation,
                    selectedMessageId = selectedMessageId,
                ),
            )
        }
    }

    companion object {
        private val LOGGING_TAG = Logger.tag("UI-Conversation")
    }
}
