package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.ChildResponse
import com.example.safenest.repository.LocationRepository
import com.example.safenest.repository.ChildRepository
import com.example.safenest.util.Result
import com.example.safenest.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GpsViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository()
    private val childRepository = ChildRepository()
    private val sessionManager = SessionManager(application)

    private val _locationState = MutableStateFlow<Result<Map<String, Any>>?>(null)
    val locationState: StateFlow<Result<Map<String, Any>>?> = _locationState.asStateFlow()

    private val _childState = MutableStateFlow<Result<ChildResponse>?>(null)
    val childState: StateFlow<Result<ChildResponse>?> = _childState.asStateFlow()

    fun getChildLocation(childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _locationState.value = Result.Loading
            _locationState.value = locationRepository.getChildLocation(childId)
        }
    }

    fun clearLocationState() {
        _locationState.value = null
    }

    fun getChild(childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _childState.value = Result.Loading
            _childState.value = childRepository.getChild(childId)
        }
    }

    fun clearChildState() {
        _childState.value = null
    }

    fun getSelectedChildId(): String? = sessionManager.getSelectedChildId()
}
