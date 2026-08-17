package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.ChildResponse
import com.example.safenest.network.WebsitePolicyResponse
import com.example.safenest.network.WebsiteRuleListResponse
import com.example.safenest.repository.WebsitePolicyRepository
import com.example.safenest.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebsiteProtectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WebsitePolicyRepository()
    private val _policyState = MutableStateFlow<Result<WebsitePolicyResponse>?>(null)
    val policyState: StateFlow<Result<WebsitePolicyResponse>?> = _policyState.asStateFlow()
    private val _rulesState = MutableStateFlow<Result<WebsiteRuleListResponse>?>(null)
    val rulesState: StateFlow<Result<WebsiteRuleListResponse>?> = _rulesState.asStateFlow()
    private val _actionState = MutableStateFlow<Result<Any>?>(null)
    val actionState: StateFlow<Result<Any>?> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _policyState.value = Result.Loading
            val result = repository.listPolicies()
            _policyState.value = when (result) {
                is Result.Success -> {
                    val existing = result.data.items.firstOrNull { it.status == "draft" || it.status == "published" }
                    if (existing != null) Result.Success(existing)
                    else repository.createPolicy("blocklist", emptyList())
                }
                is Result.Error -> result
                Result.Loading -> Result.Loading
            }
        }
    }

    fun loadRules(policyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _rulesState.value = Result.Loading
            _rulesState.value = repository.listRules(policyId)
        }
    }

    fun updateMode(policyId: String, mode: String, categories: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _actionState.value = Result.Loading
            _actionState.value = repository.updatePolicy(policyId, mode, categories)
        }
    }

    fun addHost(policyId: String, host: String, action: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _actionState.value = Result.Loading
            _actionState.value = repository.addHostRule(policyId, host, action)
        }
    }

    fun publish(policyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _actionState.value = Result.Loading
            _actionState.value = repository.publish(policyId)
        }
    }

    fun assign(policyId: String, child: ChildResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            _actionState.value = Result.Loading
            _actionState.value = repository.assign(policyId, child)
        }
    }

    fun assignSelectedChild(policyId: String, childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _actionState.value = Result.Loading
            _actionState.value = when (val childResult = repository.getChild(childId)) {
                is Result.Success -> repository.assign(policyId, childResult.data)
                is Result.Error -> childResult
                Result.Loading -> Result.Loading
            }
        }
    }
}
