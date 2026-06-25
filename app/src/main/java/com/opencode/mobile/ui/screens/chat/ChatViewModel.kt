package com.opencode.mobile.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.mobile.api.OpenCodeApiService
import com.opencode.mobile.api.await
import com.opencode.mobile.api.dto.PermissionReply
import com.opencode.mobile.api.dto.PromptRequest
import com.opencode.mobile.api.dto.QuestionReply
import com.opencode.mobile.data.PreferencesManager
import com.opencode.mobile.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentStreamingText: String = "",
    val isStreaming: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingQuestion: QuestionData? = null,
    val pendingPermission: PermissionData? = null
)

data class ChatMessage(
    val id: String = "",
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val toolCalls: List<ToolCallInfo> = emptyList()
)

data class ToolCallInfo(
    val id: String,
    val name: String,
    val input: String = "",
    val result: String = "",
    val isError: Boolean = false
)

class ChatViewModel(
    private val sessionId: String,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var apiService: OpenCodeApiService? = null
    private var serverBaseUrl: String = ""
    private var sseJob: Job? = null

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, error = null) }
            try {
                val url = preferencesManager.serverUrl.first()
                val user = preferencesManager.username.first()
                val pass = preferencesManager.password.first()
                if (url.isBlank()) {
                    _uiState.update { it.copy(isLoadingHistory = false, error = "Not connected") }
                    return@launch
                }

                serverBaseUrl = url
                apiService = OpenCodeApiService(url, user, pass)
                val service = apiService!!

                val messagesResult = service.listMessages(sessionId)
                messagesResult.fold(
                    onSuccess = { resp ->
                        val chatMessages = resp.data.map { msg ->
                            ChatMessage(
                                id = msg.id,
                                role = msg.role,
                                content = msg.content?.joinToString("\n") { block ->
                                    when (block.type) {
                                        "text" -> block.text ?: ""
                                        "tool_call" -> "🔧 Tool: ${block.toolCall?.name ?: "unknown"}\n${block.toolCall?.input ?: ""}"
                                        "tool_result" -> "📦 Result: ${block.toolResult?.result ?: ""}"
                                        else -> ""
                                    }
                                } ?: "",
                                toolCalls = msg.content?.filter { it.type == "tool_call" }?.map {
                                    ToolCallInfo(
                                        id = it.toolCall?.id ?: "",
                                        name = it.toolCall?.name ?: "unknown",
                                        input = it.toolCall?.input ?: ""
                                    )
                                } ?: emptyList()
                            )
                        }
                        _uiState.update { it.copy(messages = chatMessages) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message ?: "Failed to load messages") }
                    }
                )

                startSSEStream(service)

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load session") }
            } finally {
                _uiState.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    private fun startSSEStream(service: OpenCodeApiService) {
        sseJob?.cancel()
        sseJob = viewModelScope.launch {
            try {
                service.newSSEClient().connect().collect { event ->
                    handleEvent(event)
                }
            } catch (e: Exception) {
                // SSE connection closed
            }
        }
    }

    private fun handleEvent(event: ServerEvent) {
        when (event.type) {
            "server.connected" -> {
                // Connection established
            }
            "text.chunk" -> {
                val text = event.data?.content ?: return
                _uiState.update { it.copy(currentStreamingText = it.currentStreamingText + text, isStreaming = true) }
            }
            "text.done" -> {
                val text = _uiState.value.currentStreamingText
                if (text.isNotBlank()) {
                    val msg = ChatMessage(
                        id = event.id ?: "msg_${System.currentTimeMillis()}",
                        role = "assistant",
                        content = text
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + msg,
                            currentStreamingText = "",
                            isStreaming = false
                        )
                    }
                }
            }
            "tool.call" -> {
                val toolCall = event.data ?: return
                val toolMsg = "🔧 **${toolCall.toolName ?: "Tool"}**\n${toolCall.toolInput ?: ""}"
                _uiState.update {
                    it.copy(
                        currentStreamingText = if (it.currentStreamingText.isNotBlank()) it.currentStreamingText else "",
                        isStreaming = false
                    )
                }
            }
            "tool.result" -> {
                val data = event.data ?: return
                val resultMsg = ChatMessage(
                    id = event.id ?: "tr_${System.currentTimeMillis()}",
                    role = "tool",
                    content = "📦 **${data.toolName ?: "Tool"} Result**\n${data.toolResult ?: ""}"
                )
                _uiState.update { it.copy(messages = it.messages + resultMsg) }
            }
            "question.request" -> {
                val question = event.data?.question
                if (question != null) {
                    _uiState.update { it.copy(pendingQuestion = question) }
                }
            }
            "permission.request" -> {
                val permission = event.data?.permission
                if (permission != null) {
                    _uiState.update { it.copy(pendingPermission = permission) }
                }
            }
            "session.updated" -> {
                // Session state updated
            }
        }
    }

    fun sendPrompt(prompt: String) {
        val service = apiService ?: return
        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            role = "user",
            content = prompt
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                error = null,
                isStreaming = true,
                currentStreamingText = ""
            )
        }

        viewModelScope.launch {
            val result = service.sendPrompt(sessionId, PromptRequest(prompt = prompt))
            result.fold(
                onSuccess = { /* streaming will update via SSE */ },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isStreaming = false) }
                }
            )
        }
    }

    fun answerQuestion(answers: List<String>) {
        val service = apiService ?: return
        val questionId = _uiState.value.pendingQuestion?.id ?: return

        viewModelScope.launch {
            try {
                val request = QuestionReply(answers)
                val json = kotlinx.serialization.json.Json { encodeDefaults = true }
                val body = json.encodeToString(QuestionReply.serializer(), request)
                val req = okhttp3.Request.Builder()
                    .url("${serverBaseUrl.trimEnd('/')}/api/session/$sessionId/question/$questionId/reply")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                service.getOkHttpClient().newCall(req).await()
                _uiState.update { it.copy(pendingQuestion = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun rejectQuestion() {
        val service = apiService ?: return
        val questionId = _uiState.value.pendingQuestion?.id ?: return

        viewModelScope.launch {
            try {
                val req = okhttp3.Request.Builder()
                    .url("${serverBaseUrl.trimEnd('/')}/api/session/$sessionId/question/$questionId/reject")
                    .post("".toRequestBody("application/json".toMediaType()))
                    .build()
                service.getOkHttpClient().newCall(req).await()
                _uiState.update { it.copy(pendingQuestion = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun answerPermission(reply: String, message: String? = null) {
        val service = apiService ?: return
        val permissionId = _uiState.value.pendingPermission?.id ?: return

        viewModelScope.launch {
            try {
                val request = PermissionReply(reply, message)
                val json = kotlinx.serialization.json.Json { encodeDefaults = true }
                val body = json.encodeToString(PermissionReply.serializer(), request)
                val req = okhttp3.Request.Builder()
                    .url("${serverBaseUrl.trimEnd('/')}/api/session/$sessionId/permission/$permissionId/reply")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                service.getOkHttpClient().newCall(req).await()
                _uiState.update { it.copy(pendingPermission = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseJob?.cancel()
    }
}
