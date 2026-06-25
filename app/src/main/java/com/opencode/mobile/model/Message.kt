package com.opencode.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val role: String,
    val content: List<ContentBlock>? = null,
    val createdAt: Long? = null
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null,
    val toolCall: ToolCall? = null,
    val toolResult: ToolResult? = null
)

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val input: String? = null
)

@Serializable
data class ToolResult(
    val id: String,
    val name: String,
    val result: String? = null,
    val isError: Boolean? = null
)

@Serializable
data class MessageListResponse(
    val data: List<Message>,
    val cursor: Cursor? = null,
    val location: Location? = null
)
