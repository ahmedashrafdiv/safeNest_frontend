# Frontend Spec: Website Protection

The Parent SHALL manage website policies using allowlist/blocklist modes, protected categories, normalized custom hosts, schedules, and daily budgets. The Child SHALL synchronize a published snapshot and enforce host/subdomain/category decisions through an Android `VpnService` or an explicitly reported unsupported state. The existing app AccessibilityService remains responsible for application enforcement and is not the website-filtering engine.

The first Android release is host-level and category-level. It SHALL disclose that HTTPS paths and exact browser-tab closure are not guaranteed without browser-specific integration. Missing legacy mode defaults to blocklist; allowlist mode blocks unknown third-party hosts by default.
