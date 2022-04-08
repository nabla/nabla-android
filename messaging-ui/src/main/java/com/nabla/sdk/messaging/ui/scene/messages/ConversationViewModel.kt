package com.nabla.sdk.messaging.ui.scene.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.helper.toJvmUri
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.helpers.mediapicker.LocalMedia
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.BaseMessage
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.MessageStatus
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import kotlinx.coroutines.Dispatchers
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

@Suppress("UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER")
internal class ConversationViewModel(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val onErrorCallback: (message: String, Throwable) -> Unit,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val retryAfterErrorTriggerFlow = MutableSharedFlow<Unit>()
    private val selectedMessageIdFlow = MutableStateFlow<MessageId?>(null)

    val stateFlow: StateFlow<State>

    val editorStateFlow: StateFlow<EditorState>

    private val currentMessageStateFlow = MutableStateFlow("")
    val currentMessageFlow: Flow<String> = currentMessageStateFlow

    private val navigationEventMutableFlow = MutableLiveFlow<NavigationEvent>()
    val navigationEventFlow: LiveFlow<NavigationEvent> = navigationEventMutableFlow

    private val mediasToSendMutableFlow = MutableStateFlow<List<LocalMedia>>(emptyList())
    val mediasToSendFlow: Flow<List<LocalMedia>> = mediasToSendMutableFlow

    private val scrollToBottomAfterNextUpdateMutableFlow = MutableStateFlow(false)
    val shouldScrollToBottomAfterNextUpdate get() = scrollToBottomAfterNextUpdateMutableFlow.compareAndSet(expect = true, update = false)

    private var lastTypingEventSentAt: Instant = Instant.DISTANT_PAST
    private var isViewForeground = false

    private val conversationId: ConversationId = savedStateHandle.get<Uuid>("conversationId")?.toConversationId() ?: error("no conversationId")

    init {
        stateFlow = makeStateFlow(
            messageRepository
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

    private fun makeStateFlow(conversationDataFlow: Flow<ConversationWithMessages>): StateFlow<State> {
        return combine(
            conversationDataFlow,
            selectedMessageIdFlow,
            StateMapper()::mapToState,
        )
            .retryWhen { throwable, _ ->
                onErrorCallback("Failed to fetch conversation messages", throwable)
                emit(State.Error)

                retryAfterErrorTriggerFlow.first()
                emit(State.Loading)
                true
            }
            .stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, initialValue = State.Loading)
    }

    private fun Flow<ConversationWithMessages>.handleConversationDataSideEffects() =
        onEach { it.conversation.markConversationAsReadIfNeeded() }

    fun onViewStart() {
        isViewForeground = true

        // Mark conversation as read if a new message has been received while in background
        (stateFlow.value as? State.ConversationLoaded)?.conversation?.markConversationAsReadIfNeeded()
    }

    fun onViewStop() {
        isViewForeground = false
    }

    fun onParticipantsHeaderClicked() {
        // TODO
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
        // TODO
    }

    fun onErrorWithPictureCapture(error: Exception) {
        // TODO
    }

    fun onErrorLaunchingCameraForImageCapture(error: Throwable) {
        // TODO
    }

    fun onErrorLaunchingLibrary(error: Throwable) {
        // TODO
    }

    fun onErrorOpeningLink(error: Throwable) {
        // TODO
    }

    fun onSendButtonClicked() {

        viewModelScope.launch(Dispatchers.Default) {
            val mediaMessages = mediasToSendMutableFlow.value
            val textMessage = currentMessageStateFlow.value

            mediasToSendMutableFlow.value = emptyList()

            mediaMessages.forEach { mediaToSend ->
                // TODO delete LocalMessage
                // messageRepository.sendMessage(..)

                mediasToSendMutableFlow.value = emptyList()
            }
            if (textMessage.isNotBlank()) {
                currentMessageStateFlow.value = ""

                // TODO improve sending API to not specify irrelevant args like status, sender & sentAt
                messageRepository.sendMessage(
                    Message.Text(
                        message = BaseMessage(
                            id = MessageId.Local(uuid4()),
                            sentAt = Clock.System.now(),
                            sender = MessageSender.Patient,
                            status = MessageStatus.Sending,
                            conversationId = conversationId,
                        ),
                        text = textMessage,
                    )
                )
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
                runCatchingCancellable {
                    messageRepository.setTyping(
                        isTyping = currentMessage.isNotEmpty(),
                        conversationId = conversationId,
                    )
                }.onFailure {
                    // TODO handle errors
                    println("failed to set typing")
                }
            }
        }
        this.currentMessageStateFlow.value = currentMessage
    }

    fun onTimelineReachedTop() {
        viewModelScope.launch(Dispatchers.Default) {
            runCatchingCancellable {
                messageRepository.loadMoreMessages(conversationId)
            }.onFailure {
                onErrorCallback("Error while loading more items in conversation", it)
            }
        }
    }

    fun onItemClicked(clickedItem: TimelineItem) {
        val item = clickedItem as? TimelineItem.Message ?: return

        // Retry sending a message that previously failed
        if (item.status == MessageStatus.ErrorSending) {
            viewModelScope.launch {
                messageRepository.retrySendingMessage(conversationId, clickedItem.id as MessageId.Local)
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

    fun onProviderClicked(providerId: Uuid) {
        // TODO
    }

    fun onRetryClicked() {
        retryAfterErrorTriggerFlow.emitIn(viewModelScope, Unit)
    }

    private fun Conversation.markConversationAsReadIfNeeded() {
        if (patientUnreadMessageCount > 0 && isViewForeground) {
            conversationRepository.markConversationAsRead(conversationId)
        }
    }

    fun onDeleteMessage(item: TimelineItem.Message) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatchingCancellable {
                messageRepository.deleteMessage(conversationId, item.id)
            }.onFailure { error ->
                // TODO handle error
            }
        }
    }

    fun onUrlClicked(url: String, isFromPatient: Boolean) {
        navigationEventMutableFlow.emitIn(viewModelScope, NavigationEvent.OpenWebBrowser(url))
    }

    sealed interface State {
        object Loading : State
        object Error : State

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
        data class OpenWebBrowser(val url: String) : NavigationEvent()
        object OpenCameraPictureCapture : NavigationEvent()
        data class OpenMediaLibrary(val mimeTypes: List<MimeType>) : NavigationEvent()
    }

    private class StateMapper {
        private val timelineBuilder = TimelineBuilder()

        fun mapToState(
            conversationWithMessages: ConversationWithMessages,
            selectedMessageId: MessageId?,
        ): State {
            return State.ConversationLoaded(
                conversation = conversationWithMessages.conversation,
                items = timelineBuilder.buildTimeline(
                    paginatedMessages = conversationWithMessages.messages,
                    providersInConversation = conversationWithMessages.conversation.providersInConversation,
                    selectedMessageId = selectedMessageId,
                ),
            )
        }
    }
}
