package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemOtherMessageContentBinding
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemPatientMessageContentBinding
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageContentBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder

internal fun <ContentType : TimelineItem.Message.Content, BinderType : MessageContentBinder<ContentType>> inflateProviderMessageContentCard(
    inflater: LayoutInflater,
    parent: ViewGroup,
    buildContentBinder: (contentParent: ViewGroup) -> BinderType,
): BinderType {
    val contentContainerBinding = NablaConversationTimelineItemProviderMessageContentBinding.inflate(inflater, parent, true)
    return buildContentBinder(contentContainerBinding.chatProviderMessageContent)
}

internal fun <ContentType : TimelineItem.Message.Content, BinderType : MessageContentBinder<ContentType>> inflatePatientMessageContentCard(
    inflater: LayoutInflater,
    parent: ViewGroup,
    buildContentBinder: (contentParent: ViewGroup) -> BinderType,
): BinderType {
    val contentContainerBinding = NablaConversationTimelineItemPatientMessageContentBinding.inflate(inflater, parent, true)
    return buildContentBinder(contentContainerBinding.chatPatientMessageContent)
}

internal fun <ContentType : TimelineItem.Message.Content, BinderType : MessageContentBinder<ContentType>> inflateOtherMessageContentCard(
    inflater: LayoutInflater,
    parent: ViewGroup,
    buildContentBinder: (contentParent: ViewGroup) -> BinderType,
): BinderType {
    val contentContainerBinding = NablaConversationTimelineItemOtherMessageContentBinding.inflate(inflater, parent, true)
    return buildContentBinder(contentContainerBinding.chatOtherMessageContent)
}
