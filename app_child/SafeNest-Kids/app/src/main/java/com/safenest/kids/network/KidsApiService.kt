package com.safenest.kids.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface KidsApiService {
    @POST("api/child-devices/{device_id}/location")
    suspend fun uploadPhoneLocation(
        @Path("device_id") deviceId: String,
        @Body request: PhoneLocationUploadRequest
    ): Response<PhoneLocationUploadResponse>

    @POST("api/devices/link-device")
    suspend fun linkDevice(@Body request: LinkDeviceRequest): Response<LinkDeviceResponse>

    @GET("api/digital-control/device/rules")
    suspend fun getDeviceRules(): Response<DigitalRuleResponse>

    @GET("api/child-devices/{device_id}/website-policies")
    suspend fun listWebsiteAssignments(@Path("device_id") deviceId: String): Response<WebsiteAssignmentListResponse>

    @GET("api/child-devices/{device_id}/website-policies/{policy_id}")
    suspend fun getWebsitePolicy(
        @Path("device_id") deviceId: String,
        @Path("policy_id") policyId: String
    ): Response<WebsitePolicySyncResponse>

    @POST("api/child-devices/{device_id}/website-policies/{policy_id}/ack")
    suspend fun acknowledgeWebsitePolicy(
        @Path("device_id") deviceId: String,
        @Path("policy_id") policyId: String,
        @Body request: WebsitePolicyAckRequest
    ): Response<Map<String, Any>>

    @POST("api/child-devices/{device_id}/health")
    suspend fun reportProtectionHealth(
        @Path("device_id") deviceId: String,
        @Body request: DeviceProtectionHealthRequest,
    ): Response<DeviceProtectionHealthResponse>

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

    @GET("api/children/device-effective/app-blocking")
    suspend fun getEffectiveAppBlockingPolicy(): Response<EffectiveAppBlockingPolicyResponse>
    @GET("api/child-devices/{device_id}/screen-time-policy")
    suspend fun getScreenTimePolicy(
        @Path("device_id") deviceId: String,
    ): Response<ScreenTimePolicySyncPayload>}
