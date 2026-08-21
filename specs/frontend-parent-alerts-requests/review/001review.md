# Spec Review: frontend-parent-alerts-requests
- Branch: `main`
- Spec resolved via: argument (`frontend-parent-alerts-requests`)
- Resolution conflicts: `.specify/feature.json` يشير إلى عمل متزامن مختلف؛ استُخدم مسار المواصفة المحدد صراحةً ولم يُعدّل ملف حالة مشترك.
- Review file: `001review.md`
- Detected commands: test=`app_father/SafeNest:gradlew.bat testDebugUnitTest` lint=`not configured` types=`app_father/SafeNest:gradlew.bat compileDebugKotlin`

## Summary

| البند | النتيجة |
|---|---|
| الحالة العامة | **PARTIAL**: implementation البرمجي والاختبارات والبناء مكتملة؛ اختبار Android runtime على Realme مؤجل لعدم وجود جهاز متصل. |
| اختبارات Parent | `testDebugUnitTest` نجح، بما فيه `ParentInboxPresentationTest`. |
| بناء Debug | `assembleDebug` نجح بعد آخر تعديل. |
| Type check | `compileDebugKotlin` نجح ضمن build الأخير. |
| Lint | غير مهيأ في المشروع. |
| Test Guard | لا توجد مخالفة في `ParentInboxPresentationTest`: يختبر سلوك mapper، يستخدم DTOs حقيقية، ولا يستخدم mocks أو اختبارات framework. |
| Code Review | لا يوجد finding بثقة 80% أو أعلى في الملفات ضمن نطاق inbox بعد التحقق من backstack وقرارات الطلبات وتنظيف resolution state. |

## Task-by-task Verification

### PAR-001: Contract and scope baseline
- Spec requirement / acceptance criteria: استعمال ParentInbox الحالي دون مسار backend موازٍ.
- Implementation found: `MainActivity.showStartupInbox()`, `ParentInboxRepository.load()`, و`ParentInboxViewModel` تبقى مصدر routing والبيانات.
- Status: PASS.
- Evidence: `ParentInboxRepository` يجلب requests pending والتنبيهات غير المحلولة بالتوازي؛ Fragment يستعمل ViewModel الحالي ولا يتصل بـ API مباشرة.

### PAR-002: Parent baseline
- Spec requirement / acceptance criteria: Parent tests وDebug build ينجحان قبل التعديل.
- Implementation found: مرحلة baseline موثقة في `orchestrator-state.md`.
- Status: PASS.
- Evidence: `testDebugUnitTest assembleDebug` نجح قبل وبعد التغيير.

### PAR-003: Pure Arabic presentation layer
- Spec requirement / acceptance criteria: تحويل DTOs إلى عرض عربي آمن مع fallback.
- Implementation found: `app_father/SafeNest/app/src/main/java/com/example/safenest/util/ParentInboxPresentation.kt`.
- Status: PASS.
- Evidence: يعرض extra time/access override كقرارات مختلفة، ويصنف blocked app/arrival/exit، ولا يعرض locations الدقيقة؛ unknown alert يبقى GENERAL.

### PAR-004: Mapper behavioral tests
- Spec requirement / acceptance criteria: تغطية extra-time/app-access/known alert/unknown/location privacy.
- Implementation found: `ParentInboxPresentationTest.kt`.
- Status: PASS.
- Evidence: خمس حالات سلوكية فعلية تمر في `testDebugUnitTest`، ولا توجد mocks.

### PAR-005: RTL decision inbox layout
- Spec requirement / acceptance criteria: app bar وملخص Mint وأقسام requests/alerts وحالات end/empty/error.
- Implementation found: `fragment_parent_inbox.xml`، drawables `ic_inbox_*` و`bg_inbox_*`.
- Status: PASS (build/static).
- Evidence: layout RTL، 24dp side padding، 48dp actions، icons vector، summaries/sections/end state، وجميع references نجحت في resource linking.

### PAR-006: Accessible card rendering
- Spec requirement / acceptance criteria: بطاقتان كحد أقصى للطلب وإجراء واحد للتنبيه، مع status نصي.
- Implementation found: `ParentInboxFragment.addRequestCard`, `addAlertCard`, وhelper buttons.
- Status: PASS (build/static).
- Evidence: الطلبات تستخدم actionين فقط، التنبيهات تستخدم action واحداً حسب النوع، وحالة `تم الحظر`/`وصول آمن` نصية بجانب اللون.

### PAR-007: Decision feedback
- Spec requirement / acceptance criteria: تعطيل أزرار القرار في-flight وتحديث آمن بعد response بلا undo غير مدعوم.
- Implementation found: `submitDecision` و`renderDecisionState`.
- Status: PASS (build/static).
- Evidence: الأزرار تتعطل ويظهر `جارٍ الحفظ...` ثم يعاد تحميل inbox فقط بعد Result.Success/Error من endpoint الحالي.

### PAR-008: Mark all read
- Spec requirement / acceptance criteria: حل التنبيهات فقط بالتتابع وبـ failure feedback.
- Implementation found: `markCurrentAlertsRead`, `resolveNextAlert`, `renderAlertResolutionState`, و`clearAlertResolutionState`.
- Status: PASS (build/static).
- Evidence: queue مبنية من unresolved alerts فقط، تعالج بالترتيب، وتتوقف عند failure، وتنظف StateFlow لمنع replay عند العودة للشاشة.

### PAR-009: Safe navigation
- Spec requirement / acceptance criteria: مراجعة القاعدة/الموقع عبر وجهات Parent القائمة عند توفر context فقط.
- Implementation found: `reviewAppRules` و`viewLocation`.
- Status: PASS (build/static).
- Evidence: deviceId الفارغ ينتج رسالة عربية ولا ينتقل؛ الجاهز يفتح `InstalledAppsFragment` أو `GpsFragment`، وكلاهما مسار Parent قائم.

### PAR-010: Parent verification commands
- Spec requirement / acceptance criteria: test وDebug build يمران.
- Status: PASS.
- Evidence: `testDebugUnitTest` و`assembleDebug` نجحا بعد آخر تعديل.

### PAR-011: Review gates
- Spec requirement / acceptance criteria: Test Guard وSpec Review وCode Review محددة النطاق.
- Status: PASS (static).
- Evidence: Test Guard طبق على الاختبار الجديد، وwhitespace scope check مرّ؛ لا يوجد lint configured.

### PAR-012: Realme walkthrough
- Spec requirement / acceptance criteria: إثبات سلوك UI/runtime دون تزوير.
- Status: PARTIAL.
- Evidence: لا يوجد Realme ظاهر في `adb devices -l` ضمن الدورات السابقة لهذه الجلسة.
- Problems: لا يمكن إثبات rendering، interactions، navigation، أو backend refresh على Android runtime من JVM وحده.
- Proposed fix: وصّل Realme وفعل USB debugging ثم نفذ walkthrough في قسم Fix Plan.
- Proposed tests: لا تضف test وهمياً؛ استخدم اختبار device يدوي موثق.

## Issues List (Consolidated)

### Issue 1: اختبار Realme غير منفذ
- [ ] FIXED
- Severity: MED
- Depends on: none
- Affected tasks: PAR-012.
- Evidence: `adb devices -l` أعاد قائمة فارغة في هذه الدورة؛ JVM/build لا يكفيان لتأكيد Material rendering أو network refresh.
- Root cause analysis: الاختبار الفيزيائي يتطلب Parent APK وخدمة backend وحالة requests/alerts فعلية على هاتف متصل.
- Proposed solution: بعد توصيل Realme، شغّل Parent، ادخل inbox، تحقق من ترتيب الأقسام، زر approve/reject، mark-all-read، fallback للأحداث، وزري review/location. أكمل بـ scenario request حقيقي إن كانت البيانات متاحة.
- Test plan: `adb devices -l` ثم التثبيت عند الحاجة وwalkthrough يدوي مسجل.
- Notes / tradeoffs: لا ينبغي محاكاة Popup/card rendering في JVM أو ادعاء نتيجة لم تُرَ على الجهاز.

## Fix Plan (Ordered)
1) Issue 1: نفّذ walkthrough على Realme ووثق PASS/FAIL لكل قرار وتنقل وحالة قائمة.

## Handoff to Coding Model (Copy/Paste)
- Files to edit/create: لا توجد حاجة لإصلاح production قبل اختبار الجهاز.
- Exact behavior changes: لا تغيّر عقد requests/alerts أو تضف undo بلا endpoint؛ احتفظ بـ ParentInbox كمصدر startup inbox.
- Edge cases: اختبر alertType غير معروف، deviceId فارغ، request فاشل، وpartial data failure.
- Tests to add/update: أضف اختباراً فقط إذا كشف Realme bug قابلاً لإعادة الإنتاج في mapper أو state handling.
- Suggested commit breakdown: commit محلي scoped لملفات `ParentInboxFragment`, `ParentInboxViewModel`, `ParentInboxPresentation`, test، layout، وdrawables ومواصفة هذه الميزة فقط بعد عزل الأعمال المتزامنة.
