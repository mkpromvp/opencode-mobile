package com.opencode.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerEvent(
    val id: String? = null,
    val type: String,
    val data: EventData? = null,
    val metadata: EventMetadata? = null,
    val location: Location? = null
)

@Serializable
data class EventData(
    val content: String? = null,
    val role: String? = null,
    val sessionID: String? = null,
    val agent: String? = null,
    val toolCallID: String? = null,
    val toolName: String? = null,
    val toolInput: String? = null,
    val toolResult: String? = null,
    val isError: Boolean? = null,
    val message: String? = null,
    val status: String? = null,
    val answers: List<String>? = null,
    val permission: PermissionData? = null,
    val question: QuestionData? = null
)

@Serializable
data class EventMetadata(
    val timestamp: Long? = null
)

@Serializable
data class PermissionData(
    val id: String? = null,
    val tool: String? = null,
    val args: String? = null
)

@Serializable
data class QuestionData(
    val id: String? = null,
    val question: String? = null,
    val options: List<String>? = null
)
