package com.opencode.mobile.api

import com.opencode.mobile.api.dto.*
import com.opencode.mobile.model.*
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenCodeApiService(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaType = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(BasicAuthInterceptor(username, password))
        .build()

    private fun buildUrl(path: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase$path"
    }

    private fun Request.Builder.withLocation(location: Location?): Request.Builder {
        if (location?.directory != null) {
            addHeader("x-opencode-directory", location.directory)
        }
        if (location?.workspaceID != null) {
            addHeader("x-opencode-workspace", location.workspaceID)
        }
        return this
    }

    suspend fun health(): Result<Boolean> = runCatching {
        val request = Request.Builder().url(buildUrl("/api/health")).get().build()
        val response = client.newCall(request).await()
        response.isSuccessful
    }

    suspend fun listSessions(cursor: String? = null, limit: Int = 50): Result<SessionListResponse> = runCatching {
        val url = buildUrl("/api/session").let {
            val params = mutableListOf<String>()
            cursor?.let { params.add("cursor=$it") }
            params.add("limit=$limit")
            if (params.isNotEmpty()) "$it?${params.joinToString("&")}" else it
        }
        val request = Request.Builder().url(url).get().build()
        val body = client.newCall(request).await().body?.string() ?: throw IOException("Empty body")
        json.decodeFromString<SessionListResponse>(body)
    }

    suspend fun createSession(request: CreateSessionRequest): Result<SessionDetailResponse> = runCatching {
        val body = json.encodeToString(CreateSessionRequest.serializer(), request)
        val req = Request.Builder()
            .url(buildUrl("/api/session"))
            .post(body.toRequestBody(mediaType))
            .build()
        val responseBody = client.newCall(req).await().body?.string() ?: throw IOException("Empty body")
        json.decodeFromString<SessionDetailResponse>(responseBody)
    }

    suspend fun getSession(sessionId: String): Result<SessionDetailResponse> = runCatching {
        val request = Request.Builder()
            .url(buildUrl("/api/session/$sessionId"))
            .get()
            .build()
        val body = client.newCall(request).await().body?.string() ?: throw IOException("Empty body")
        json.decodeFromString<SessionDetailResponse>(body)
    }

    suspend fun sendPrompt(sessionId: String, request: PromptRequest): Result<SessionDetailResponse> = runCatching {
        val body = json.encodeToString(PromptRequest.serializer(), request)
        val req = Request.Builder()
            .url(buildUrl("/api/session/$sessionId/prompt"))
            .post(body.toRequestBody(mediaType))
            .build()
        val responseBody = client.newCall(req).await().body?.string() ?: throw IOException("Empty body")
        json.decodeFromString<SessionDetailResponse>(responseBody)
    }

    suspend fun listMessages(sessionId: String, cursor: String? = null, limit: Int = 50): Result<MessageListResponse> = runCatching {
        val url = buildUrl("/api/session/$sessionId/message").let {
            val params = mutableListOf<String>()
            cursor?.let { params.add("cursor=$it") }
            params.add("limit=$limit")
            if (params.isNotEmpty()) "$it?${params.joinToString("&")}" else it
        }
        val request = Request.Builder().url(url).get().build()
        val body = client.newCall(request).await().body?.string() ?: throw IOException("Empty body")
        json.decodeFromString<MessageListResponse>(body)
    }

    suspend fun listAgents(): Result<AgentListResponse> = runCatching {
        val request = Request.Builder().url(buildUrl("/api/agent")).get().build()
        val body = client.newCall(request).await().body?.string() ?: throw IOException("Empty body")
        json.decodeFromString<AgentListResponse>(body)
    }

    suspend fun switchAgent(sessionId: String, agent: String): Result<Unit> = runCatching {
        val body = json.encodeToString(SwitchAgentRequest.serializer(), SwitchAgentRequest(agent))
        val req = Request.Builder()
            .url(buildUrl("/api/session/$sessionId/agent"))
            .post(body.toRequestBody(mediaType))
            .build()
        client.newCall(req).await()
        Unit
    }

    suspend fun waitForIdle(sessionId: String): Result<Unit> = runCatching {
        val request = Request.Builder()
            .url(buildUrl("/api/session/$sessionId/wait"))
            .post("".toRequestBody(mediaType))
            .build()
        client.newCall(request).await()
        Unit
    }

    suspend fun getSessionContext(sessionId: String): Result<String> = runCatching {
        val request = Request.Builder()
            .url(buildUrl("/api/session/$sessionId/context"))
            .get()
            .build()
        client.newCall(request).await().body?.string() ?: throw IOException("Empty body")
    }

    suspend fun compactSession(sessionId: String): Result<Unit> = runCatching {
        val request = Request.Builder()
            .url(buildUrl("/api/session/$sessionId/compact"))
            .post("".toRequestBody(mediaType))
            .build()
        client.newCall(request).await()
        Unit
    }

    fun newSSEClient(): SSEClient {
        return SSEClient(baseUrl, username, password)
    }

    fun getOkHttpClient(): OkHttpClient = client
}

class BasicAuthInterceptor(private val username: String, private val password: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = Credentials.basic(username, password)
        val request = chain.request().newBuilder()
            .header("Authorization", credentials)
            .build()
        return chain.proceed(request)
    }
}

suspend fun Call.await(): Response {
    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) {}
            }
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWith(Result.failure(e))
            }
        })
        continuation.invokeOnCancellation {
            try { cancel() } catch (_: Exception) {}
        }
    }
}
