# Frontend Spec: App-Control Modes

## Purpose

Give the Parent two explicit choices for app control: allow only selected applications, or block only selected applications.

## Requirements

The Parent SHALL submit `app_control_mode`, `allowed_app`, `blocked_app`, and `app_time_limits` through the digital-rule API. In allowlist mode, every non-protected package not in `allowed_app`, including future installations, SHALL be blocked by the Child. In blocklist mode, only `blocked_app` packages SHALL be blocked and future installations SHALL remain available. The Child SHALL default missing/unknown modes to blocklist for legacy compatibility.

The Parent UI SHALL explain the consequence of each mode, expose the installed inventory for package selection, warn before saving an empty allowlist, and preserve per-app time limits independently from access mode.
