package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.ParentResponse
import com.example.safenest.repository.ParentRepository
import com.example.safenest.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val parentRepository = ParentRepository()

    private val _profileState = MutableStateFlow<Result<ParentResponse>?>(null)
    val profileState: StateFlow<Result<ParentResponse>?> = _profileState.asStateFlow()

    private val _updateProfileState = MutableStateFlow<Result<ParentResponse>?>(null)
    val updateProfileState: StateFlow<Result<ParentResponse>?> = _updateProfileState.asStateFlow()

    fun getProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            _profileState.value = Result.Loading
            _profileState.value = parentRepository.getProfile()
        }
    }

    fun clearProfileState() {
        _profileState.value = null
    }

    fun updateProfile(name: String, phoneNumber: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateProfileState.value = Result.Loading
            _updateProfileState.value = parentRepository.updateProfile(name, phoneNumber)
        }
    }

    fun clearUpdateProfileState() {
        _updateProfileState.value = null
    }
}
