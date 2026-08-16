package com.safenest.kids.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface KidsApiService {
    @POST("api/devices/link-device")
    suspend fun linkDevice(@Body request: LinkDeviceRequest): Response<LinkDeviceResponse>

    @GET("api/digital-control/device/rules")
    suspend fun getDeviceRules(): Response<DigitalRuleResponse>

    // TODO: The exact request/response body shape for installed-apps is NOT fully documented.
    // Verify against the live Swagger UI at https://safe-nest-deployment.vercel.app/docs
    // before relying on it in production.
    @PUT("api/children/{child_id}/installed-apps")
    suspend fun updateInstalledApps(
        @Path("child_id") childId: String,
        @Body request: InstalledAppsRequest
    ): Response<Unit>

    @POST("api/digital-control/app-usage")
    suspend fun reportAppUsage(@Body request: AppUsageRequest): Response<Unit>

    /**
     * Update the FCM token for an already-paired child device.
     * Authenticated via the device access token (Bearer).
     *
     * NOTE: The backend team must implement this endpoint.
     * It mirrors PUT /api/parents/fcm-token but writes to the
     * Devices collection document matching the authenticated device.
     */
    @PUT("api/devices/fcm-token")
    suspend fun updateDeviceFcmToken(@Body request: UpdateFcmTokenRequest): Response<Unit>
}
