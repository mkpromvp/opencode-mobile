package com.opencode.mobile.api

import com.opencode.mobile.model.ServerEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

class SSEClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(BasicAuthInterceptor(username, password))
        .build()

    fun connect(): Flow<ServerEvent> = callbackFlow {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/event")
            .header("Accept", "text/event-stream")
            .build()

        val factory = EventSources.createFactory(client)

        var eventSource: EventSource? = null

        val listener = object : okhttp3.sse.EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val event = json.decodeFromString<ServerEvent>(data)
                    trySend(event)
                } catch (e: Exception) {
                    // skip malformed events
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: Exception("SSE connection failed"))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        eventSource = factory.newEventSource(request, listener)

        awaitClose {
            eventSource?.cancel()
        }
    }
}
