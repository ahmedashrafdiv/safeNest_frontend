package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.ChildResponse
import com.example.safenest.network.DigitalRuleResponse
import com.example.safenest.repository.ChildRepository
import com.example.safenest.repository.DigitalControlRepository
import com.example.safenest.util.Result
import com.example.safenest.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val childRepository = ChildRepository()
    private val digitalControlRepository = DigitalControlRepository()
    private val sessionManager = SessionManager(application)

    private val _childrenState = MutableStateFlow<Result<List<ChildResponse>>?>(null)
    val childrenState: StateFlow<Result<List<ChildResponse>>?> = _childrenState.asStateFlow()

    private val _digitalRuleState = MutableStateFlow<Result<DigitalRuleResponse>?>(null)
    val digitalRuleState: StateFlow<Result<DigitalRuleResponse>?> = _digitalRuleState.asStateFlow()

    fun getChildren() {
        viewModelScope.launch(Dispatchers.IO) {
            _childrenState.value = Result.Loading
            _childrenState.value = childRepository.getChildren()
        }
    }

    fun clearChildrenState() {
        _childrenState.value = null
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

    /** Persists the selected child into SessionManager so other screens can read it. */
    fun saveSelectedChildId(childId: String) = sessionManager.saveSelectedChildId(childId)

    fun getSelectedChildId(): String? = sessionManager.getSelectedChildId()
}
