package com.safenest.kids.service

import com.safenest.kids.network.WebsitePolicyRuleSnapshot
import com.safenest.kids.network.WebsitePolicySyncResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsitePolicyEngineTest {
    private fun policy(mode: String, rules: List<WebsitePolicyRuleSnapshot> = emptyList(), categories: List<String> = emptyList()) =
        WebsitePolicySyncResponse(
            deviceId = "device-1",
            childId = "child-1",
            policyId = "policy-1",
            version = 1,
            contentHash = "hash",
            defaultAction = "allow_with_logging",
            websiteControlMode = mode,
            mandatoryBlockedCategories = categories,
            timezone = "UTC",
            rules = rules
        )

    @Test
    fun allowlistBlocksUnknownHost() {
        val engine = WebsitePolicyEngine(policy("allowlist", listOf(rule("safe.example", "allow"))))
        assertFalse(engine.decide("safe.example").blocked)
        assertTrue(engine.decide("new.example").blocked)
    }

    @Test
    fun blocklistLeavesUnknownHostOpen() {
        val engine = WebsitePolicyEngine(policy("blocklist", listOf(rule("blocked.example", "block"))))
        assertTrue(engine.decide("blocked.example").blocked)
        assertFalse(engine.decide("new.example").blocked)
    }

    @Test
    fun mandatoryAdultCategoryBlocksMatchingHost() {
        val engine = WebsitePolicyEngine(policy("blocklist", categories = listOf("adult")))
        assertTrue(engine.decide("example-porn-site.com").blocked)
    }

    @Test
    fun dailyBudgetBlocksAfterObservedUsage() {
        val engine = WebsitePolicyEngine(policy("blocklist", listOf(rule("video.example", "allow", 60))))
        assertFalse(engine.decide("video.example", 1_000L).blocked)
        assertTrue(engine.decide("video.example", 62_000L).blocked)
    }

    private fun rule(host: String, action: String, budget: Int? = null) = WebsitePolicyRuleSnapshot(
        normalizedPattern = host,
        matchMode = "host_and_subdomains",
        action = action,
        category = null,
        priority = 900,
        scheduleId = null,
        dailyBudgetSeconds = budget
    )
}
