package com.nabla.sdk.messaging.core

import androidx.annotation.CheckResult
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.MessagingModule
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.PaginatedContent
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItem
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.coroutines.flow.Flow

/**
 * Main entry-point for SDK messaging features.
 *
 * Mandatory: before any interaction with messaging features make sure you
 * successfully authenticated your user by calling [NablaClient.authenticate].
 *
 * We recommend you reuse the same instance for all interactions,
 * check documentation of [initialize] and [getInstance].
 */
public interface NablaMessagingClient : MessagingModule {

    /**
     * Exposed for internal usage by Nabla Messaging UI.
     * You're not expected to use it.
     */
    @NablaInternal
    public val logger: Logger

    /**
     * Watch the list of conversations the current user is involved in.
     *
     * @see PaginatedContent for pagination mechanism.
     * @see Response for cache and offline data management.
     *
     * Returned flow might throw any of [NablaException] children.
     */
    public fun watchConversations(): Flow<Response<PaginatedContent<List<Conversation>>>>

    /**
     * Create a new conversation on behalf of the current user. This conversation will be created server-side
     * and visible from the console right after calling this method.
     *
     * Reference the returned [Conversation.id] for further actions on that freshly created conversation: send message, mark as read, etc.
     *
     * @param message - initial message to be sent to the conversation on behalf of the current user.
     * @param title optional - title for the conversation
     * @param providerIds optional - List of [com.nabla.sdk.core.domain.entity.Provider.id], providers that will
     *                    participate to the conversation.
     *                    Make sure the specified providers have enough rights to participate to a conversation.
     *                    See [Roles and Permissions](https://docs.nabla.com/docs/roles-and-permissions).
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    public suspend fun createConversationWithMessage(
        message: MessageInput,
        title: String? = null,
        providerIds: List<Uuid>? = null,
    ): Result<Conversation>

    /**
     * Start a new conversation. This conversation will not be created server-side nor visible
     * from the console until a first message is sent in it by the Patient.
     *
     * Check [createConversationWithMessage] for more details.
     *
     * @return The id to use to reference the conversation in other endpoints, notably in [sendMessage].
     */
    @CheckResult
    public fun startConversation(
        title: String? = null,
        providerIds: List<Uuid>? = null,
    ): ConversationId.Local

    /**
     * Watch a conversation details.
     *
     * This flow will be called with a new [Conversation] every time something changes
     * in that specific conversation state.
     *
     * You probably want to use that method to watch for things like the [Conversation.title]
     * changes or [Conversation.providersInConversation] changes.
     *
     * Returned flow might throw any of [NablaException] children.
     *
     * @see Response for cache and offline data management.
     *
     * @param conversationId the id of the conversation you want to watch update for.
     */
    public fun watchConversation(conversationId: ConversationId): Flow<Response<Conversation>>

    /**
     * Watch the list of [ConversationItem] in a conversation.
     * The current user should be involved in that conversation or a security error will be raised.
     *
     * @see PaginatedContent for pagination mechanism.
     * @see Response for cache and offline data management.
     *
     * Returned flow might throw any of [NablaException] children.
     *
     * @param conversationId the id from [Conversation.id].
     */
    public fun watchConversationItems(conversationId: ConversationId): Flow<Response<PaginatedContent<List<ConversationItem>>>>

    /**
     * Send a new message in the conversation.
     *
     * This will immediately append the message to the list of messages in the conversation
     * while making the necessary network query (optimistic behavior).
     *
     * A successful sending will result in the message's [Message.sendStatus] changing to [SendStatus.Sent]
     * and [Message.id] changing to [MessageId.Remote]. While failures will keep a [MessageId.Local] id
     * and change status to [SendStatus.ErrorSending].
     *
     * @param input input of the message to send, check `MessageInput.**` to create new messages.
     * @param conversationId the id of the conversation to send the message to.
     * @param replyTo the id of the message to reply to, or null if not a reply.
     *                Please note that the replied-to message should be already successfully sent,
     *                thus have a [MessageId.Remote] id.
     *
     * @see MessageInput.Text
     * @see MessageInput.Media.Image
     * @see MessageInput.Media.Document
     * @see MessageInput.Media.Audio
     *
     * @return [Result] of the operation containing the id of the message created, eventual errors will be of type [NablaException].
     */
    @CheckResult
    public suspend fun sendMessage(
        input: MessageInput,
        conversationId: ConversationId,
        replyTo: MessageId.Remote? = null,
    ): Result<MessageId.Local>

    /**
     * Retry sending a message for which [Message.sendStatus] is [SendStatus.ErrorSending].
     *
     * @param localMessageId the [Message.id] which is guaranteed to be [MessageId.Local] if status is [SendStatus.ErrorSending].
     * @param conversationId concerned conversation, find it in [Message.conversationId].
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    public suspend fun retrySendingMessage(localMessageId: MessageId.Local, conversationId: ConversationId): Result<Unit>

    /**
     * Change the current user typing status in the conversation.
     *
     * IMPORTANT: This is an ephemeral operation, if you want to keep your user marked as actively typing
     *            you should call this with isTyping=true at least once every 20 seconds.
     *
     * Call with isTyping=false to immediately mark the user as not typing anymore.
     * Typical use case is when the user deletes their draft.
     *
     * Please note that a successful call to [sendMessage] is enough to set typing to false,
     * so calling both will simply be a needless redundancy.
     *
     * As this will always result in a network call, please avoid overuse. For instance, you don't want
     * to call this each time the user types a new char, add a debounce instead.
     *
     * @param conversationId id of the conversation where user is/isn't actively typing.
     * @param isTyping whether user is actively typing or not.
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    public suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean): Result<Unit>

    /**
     * Acknowledge that the current user has seen all messages in it.
     * Will result in all messages sent before current timestamp to be marked as read.
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    public suspend fun markConversationAsRead(conversationId: ConversationId): Result<Unit>

    /**
     * Delete a message in the conversation. Current user should be its author.
     *
     * This will change the message type to [Message.Deleted].
     *
     * While this works for both messages that were successfully sent and those that failed sending,
     * calling [deleteMessage] on a message being currently in status [SendStatus.Sending] is very likely
     * to have noop or unexpected behavior.
     *
     * If you want to delete a message being currently in sending status,
     * please cancel the suspendable call to [sendMessage].
     *
     * @param conversationId concerned conversation, find it in [Message.conversationId].
     * @param id id of message to be deleted.
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    public suspend fun deleteMessage(conversationId: ConversationId, id: MessageId): Result<Unit>
}
