package com.opencode.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val tag: String? = null,
    val message: String? = null
)
