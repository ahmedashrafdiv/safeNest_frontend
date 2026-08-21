# Orchestrator State: Device Linking via OTP

| Item | Value |
|---|---|
| Feature | `frontend-device-linking-otp` |
| Scope | Parent Android, Child Android, Backend pairing API |
| Current phase | Physical two-device verification pending |
| Legacy safety | No IoT or sensor pairing changes |
| Publishing | No GitHub push or deploy authorised |
| Device verification | `adb devices -l` أعاد قائمة فارغة في هذه الدورة؛ يلزم جهازان Android/Realme لاختبار إنشاء OTP، إدخاله، Waiting→Success، expiry، replay، ثم handoff إلى PermissionsFragment |
