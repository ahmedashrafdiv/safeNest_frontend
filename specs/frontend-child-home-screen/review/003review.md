# Spec Review: frontend-child-home-screen
- Branch: `main`
- Spec resolved via: argument (`frontend-child-home-screen`)
- Resolution conflicts: `.specify/feature.json` يشير إلى feature متزامن، ولذلك جرى استخدام مسار هذه المواصفة صراحةً ولم يُعدّل الملف.
- Review file: `003review.md`
- Detected commands: test=`JAVA_HOME=D:\Android\jdk\temurin-17 gradlew.bat testDebugUnitTest` lint=`not configured` types=`JAVA_HOME=D:\Android\jdk\temurin-17 gradlew.bat compileDebugKotlin`

## Summary
- Overall status: **PARTIAL** — الكود الخاص بالـ Home screen وmenu والتحقق الأبوي والتعليق وطلب الوقت الإضافي مكتمل ويبني ويجتاز اختبارات JVM؛ الاختبار الفيزيائي على Realme فقط ما زال غير منفذ لعدم وجود هاتف متصل.
- High-risk issues: لا توجد مشكلة برمجية مفتوحة بثقة عالية بعد المراجعة. لا يمكن الجزم بسلوك Android UI أو HOME role أو uninstall handoff دون جهاز فعلي.
- Missing tests / regression risk: الحوار وعمليات WorkManager/system intents تحتاج اختبار Realme؛ الاختبار JVM يغطي contract طلب الوقت الإضافي وليس network/UI framework.
- Test suite results: `testDebugUnitTest` نجح في آخر تشغيل مع `assembleDebug`.
- Lint results: not configured.
- Type check results: `compileDebugKotlin` نجح كجزء من build الأخير؛ ظهر تحذير Kotlin غير حاجب موجود في مسار قديم ولا يوقف التجميع.

## Quality Gates

| Gate | Result | Evidence |
|---|---|---|
| Test Guard | PASS | `ExtraTimeRequestDeciderTest` يستخدم DTO حقيقياً، لا mocks، ويغطي contract اليومي وduplicate ونجاح server id وحالات fail-closed. |
| Scoped Code Review | PASS | روجع `HomeFragment`, dialog/sheet, lifecycle coordinator, وworkers المعدلة. لم تبقَ finding بثقة 80% أو أعلى بعد إصلاح جدولة Screen Time وWebsite عند إعادة التفعيل. |
| JVM tests and Debug APK | PASS | `testDebugUnitTest` و`assembleDebug` نجحا في آخر تشغيل. |
| Realme / ADB | BLOCKED | `adb devices -l` أعاد قائمة فارغة؛ لم يجرِ تثبيت أو اختبار APK. |

## Task-by-task Verification

### Tasks T015–T020: Layngo Home screen
- Spec requirement / acceptance criteria: شاشة Layngo، حلقة ميزانية، greeting من profile، حالة suspended، وإبقاء worker registrations.
- Implementation found: `fragment_home.xml`, `BudgetRingView.kt`, و`HomeFragment.kt` عبر `loadSessionProfile`, `loadScreenTimeBudget`, `renderSuspendedState`, و`registerPolicyWorkers`.
- Status: **PASS (code/build)**.
- Evidence: `HomeFragment` يبقي fallback المحلي ويقرأ `screen-time-decision`، بينما layout يحمل header/ring/card/help/bottom navigation كما في المواصفة.

### Task T021: Build and confirm rendering
- Spec requirement / acceptance criteria: APK يبنى وتظهر الشاشة على جهاز.
- Implementation found: APK Debug ينتج من `app_child/SafeNest-Kids/app/build/outputs/apk/debug/app-debug.apk`.
- Status: **PARTIAL**.
- Evidence: `testDebugUnitTest assembleDebug --no-daemon --console=plain` نجح في آخر تشغيل.
- Problems: لا يوجد Realme متصل بـ ADB حالياً، لذلك لا يوجد دليل rendering فيزيائي.

### Tasks T022–T023: Menu and parent-password gate
- Spec requirement / acceptance criteria: sheet بخيارين حصراً، ثم dialog ببريد الأب المقفل وكلمة مرور وحالة loading/error.
- Implementation found: `ParentControlsSheet.kt`, `sheet_parent_controls.xml`, `ParentVerificationDialog.kt`, `dialog_parent_verification.xml`, و`HomeFragment.kt`.
- Status: **PASS (code/build)**.
- Evidence: sheet يرسل فقط `SIGN_OUT` أو `SUSPEND_PROTECTION`; dialog يستخدم البريد المخزن فقط، ويصنف النتائج عبر `ParentVerificationDecider`, ولا يخزن كلمة المرور أو يسجلها.

### Tasks T024–T029: Verified lifecycle controls
- Spec requirement / acceptance criteria: unpair/disable/re-enable/delete خلف تحقق الأب، cancel workers، عدم إعادة policy أثناء التعليق.
- Implementation found: `ProtectionLifecycleCoordinator.kt`, guards في `RuleSyncWorker`, `ScreenTimePolicySyncWorker`, `WebsitePolicySyncWorker`, `PhoneLocationPolicySyncWorker`, `ProtectedHomePolicySyncWorker`, `ContentBlurPolicySyncWorker`, و`ServiceWatchdogReceiver`.
- Status: **PASS (code/build)**.
- Evidence: `suspendProtection` يمسح policy ويوقف workers/services؛ جميع workers المذكورة تعود `Result.success()` عند `isProtectionSuspended()`؛ `signOut` يمسح pairing ويعيد إنشاء Activity؛ uninstall يزيل Device Admin حيث تسمح المنصة ثم يطلب system uninstall. دور HOME يحال إلى إعدادات النظام بدلاً من محاولة إسقاط role غير مدعوم.

### Tasks T030–T031: Extra time, help, and navigation
- Spec requirement / acceptance criteria: request daily extra time مع client request id مستقر، outcomes submitted/duplicate/failed، وhelp/navigation حقيقية.
- Implementation found: `ExtraTimeRequestDecider.kt`, `ExtraTimeRequestDeciderTest.kt`, و`HomeFragment.submitExtraTimeRequest`/`showHelp`.
- Status: **PASS (code/build)**.
- Evidence: payload ثابت على `extra_time` + `screen_time` + `daily` ومدة 30 دقيقة ضمن حدود Backend؛ request id يحفظ في prefs حتى لا ينشئ retry طلباً جديداً؛ لا يمنح التطبيق وقتاً محلياً قبل موافقة Parent.

### Tasks T032–T033: Final static verification and scope
- Spec requirement / acceptance criteria: اختبارات/build، وعدم تغيير Parent أو `AppBlockerAccessibilityService.kt`.
- Implementation found: بناء واختبارات ناجحة؛ الملفات المعدلة تقع تحت `app_child/SafeNest-Kids` ومواصفة Child فقط.
- Status: **PASS (static scope)**.
- Evidence: لا تغييرات تم إدخالها خلال هذه الدورة تحت `app_father/` أو `AppBlockerAccessibilityService.kt`.

## Issues List (Consolidated)

### Issue 1: الاختبار الفيزيائي على Realme ما زال غير موثق
- [ ] FIXED
- Severity: MED
- Depends on: none
- Affected tasks: T021.
- Evidence: البناء وJVM tests يثبتان compile/logic فقط؛ `adb devices` لم يوفّر هاتفاً أثناء الدورة.
- Root cause analysis: سلوك bottom sheet, password input, role/home settings, uninstall handoff, وrendering لا يمكن إثباته بدون Android runtime فعلي.
- Proposed solution: عند عودة Realme، ثبّت APK Debug، أعد تفعيل خدمات Accessibility بعد التثبيت، ثم اختبر الشاشة والـ menu والتحقق الخطأ/القفل والتعليق وإعادة التفعيل وextra time، وسجل النتيجة هنا.
- Test plan: `adb devices -l`; `adb install -r app-debug.apk`; اختبار يدوي بالترتيب أعلاه.
- Notes / tradeoffs: لا يوجد إصلاح برمجي آمن يبدل هذا الاختبار؛ لا ينبغي وضع علامة PASS للتجربة البصرية دون هاتف.

## Fix Plan (Ordered)
1) Issue 1: الاختبار الفيزيائي على Realme ما زال غير موثق — وصّل الهاتف وثبّت APK ثم نفّذ سيناريوهات T021.

## Handoff to Coding Model (Copy/Paste)
- Files to edit/create: لا يوجد إصلاح كود مطلوب قبل اختبار الهاتف.
- Exact behavior changes: لا شيء؛ APK الحالي اجتاز البناء والاختبارات.
- Edge cases: تحقّق من OFF/ON لخدمات Accessibility بعد التثبيت، وماذا يحدث لو كان HOME role نشطاً أو رفض Android إزالة Device Admin.
- Tests to add/update: لا تضف JVM tests تجريبية للـ Android framework؛ وثّق الاختبار الفيزيائي بنتيجة حقيقية.
- Suggested commit breakdown: commit scoped محلي واحد لمرحلات Child Home، منفصل عن Content Blur والعمل المتزامن.
