package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.ZoneResponse
import com.example.safenest.repository.ZoneRepository
import com.example.safenest.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SafeZonesViewModel(application: Application) : AndroidViewModel(application) {

    private val zoneRepository = ZoneRepository()

    private val _zonesState = MutableStateFlow<Result<List<ZoneResponse>>?>(null)
    val zonesState: StateFlow<Result<List<ZoneResponse>>?> = _zonesState.asStateFlow()

    private val _createZoneState = MutableStateFlow<Result<ZoneResponse>?>(null)
    val createZoneState: StateFlow<Result<ZoneResponse>?> = _createZoneState.asStateFlow()

    private val _deleteZoneState = MutableStateFlow<Result<Unit>?>(null)
    val deleteZoneState: StateFlow<Result<Unit>?> = _deleteZoneState.asStateFlow()

    fun getChildZones(childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _zonesState.value = Result.Loading
            _zonesState.value = zoneRepository.getChildZones(childId)
        }
    }

    fun clearZonesState() {
        _zonesState.value = null
    }

    fun createZone(name: String, zoneType: String, childId: String, latitude: Double, longitude: Double, radiusMeters: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _createZoneState.value = Result.Loading
            _createZoneState.value = zoneRepository.createZone(name, zoneType, childId, latitude, longitude, radiusMeters)
        }
    }

    fun clearCreateZoneState() {
        _createZoneState.value = null
    }

    fun deleteZone(zoneId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _deleteZoneState.value = Result.Loading
            _deleteZoneState.value = zoneRepository.deleteZone(zoneId)
        }
    }

    fun clearDeleteZoneState() {
        _deleteZoneState.value = null
    }
}
