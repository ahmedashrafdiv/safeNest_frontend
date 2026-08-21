package com.example.safenest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.ChildPlaceResponse
import com.example.safenest.network.PlaceCreateRequest
import com.example.safenest.network.PlaceUpdateRequest
import com.example.safenest.repository.PlaceRepository
import com.example.safenest.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlaceViewModel : ViewModel() {
    private val repository = PlaceRepository()
    private val _placesState = MutableStateFlow<Result<List<ChildPlaceResponse>>?>(null)
    val placesState: StateFlow<Result<List<ChildPlaceResponse>>?> = _placesState
    private val _mutationState = MutableStateFlow<Result<ChildPlaceResponse>?>(null)
    val mutationState: StateFlow<Result<ChildPlaceResponse>?> = _mutationState

    fun load(childId: String) = viewModelScope.launch {
        _placesState.value = Result.Loading
        _placesState.value = repository.listPlaces(childId)
    }

    fun create(childId: String, request: PlaceCreateRequest) = viewModelScope.launch {
        _mutationState.value = Result.Loading
        _mutationState.value = repository.createPlace(childId, request)
    }

    fun update(childId: String, placeId: String, request: PlaceUpdateRequest) = viewModelScope.launch {
        _mutationState.value = Result.Loading
        _mutationState.value = repository.updatePlace(childId, placeId, request)
    }

    fun clearMutation() { _mutationState.value = null }
    fun clearPlaces() { _placesState.value = null }
}
