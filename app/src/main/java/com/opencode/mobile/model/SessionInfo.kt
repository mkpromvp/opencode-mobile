package com.opencode.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionInfo(
    val id: String,
    val agent: String? = null,
    val model: String? = null,
    val location: Location? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

@Serializable
data class Location(
    val directory: String? = null,
    val workspaceID: String? = null,
    val project: String? = null
)

@Serializable
data class SessionListResponse(
    val data: List<SessionInfo>,
    val cursor: Cursor? = null,
    val location: Location? = null
)

@Serializable
data class SessionDetailResponse(
    val data: SessionInfo,
    val location: Location? = null
)

@Serializable
data class Cursor(
    val previous: String? = null,
    val next: String? = null
)
