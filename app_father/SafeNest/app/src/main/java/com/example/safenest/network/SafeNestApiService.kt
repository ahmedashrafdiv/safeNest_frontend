package com.example.safenest.network

import retrofit2.Response
import retrofit2.http.*

interface SafeNestApiService {

    companion object {
        const val BASE_URL = "https://safe-nest-deployment.vercel.app/"
    }

    // ============ Authentication ============

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(@Body request: EmailVerificationRequest): Response<MessageResponse>

    @POST("api/auth/resend-otp")
    suspend fun resendOtp(@Body request: ResendOtpRequest): Response<MessageResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>

    // ============ Parent Profile ============

    @GET("api/parents/profile")
    suspend fun getProfile(): Response<ParentResponse>

    @PUT("api/parents/profile")
    suspend fun updateProfile(@Body request: ParentUpdateRequest): Response<ParentResponse>

    @POST("api/parents/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>

    @DELETE("api/parents/account")
    suspend fun deleteAccount(): Response<MessageResponse>

    @PUT("api/parents/fcm-token")
    suspend fun updateFcmToken(@Body request: FCMTokenUpdateRequest): Response<MessageResponse>

    // ============ Children ============

    @GET("api/children")
    suspend fun getChildren(): Response<List<ChildResponse>>

    @POST("api/children")
    suspend fun createChild(@Body request: ChildCreateRequest): Response<ChildResponse>

    @GET("api/children/{child_id}")
    suspend fun getChild(@Path("child_id") childId: String): Response<ChildResponse>

    @PUT("api/children/{child_id}")
    suspend fun updateChild(
        @Path("child_id") childId: String,
        @Body request: ChildUpdateRequest
    ): Response<ChildResponse>

    @DELETE("api/children/{child_id}")
    suspend fun deleteChild(@Path("child_id") childId: String): Response<MessageResponse>

    @GET("api/children/{child_id}/installed-apps")
    suspend fun getInstalledApps(@Path("child_id") childId: String): Response<InstalledAppsResponse>

    @PUT("api/children/{child_id}/installed-apps")
    suspend fun updateInstalledApps(
        @Path("child_id") childId: String,
        @Body request: InstalledAppsUpdateRequest
    ): Response<MessageResponse>

    // ============ Digital Control ============

    @POST("api/digital-control/rule")
    suspend fun createDigitalRule(@Body request: DigitalRuleCreateRequest): Response<DigitalRuleResponse>

    @GET("api/digital-control/child/{child_id}")
    suspend fun getDigitalRule(@Path("child_id") childId: String): Response<DigitalRuleResponse>

    @PUT("api/digital-control/{rule_id}")
    suspend fun updateDigitalRule(
        @Path("rule_id") ruleId: String,
        @Body request: DigitalRuleUpdateRequest
    ): Response<DigitalRuleResponse>

    @DELETE("api/digital-control/rule/{rule_id}")
    suspend fun deleteDigitalRule(@Path("rule_id") ruleId: String): Response<Unit>

    @POST("api/digital-control/app-usage")
    suspend fun updateAppUsage(@Body request: AppUsageUpdateRequest): Response<MessageResponse>

    @DELETE("api/digital-control/child/{child_id}/daily-usage-log")
    suspend fun clearDailyUsageLog(@Path("child_id") childId: String): Response<Unit>

    @POST("api/digital-control/video-history")
    suspend fun addVideoHistory(@Body request: VideoHistoryCreateRequest): Response<Unit>

    @GET("api/digital-control/child/{child_id}/video-history")
    suspend fun getVideoHistory(@Path("child_id") childId: String): Response<List<Map<String, Any>>>

    @DELETE("api/digital-control/child/{child_id}/video-history")
    suspend fun clearVideoHistory(@Path("child_id") childId: String): Response<Unit>

    // ============ Devices ============

    @POST("api/devices/pair")
    suspend fun pairDevice(@Body request: DevicePairingRequest): Response<DeviceOut>

    @GET("api/devices")
    suspend fun listDevices(): Response<List<DeviceOut>>

    @GET("api/devices/status")
    suspend fun listDevicesStatus(): Response<List<DeviceStatusResponse>>

    @DELETE("api/devices/{device_id}")
    suspend fun deleteDevice(@Path("device_id") deviceId: String): Response<Map<String, Any>>

    @POST("api/devices/generate-pin")
    suspend fun generatePin(@Body request: GeneratePinRequest): Response<GeneratePinResponse>

    // ============ GPS ============

    @POST("gps/pair")
    suspend fun pairGps(@Query("child_id") childId: String): Response<Unit>

    @POST("gps/update-from-thingspeak")
    suspend fun updateGpsFromThingspeak(@Query("child_id") childId: String): Response<Unit>

    @DELETE("gps/{child_id}")
    suspend fun deleteGps(@Path("child_id") childId: String): Response<Unit>

    // ============ Location ============

    @GET("location/live/{child_id}")
    suspend fun getChildLocation(@Path("child_id") childId: String): Response<Map<String, Any>>

    // ============ Zones ============

    @POST("api/zones")
    suspend fun createZone(@Body request: ZoneCreateRequest): Response<ZoneResponse>

    @GET("api/zones/child/{child_id}")
    suspend fun getChildZones(@Path("child_id") childId: String): Response<List<ZoneResponse>>

    @PUT("api/zones/{zone_id}")
    suspend fun updateZone(
        @Path("zone_id") zoneId: String,
        @Body request: ZoneUpdateRequest
    ): Response<ZoneResponse>

    @DELETE("api/zones/{zone_id}")
    suspend fun deleteZone(@Path("zone_id") zoneId: String): Response<Unit>

    // ============ Alerts ============

    @GET("api/alerts")
    suspend fun listAlerts(): Response<List<AlertOut>>

    @PUT("api/alerts/{alert_id}")
    suspend fun updateAlert(
        @Path("alert_id") alertId: String,
        @Body request: AlertUpdateRequest
    ): Response<AlertOut>

    @DELETE("api/alerts/{alert_id}")
    suspend fun deleteAlert(@Path("alert_id") alertId: String): Response<Map<String, Any>>
}
