package com.opencode.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentInfo(
    val id: String,
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class AgentListResponse(
    val data: List<AgentInfo>,
    val location: Location? = null
)
