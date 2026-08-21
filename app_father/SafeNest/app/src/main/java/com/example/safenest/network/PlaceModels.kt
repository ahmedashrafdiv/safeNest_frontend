package com.example.safenest.network

import com.google.gson.annotations.SerializedName

data class PlaceCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("place_type") val placeType: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius_meters") val radiusMeters: Int,
    @SerializedName("notify_on_enter") val notifyOnEnter: Boolean,
    @SerializedName("notify_on_exit") val notifyOnExit: Boolean,
)

data class PlaceUpdateRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("place_type") val placeType: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("radius_meters") val radiusMeters: Int? = null,
    @SerializedName("notify_on_enter") val notifyOnEnter: Boolean? = null,
    @SerializedName("notify_on_exit") val notifyOnExit: Boolean? = null,
)

data class ChildPlaceResponse(
    @SerializedName("place_id") val placeId: String,
    @SerializedName("parent_id") val parentId: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("name") val name: String,
    @SerializedName("place_type") val placeType: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius_meters") val radiusMeters: Int,
    @SerializedName("notify_on_enter") val notifyOnEnter: Boolean,
    @SerializedName("notify_on_exit") val notifyOnExit: Boolean,
    @SerializedName("place_version") val placeVersion: Int,
    @SerializedName("legacy") val legacy: Boolean = false,
)
