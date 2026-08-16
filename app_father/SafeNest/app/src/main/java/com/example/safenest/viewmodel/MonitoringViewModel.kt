package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.AllowedAppItem
import com.example.safenest.network.InstalledAppItem
import com.example.safenest.network.DigitalRuleResponse
import com.example.safenest.network.InstalledAppsResponse
import com.example.safenest.network.MessageResponse
import com.example.safenest.repository.ChildRepository
import com.example.safenest.repository.DigitalControlRepository
import com.example.safenest.util.Result
import com.example.safenest.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MonitoringViewModel(application: Application) : AndroidViewModel(application) {

    private val digitalControlRepository = DigitalControlRepository()
    private val childRepository = ChildRepository()
    private val sessionManager = SessionManager(application)

    private val _installedAppsState = MutableStateFlow<Result<InstalledAppsResponse>?>(null)
    val installedAppsState: StateFlow<Result<InstalledAppsResponse>?> = _installedAppsState.asStateFlow()

    private val _updateInstalledAppsState = MutableStateFlow<Result<MessageResponse>?>(null)
    val updateInstalledAppsState: StateFlow<Result<MessageResponse>?> = _updateInstalledAppsState.asStateFlow()

    private val _digitalRuleState = MutableStateFlow<Result<DigitalRuleResponse>?>(null)
    val digitalRuleState: StateFlow<Result<DigitalRuleResponse>?> = _digitalRuleState.asStateFlow()

    private val _updateDigitalRuleState = MutableStateFlow<Result<DigitalRuleResponse>?>(null)
    val updateDigitalRuleState: StateFlow<Result<DigitalRuleResponse>?> = _updateDigitalRuleState.asStateFlow()

    fun getInstalledApps(childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _installedAppsState.value = Result.Loading
            _installedAppsState.value = childRepository.getInstalledApps(childId)
        }
    }

    fun clearInstalledAppsState() {
        _installedAppsState.value = null
    }

    fun updateInstalledApps(childId: String, apps: List<InstalledAppItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateInstalledAppsState.value = Result.Loading
            _updateInstalledAppsState.value = childRepository.updateInstalledApps(childId, apps)
        }
    }

    fun clearUpdateInstalledAppsState() {
        _updateInstalledAppsState.value = null
    }

    fun getDigitalRule(childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _digitalRuleState.value = Result.Loading
            _digitalRuleState.value = digitalControlRepository.getDigitalRule(childId)
        }
    }

    fun clearDigitalRuleState() {
        _digitalRuleState.value = null
    }

    fun updateDigitalRule(
        ruleId: String,
        maxScreenTime: Int? = null,
        blockedApp: List<String>? = null,
        appTimeLimits: Map<String, Int>? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateDigitalRuleState.value = Result.Loading
            _updateDigitalRuleState.value = digitalControlRepository.updateDigitalRule(
                ruleId,
                maxScreenTime,
                blockedApp,
                appTimeLimits
            )
        }
    }

    fun clearUpdateDigitalRuleState() {
        _updateDigitalRuleState.value = null
    }

    fun getSelectedChildId(): String? = sessionManager.getSelectedChildId()
}
