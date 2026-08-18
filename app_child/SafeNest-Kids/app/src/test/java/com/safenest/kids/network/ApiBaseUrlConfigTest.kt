package com.safenest.kids.network

import com.safenest.kids.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiBaseUrlConfigTest {
    @Test
    fun debugBuildUsesDeployedBackendForPolicySynchronization() {
        assertEquals(
            "https://safe-nest-deployment.vercel.app/",
            BuildConfig.API_BASE_URL,
        )
    }
}
