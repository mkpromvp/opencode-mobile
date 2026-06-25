package com.opencode.mobile.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    val id: String? = null,
    val agent: String? = null,
    val model: String? = null,
    val location: LocationDto? = null
)

@Serializable
data class LocationDto(
    val directory: String? = null,
    val workspaceID: String? = null
)

@Serializable
data class PromptRequest(
    val prompt: String,
    val id: String? = null,
    val delivery: String? = null,
    val resume: Boolean? = null
)

@Serializable
data class SwitchAgentRequest(
    val agent: String
)

@Serializable
data class SwitchModelRequest(
    val model: String
)

@Serializable
data class PermissionReply(
    val reply: String,
    val message: String? = null
)

@Serializable
data class QuestionReply(
    val answers: List<String>
)
