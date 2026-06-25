package com.opencode.mobile.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.mobile.api.OpenCodeApiService
import com.opencode.mobile.api.dto.CreateSessionRequest
import com.opencode.mobile.data.PreferencesManager
import com.opencode.mobile.model.AgentInfo
import com.opencode.mobile.model.SessionInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SessionsUiState(
    val sessions: List<SessionInfo> = emptyList(),
    val agents: List<AgentInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null
)

class SessionsViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    private var apiService: OpenCodeApiService? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val url = preferencesManager.serverUrl.first()
                val user = preferencesManager.username.first()
                val pass = preferencesManager.password.first()
                if (url.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, error = "Not connected") }
                    return@launch
                }

                apiService = OpenCodeApiService(url, user, pass)
                val service = apiService!!

                val sessionsResult = service.listSessions()
                val agentsResult = service.listAgents()

                sessionsResult.fold(
                    onSuccess = { resp ->
                        _uiState.update { it.copy(sessions = resp.data) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message ?: "Failed to load sessions") }
                    }
                )

                agentsResult.fold(
                    onSuccess = { resp ->
                        _uiState.update { it.copy(agents = resp.data) }
                    },
                    onFailure = { /* ignore */ }
                )

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load data") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun createSession(agent: String? = null, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            try {
                val service = apiService ?: return@launch
                val request = CreateSessionRequest(agent = agent)
                val result = service.createSession(request)
                result.fold(
                    onSuccess = { resp ->
                        preferencesManager.saveLastSessionId(resp.data.id)
                        onCreated(resp.data.id)
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = e.message ?: "Failed to create session") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to create session") }
            } finally {
                _uiState.update { it.copy(isCreating = false) }
            }
        }
    }
}
