# Frontend Implementation Plan: Website Protection

## Goal

Give parents a clear policy-management screen and give the Child a synchronized, offline-capable host/category enforcement layer without overstating what Android DNS filtering can guarantee.

## Parent architecture

The Parent uses Retrofit models, a repository, a ViewModel, and `WebsiteProtectionFragment`. The screen selects allowlist or blocklist mode, selects mandatory categories, adds normalized custom hosts, assigns a policy to a Child device, and publishes immutable versions. The UI explains that allowlist mode blocks unknown hosts while blocklist mode leaves unknown hosts open.

## Child architecture

`WebsitePolicySyncWorker` discovers assignments, downloads the complete versioned snapshot, stores it atomically, acknowledges the exact version/hash, and starts the VPN when Android consent is already available. `WebsitePolicyEngine` is pure Kotlin logic for allowlist/blocklist defaults, mandatory category decisions, host/subdomain matching, and approximate per-host budgets. `WebsiteDnsVpnService` is a foreground DNS-only `VpnService` that parses IPv4/UDP DNS packets, returns NXDOMAIN for blocked hosts, forwards allowed DNS queries through a protected socket, and reports health states.

## Permission and truthfulness decisions

The Child onboarding explicitly requests Android VPN consent and explains the host-level limitation. The home screen separates application protection from website-filtering status. Denied consent, another VPN, establishment failure, or no policy snapshot are shown as unavailable/degraded rather than as active protection. HTTPS paths and exact browser-tab closure remain unsupported in the first release.

## Release gates

Android unit tests and both debug APK builds must pass. Live-device verification remains pending until Parent and Child devices are paired and the VPN consent, policy refresh, allowlist/blocklist behavior, category blocking, budget behavior, and unavailable states can be observed on-device.
