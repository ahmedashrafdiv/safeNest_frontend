package com.example.safenest.util

import com.example.safenest.network.AccessRequestItem
import com.example.safenest.network.AlertOut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentInboxPresentationTest {
    @Test
    fun test_extra_time_request_has_approval_and_keeps_limit_actions() {
        val presentation = ParentInboxPresentation.request(request(requestType = "extra_time", scopeValue = "Roblox", requestedSeconds = 900))

        assertEquals(InboxRequestKind.EXTRA_TIME, presentation.kind)
        assertTrue(presentation.title.contains("15 دقيقة"))
        assertEquals("موافقة", presentation.primaryActionLabel)
        assertEquals("إبقاء الحد", presentation.secondaryActionLabel)
    }

    @Test
    fun test_app_access_request_uses_respectful_allow_and_not_now_actions() {
        val presentation = ParentInboxPresentation.request(request(requestType = "access_override", scopeValue = "com.spotify.music"))

        assertEquals(InboxRequestKind.APP_ACCESS, presentation.kind)
        assertTrue(presentation.title.contains("Spotify"))
        assertEquals("سماح", presentation.primaryActionLabel)
        assertEquals("ليس الآن", presentation.secondaryActionLabel)
    }

    @Test
    fun test_blocked_app_alert_has_text_status_and_rule_review_action() {
        val presentation = ParentInboxPresentation.alert(alert(type = "app_blocked", message = "Blocked TikTok launch"))

        assertEquals(InboxAlertKind.BLOCKED_APP, presentation.kind)
        assertEquals("تم الحظر", presentation.statusLabel)
        assertEquals(InboxAlertAction.REVIEW_APP_RULES, presentation.action)
        assertTrue(presentation.title.contains("TikTok"))
    }

    @Test
    fun test_safe_place_arrival_does_not_expose_coordinates_or_require_action() {
        val presentation = ParentInboxPresentation.alert(alert(type = "safe_zone_arrival", message = "وصلت ليان إلى البيت"))

        assertEquals(InboxAlertKind.SAFE_PLACE_ARRIVAL, presentation.kind)
        assertEquals("وصول آمن", presentation.statusLabel)
        assertEquals(InboxAlertAction.NONE, presentation.action)
        assertFalse(presentation.detail.contains("latitude", ignoreCase = true))
    }

    @Test
    fun test_attention_place_entry_is_informational_and_has_no_location_action() {
        val presentation = ParentInboxPresentation.alert(alert(type = "place_attention_entry", message = "دخلت ليان مكتبة الحي"))

        assertEquals(InboxAlertKind.ATTENTION_PLACE_ENTRY, presentation.kind)
        assertEquals(InboxAlertAction.NONE, presentation.action)
        assertEquals("مكان يحتاج انتباهًا", presentation.statusLabel)
    }

    @Test
    fun test_risk_place_entry_allows_location_without_exposing_coordinates() {
        val presentation = ParentInboxPresentation.alert(alert(type = "place_risk_entry", message = "دخلت ليان الطريق المزدحم"))

        assertEquals(InboxAlertKind.RISK_PLACE_ENTRY, presentation.kind)
        assertEquals(InboxAlertAction.VIEW_LOCATION, presentation.action)
        assertFalse(presentation.detail.contains("latitude", ignoreCase = true))
        assertFalse(presentation.detail.contains("longitude", ignoreCase = true))
    }

    @Test
    fun test_unknown_alert_falls_back_without_inventing_sensitive_event() {
        val presentation = ParentInboxPresentation.alert(alert(type = "battery_warning", message = ""))

        assertEquals(InboxAlertKind.GENERAL, presentation.kind)
        assertEquals(InboxAlertAction.NONE, presentation.action)
        assertEquals("battery_warning", presentation.title)
    }

    private fun request(
        requestType: String,
        scopeValue: String,
        requestedSeconds: Int = 0,
    ) = AccessRequestItem(
        requestId = "request-1",
        childId = "child-1",
        deviceId = "device-1",
        requestType = requestType,
        scopeType = "app",
        scopeValue = scopeValue,
        requestedSeconds = requestedSeconds,
        reason = null,
        status = "pending",
        requestedAt = "2026-08-20T13:10:00Z",
        requestExpiresAt = null,
        childName = "ليان",
    )

    private fun alert(type: String, message: String) = AlertOut(
        alertId = "alert-1",
        parentId = "parent-1",
        deviceId = "device-1",
        deviceName = "Layan phone",
        alertType = type,
        message = message,
        isResolved = false,
        timestamp = "2026-08-20T13:10:00Z",
    )
}
