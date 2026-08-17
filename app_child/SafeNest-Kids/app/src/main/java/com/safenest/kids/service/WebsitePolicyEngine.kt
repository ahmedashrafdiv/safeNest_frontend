package com.safenest.kids.service

import com.google.gson.Gson
import com.safenest.kids.network.WebsitePolicyRuleSnapshot
import com.safenest.kids.network.WebsitePolicySyncResponse
import java.util.Locale

class WebsitePolicyEngine(private val policy: WebsitePolicySyncResponse) {
    private val usageSeconds = mutableMapOf<String, Long>()
    private val lastObserved = mutableMapOf<String, Long>()
    private val dayStamp = currentDayStamp()

    data class Decision(val blocked: Boolean, val reason: String)

    fun decide(rawHost: String, nowMillis: Long = System.currentTimeMillis()): Decision {
        val host = normalizeHost(rawHost) ?: return Decision(
            blocked = policy.websiteControlMode == "allowlist",
            reason = "invalid_host"
        )
        if (isMandatoryCategoryHost(host)) return Decision(true, "mandatory_category_block")

        val matching = policy.rules
            .filter { matches(it, host) }
            .sortedWith(compareByDescending<WebsitePolicyRuleSnapshot> { it.priority }.thenBy { it.matchMode })
            .firstOrNull()
        val ruleDecision = when {
            matching?.action == "block" -> Decision(true, "explicit_parent_rule")
            matching?.action == "allow" -> Decision(false, "explicit_parent_rule")
            policy.websiteControlMode == "allowlist" -> Decision(true, "allowlist_default")
            else -> Decision(false, "safe_default")
        }
        if (ruleDecision.blocked) return ruleDecision

        val budget = matching?.dailyBudgetSeconds ?: return ruleDecision
        val used = updateUsage(host, nowMillis)
        return if (used >= budget) Decision(true, "daily_budget_exhausted") else ruleDecision
    }

    private fun matches(rule: WebsitePolicyRuleSnapshot, host: String): Boolean {
        val pattern = normalizeHost(rule.normalizedPattern) ?: return false
        return when (rule.matchMode) {
            "exact_host" -> host == pattern
            "host_and_subdomains" -> host == pattern || host.endsWith(".$pattern")
            else -> false
        }
    }

    private fun isMandatoryCategoryHost(host: String): Boolean {
        if (!policy.mandatoryBlockedCategories.contains("adult")) return false
        val tokens = listOf("porn", "xxx", "sex", "adult", "hentai", "nudity")
        return tokens.any { host.contains(it) }
    }

    private fun updateUsage(host: String, nowMillis: Long): Long {
        if (dayStamp != currentDayStamp()) return 0L
        val previous = lastObserved[host]
        if (previous != null) {
            val delta = ((nowMillis - previous) / 1000L).coerceIn(0L, 60L)
            usageSeconds[host] = (usageSeconds[host] ?: 0L) + delta
        }
        lastObserved[host] = nowMillis
        return usageSeconds[host] ?: 0L
    }

    companion object {
        private fun currentDayStamp(): String = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(java.util.Date())

        fun normalizeHost(value: String): String? {
            val candidate = value.trim().lowercase(Locale.US).trimEnd('.')
            if (candidate.isBlank() || candidate.contains("/") || candidate.contains(":") || candidate.contains(" ")) return null
            return candidate
        }
    }
}
