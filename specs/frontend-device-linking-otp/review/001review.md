# Spec Review: frontend-device-linking-otp
- Branch: repository has concurrent uncommitted work; OTP review is limited to the feature files.
- Spec resolved via: explicit feature folder `specs/frontend-device-linking-otp`.
- Resolution conflicts: `.specify/feature.json` points to another in-progress feature; the explicit OTP folder is used intentionally.
- Review file: `001review.md`
- Detected commands: backend test=`set JWT_SECRET=device-management-route-test-secret && python -m pytest -q`; Parent test/build=`gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain`; Child test/build=`gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain`; lint=`not configured`.

## Summary

- Overall status: PASS for repository verification; physical E2E remains UNKNOWN.
- High-risk issues: none open after Issue 1 fix.
- Missing tests / regression risk: physical two-device rendering and transition verification remains deferred to Realme.
- Test suite results: Backend `273 passed`; targeted pairing suite `13 passed`; Parent Gradle test/build passed; Child Gradle test/build passed.
- Lint results: not configured.
- Type check results: Android Kotlin compilation passed for both apps; no standalone type-check command is configured.

## Task-by-task Verification

### Task T001: تحديث schema/service/routes لاستخدام OTP سداسي وحالة pairing آمنة
- Spec requirement / acceptance criteria: رمز رقمي من ست خانات، عشر دقائق، hash فقط، claim أحادي الاستخدام، وحالة Pairing مقيدة بالأب.
- Implementation found:
  - Files: `backend/safenest_review/app/schemas/device_management_schemas.py`, `backend/safenest_review/app/services/device_management_pairing_service.py`, `backend/safenest_review/app/routes/device_management_routes.py`.
  - Key symbols: `DevicePairingCreate`, `DevicePairingStatusResponse`, `_create_unique_otp`, `claim`, `get_pairing_status`.
- Status: PARTIAL.
- Evidence: OTP schema is `^\d{6}$`, default expiry is 600 seconds, pairing persistence stores `token_hash`, claim uses a Firestore transaction when available, and Parent-only status does not return pairing code/token.
- Problems: configured three-attempt behavior is not applied when `_find_pairing` cannot identify a guessed code.
- Proposed fix: implement device-scoped short-lived invalid-claim attempt tracking, reject after the configured default limit, and add focused test coverage.
- Proposed tests: `set JWT_SECRET=device-management-route-test-secret && python -m pytest -q tests/test_device_management_phase2.py tests/test_device_management_phase3.py tests/test_device_management_phase6.py`.

### Task T002: إضافة اختبارات Backend للانتهاء وإعادة الاستخدام وحالة pairing
- Spec requirement / acceptance criteria: تغطية OTP، انتهاء الصلاحية، replay، وحالة Parent الآمنة.
- Implementation found:
  - Files: `backend/safenest_review/tests/test_device_management_phase2.py`, `tests/test_device_management_phase3.py`, `tests/test_device_management_phase6.py`.
  - Key symbols: `test_pairing_accepts_only_six_digit_otp_with_ten_minute_default`, `test_parent_pairing_status_exposes_progress_without_pairing_secret`.
- Status: PARTIAL.
- Evidence: المجموعة تغطي contract السداسي وhash/status/replay/expiry ومسار status؛ لا توجد حالة تستنفد محاولات رمز مجهول.
- Problems: يتبع Issue 1.
- Proposed fix: أضف اختباراً لسلسلة مطالبات خاطئة من نفس الجهاز يثبت رفض الرابعة وحدود صلاحيتها.
- Proposed tests: نفس أمر pairing المحدد أعلاه.

### Task T003: إضافة Parent models/repository/ViewModel لمسار create/status pairing
- Spec requirement / acceptance criteria: Parent ينشئ pairing ويقرأ حالته ضمن سياق الطفل المحدد.
- Implementation found:
  - Files: `app_father/SafeNest/app/src/main/java/com/example/safenest/network/ChildDeviceModels.kt`, `SafeNestApiService.kt`, `repository/ChildDeviceRepository.kt`, `viewmodel/ChildDevicesViewModel.kt`.
  - Key symbols: `ChildDevicePairingStatusResponse`, `getChildDevicePairingStatus`, `refreshPairingStatus`.
- Status: PASS.
- Evidence: المسار child-scoped، والـ ViewModel يعيد خطأ عربياً عند غياب الطفل، وبناء Parent نجح.

### Task T004: بناء DeviceLinkingFragment وواجهات Ready/Code/Waiting/Success
- Spec requirement / acceptance criteria: حالات Parent العربية الأربع مع countdown وpolling lifecycle-bound.
- Implementation found:
  - Files: `fragments/DeviceLinkingFragment.kt`, `res/layout/fragment_device_linking.xml`.
  - Key symbols: `LinkScreen`, `startCountdown`, `startPolling`, `handlePairingStatus`.
- Status: PASS.
- Evidence: الشاشة تعرض النصوص المطلوبة وNavy code card وMint safety/success surfaces؛ polling يلغي في `onDestroyView` وحالة success تعتمد `CLAIMED` الخادمية؛ بناء Parent نجح.

### Task T005: ربط MyDevicesFragment بالمسار العربي الجديد
- Spec requirement / acceptance criteria: لا Toast للرمز؛ يبدأ التدفق من صفحة الأجهزة مع سياق الطفل.
- Implementation found:
  - Files: `fragments/MyDevicesFragment.kt`.
  - Key symbols: `addPairingAction`.
- Status: PASS.
- Evidence: زر «ربط جهاز جديد» يتنقل إلى `DeviceLinkingFragment` ولا ينشئ رمزاً قبل قرار الأب.

### Task T006: تحديث Child claim models/service وواجهة OTP سداسية وخطأ الاسترجاع
- Spec requirement / acceptance criteria: ست خلايا LTR في RTL، claim الحديث، رسالة الخطأ المحددة، والانتقال إلى الأذونات بعد النجاح.
- Implementation found:
  - Files: `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/PairingFragment.kt`, `network/ApiModels.kt`, `network/KidsApiService.kt`, `network/ApiClient.kt`, `res/layout/fragment_pairing.xml`.
  - Key symbols: `ChildDeviceClaimRequest`, `claimChildDevice`, `configureOtpCells`, `showOtpError`, `completePairing`.
- Status: PASS.
- Evidence: خلايا إدخال مستقلة تدعم لصق الأرقام والرجوع؛ endpoint claim معفى من إرسال token؛ النجاح يحفظ child/device token ثم يعرض `PermissionsFragment`؛ البناء والاختبارات نجحت.

### Task T007: إضافة اختبارات Parent/Child JVM للمنطق الجديد
- Spec requirement / acceptance criteria: تغطية state/countdown في Parent وخلايا OTP ومطابقة الأخطاء في Child.
- Implementation found:
  - Files: لا توجد اختبارات JVM مخصصة لرمز OTP الجديد في Parent أو Child.
- Status: FAIL.
- Evidence: أوامر Gradle نجحت، لكن لا يوجد regression test للانتقال إلى success بعد `claimed`، أو expiry/resend، أو لصق/إدخال OTP في Child.
- Problems: يتبع Issue 2.
- Proposed fix: استخراج حساب الوقت/تحويل حالة Parent والتحقق من OTP إلى helpers نقية صغيرة تُستخدم من Fragment وتُختبر في JVM.
- Proposed tests: Parent وChild `testDebugUnitTest` بعد إضافة الاختبارات.

### Task T008: تشغيل build/tests والمراجعات ومعالجة البنود المفتوحة
- Spec requirement / acceptance criteria: نجاح الاختبارات والبناء والمراجعات المقيّدة للنطاق.
- Implementation found:
  - Files: هذا التقرير، أوامر test/build المسجلة أعلاه.
- Status: PARTIAL.
- Evidence: كل أوامر البناء والاختبار الحالية ناجحة، لكن Issue 1 وIssue 2 ما زالا مفتوحين.
- Proposed fix: تطبيق Fix Plan ثم إعادة أوامر الجودة الكاملة.

### Task T009: توثيق اختبار Realme E2E
- Spec requirement / acceptance criteria: اختبار جهازين عند التوفر، أو توثيق أنه متبقٍ.
- Implementation found:
  - Files: `orchestrator-state.md`.
- Status: UNKNOWN.
- Evidence: `adb devices -l` أعاد قائمة فارغة في هذه الدورة؛ لا يزعم التقرير نجاحاً فيزيائياً.
- Proposed fix: عند اتصال Realme وجهاز ثانٍ، تحقق من valid/expired/replayed OTP وParent waiting/success وChild permissions handoff.

## Issues List (Consolidated)

### Issue 1: تقييد محاولات OTP غير الصحيحة غير مطبق
- [x] FIXED
- Fix notes: أضيف `ChildDevicePairingAttempts` بمفتاح hash لمعرف الجهاز في `device_management_pairing_service.py`، مع نافذة عشر دقائق وثلاث محاولات واختبار `test_three_invalid_codes_lock_the_same_device_for_pairing_window`. نجح أمر pairing المحدد: `13 passed`.
- Severity: HIGH
- Depends on: none
- Affected tasks: T001, T002, T008.
- Evidence: `DeviceManagementPairingService.claim()` يستدعي `_find_pairing()` ثم يعيد `pairing_invalid` مباشرة عند عدم وجود hash؛ لا يسجل محاولة حسب `device_id`.
- Root cause analysis: لا يمكن ربط رمز خاطئ بسجل pairing محدد لأن Client يرسل الرمز فقط، لذا يبقى `attempt_count` في pairing غير قابل للاستخدام لمسار التخمين الخاطئ.
- Proposed solution: أضف collection قصيرة العمر لمحاولات الجهاز، مفتاحها `device_id`، عدّاد/expiry بعشر دقائق، وارفض بعد ثلاث محاولات خاطئة. امسح أو أعد ضبط السجل بعد claim ناجح، مع إبقاء transaction Claim الحالية للأكواد الصحيحة.
- Test plan: أضف اختباراً يثبت أن ثلاثة رموز مجهولة تسجل وأن المطالبة التالية تعيد `pairing_attempts_exhausted`، ثم شغّل `set JWT_SECRET=device-management-route-test-secret && python -m pytest -q`.
- Notes / tradeoffs: الحد حسب الجهاز يحقق حماية عملية من التخمين من دون كشف `pairing_id` داخل UI الطفل.

### Issue 2: لا توجد اختبارات JVM مخصصة لتفاعل Parent/Child الجديد
- [x] FIXED
- Fix notes: أضيف `PairingOtpDisplay` واختباره في Parent لحساب الانتهاء وتنسيق الرمز، وأضيف `OtpInputValidator` واختباره في Child لتصفية النص الملصوق والتحقق من ست خانات. نجح Parent وChild في `testDebugUnitTest assembleDebug` بعد الإضافة.
- Severity: MED
- Depends on: Issue 1
- Affected tasks: T007, T008.
- Evidence: لا توجد ملفات اختبار جديدة في `app_father/.../src/test` أو `app_child/.../src/test` للـ OTP flow رغم إضافة منطق countdown/polling/input.
- Root cause analysis: التنفيذ بقي داخل Fragments، ما يجعل سلوك الزمن/المدخلات أصعب في اختبار JVM مباشرة.
- Proposed solution: استخرج helper نقي صغير لكل تطبيق: Parent لحساب expiry/قرار الحالة وChild للتحقق من ست خانات؛ استخدمه من Fragment وأضف اختبارات سيناريو سلوكية غير مكررة.
- Test plan: شغّل Parent وChild `testDebugUnitTest assembleDebug --no-daemon --console=plain` بعد إضافة الاختبارات.
- Notes / tradeoffs: لا يختبر هذا rendering، الذي يبقى ضمن اختبار Realme المتبقي.

## Fix Plan (Ordered)

1) Issue 1: تقييد محاولات OTP غير الصحيحة غير مطبق — أضف عداداً قصير العمر حسب الجهاز واختبر الاستنفاد.
2) Issue 2: لا توجد اختبارات JVM مخصصة لتفاعل Parent/Child الجديد — استخرج helpers نقية وأضف اختباراتها بعد إصلاح الأمان.

## Handoff to Coding Model (Copy/Paste)

- Files to edit/create: `device_management_pairing_service.py` و`test_device_management_phase3.py` أولاً؛ ثم helpers واختبارات JVM تحت Parent وChild.
- Exact behavior changes: لا تسمح بتخمين OTP غير محدود؛ حافظ على error states العربية وتدفقات الواجهة الحالية.
- Edge cases: expiry، replay، جهاز فعال سابقاً، ومطالبة status لا تعيد secret.
- Tests to add/update: استنفاد المحاولات، helper Parent للوقت/الحالة، helper Child لصحة الكود.
- Suggested commit breakdown: Backend security ثم Android helpers/tests ثم feature specs/review.
