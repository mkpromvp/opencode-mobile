package com.opencode.mobile.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.mobile.api.OpenCodeApiService
import com.opencode.mobile.data.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConnectUiState(
    val serverUrl: String = "",
    val username: String = "opencode",
    val password: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null
)

class ConnectViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.serverUrl.collect { url ->
                _uiState.update { it.copy(serverUrl = url) }
            }
        }
        viewModelScope.launch {
            preferencesManager.username.collect { user ->
                _uiState.update { it.copy(username = user) }
            }
        }
        viewModelScope.launch {
            preferencesManager.password.collect { pass ->
                _uiState.update { it.copy(password = pass) }
            }
        }
    }

    fun updateServerUrl(url: String) { _uiState.update { it.copy(serverUrl = url, error = null) } }
    fun updateUsername(user: String) { _uiState.update { it.copy(username = user, error = null) } }
    fun updatePassword(pass: String) { _uiState.update { it.copy(password = pass, error = null) } }

    fun connect(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.serverUrl.isBlank()) {
            _uiState.update { it.copy(error = "Server URL is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val service = OpenCodeApiService(state.serverUrl, state.username, state.password)
                val result = service.health()
                result.fold(
                    onSuccess = { healthy ->
                        if (healthy) {
                            preferencesManager.saveConnection(
                                state.serverUrl, state.username, state.password
                            )
                            _uiState.update { it.copy(isLoading = false, isConnected = true) }
                            onSuccess()
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = "Server not healthy") }
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Connection failed") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Connection failed") }
            }
        }
    }
}
