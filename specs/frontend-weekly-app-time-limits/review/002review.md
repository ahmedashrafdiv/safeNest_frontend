# Spec Review: frontend-weekly-app-time-limits
- Branch: `main`
- Spec resolved via: argument (`frontend-weekly-app-time-limits`)
- Resolution conflicts: `.specify/feature.json` يشير إلى ميزة متزامنة مختلفة؛ استُخدم مسار هذه المواصفة صراحةً ولم يُعدّل ملف الحالة المشترك.
- Review file: `002review.md`
- Detected commands: test=`app_father/SafeNest:gradlew.bat testDebugUnitTest` lint=`not configured` types=`app_father/SafeNest:gradlew.bat compileDebugKotlin`

## Summary

| البند | النتيجة |
|---|---|
| الحالة العامة | **PARTIAL**: المتطلبات البرمجية وUI المكتوب اكتملت؛ التحقق المرئي والتزامن end-to-end على Realme مؤجل لعدم وجود هاتف متصل. |
| المخاطر عالية الخطورة | لا توجد مشكلة كود مؤكدة مفتوحة. |
| اختبارات Parent | `testDebugUnitTest` و`assembleDebug` نجحا بعد تعديلات UI. |
| اختبارات Child | `testDebugUnitTest` و`assembleDebug` نجحا في baseline الحالي. |
| Type check | `compileDebugKotlin` للـ Parent نجح بعد آخر تعديل. |
| Lint | غير مهيأ في المشروع. |

## Task-by-task Verification

### Tasks FWTL-001–FWTL-004: إنفاذ Child حسب اليوم
- Status: **PASS (static/build)**.
- Evidence: تقارير المراجعة السابقة واختبار baseline الحالي يثبتان أن Child يحل حد اليوم من map أسبوعية مع fallback متوافق وsemantic `0/1440`.

### Tasks FWTL-005–FWTL-008: نموذج Parent والـ helper النقي
- Status: **PASS**.
- Evidence: `ApiModels.kt` و`AppControlStatus.kt` يحملان map الأسبوعية، و`AppControlStatusTest` يغطي tab status و97 قيمة وcopy Saturday.

### Tasks FWTL-009–FWTL-011: شاشة overview
- Status: **PASS (static/build)**.
- Evidence: `fragment_installed_apps.xml` و`InstalledAppsFragment.render()` يعرضان اختيار السياسة والتبويبات والملخصات وزر الحفظ. تم تدقيق أهداف اللمس لمساحة 48dp للنقاط الثلاث والـ tabs، والحالة نصية وليس لوناً فقط.

### Tasks FWTL-012–FWTL-014: قائمة الإجراءات والمحرر الأسبوعي inline
- Status: **PASS (static/build)**.
- Evidence: `showActionMenu()` يحتوي فقط سماح/تحديد وقت/حظر. أضيفت أيقونات vector موحدة وpopup surface مناسب بدلاً من اعتماد مرئي على النص أو emoji. `weeklyEditor()` يبقي 7 أيام داخل scroll ويستخدم `expandedPackage` لمحرر واحد، و`showTimeDropdown()` يعرض 97 قيمة كل 15 دقيقة في popup anchored. `saveWeeklyLimit()` يحتفظ بمسار scope/override القائم.

### Tasks FWTL-015–FWTL-016: الاختبارات والمراجعة
- Status: **PASS (static/build)**.
- Evidence: Parent وChild builds/tests نجحت في هذه الدورة؛ راجعت Test Guard ولم أضف اختبارات framework أو mocks غير مبررة؛ Code Review محدد لم ينتج finding بثقة 80% أو أعلى بعد تصحيح كل visible copy في هذا التدفق إلى العربية.

## Issues List (Consolidated)

### Issue 1: لا يوجد دليل فيزيائي حديث على Realme
- [ ] FIXED
- Severity: MED
- Depends on: none
- Affected tasks: FWTL-015.
- Evidence: `adb devices -l` أعاد قائمة فارغة في هذه الدورة؛ نجاح JVM/build لا يثبت rendering أو popup/scroll أو وصول policy إلى Child.
- Root cause analysis: Android UI وAccessibility/service sync يحتاجان runtime حقيقي ولا يمكن جعلهما PASS من اختبارات JVM.
- Proposed solution: عند توصيل Realme، ثبّت Parent/Child APKs الحالية عند الحاجة، ثم اختبر overview، allowlist/blocklist، قائمة YouTube، المحرر inline، dropdown الخميس، copy Saturday، حفظ وإعادة تحميل القاعدة، ثم التحقق من Child في اليوم ذي الحد المطبق.
- Test plan: `adb devices -l` ثم اختبار يدوي موثق؛ لا يوجد تعديل كود مطلوب قبلها.
- Notes / tradeoffs: لا ينبغي إضافة اختبار وهمي للـ PopupWindow أو ادعاء تحقق مرئي دون الهاتف.

## Fix Plan (Ordered)
1) Issue 1: نفّذ walkthrough حقيقي على Realme ووثق النتائج؛ لا يحتاج اعتماداً على إصلاح برمجي سابق.

## Handoff to Coding Model (Copy/Paste)
- Files to edit/create: لا يوجد إصلاح production مطلوب الآن.
- Exact behavior changes: لا تغيّر شكل policy أو map الأسبوعية؛ الإصلاحات الأخيرة تجميلية/تعريبية وتحافظ على عقد الحفظ.
- Edge cases: تحقق من أن تطبيق Parent لا يُحظر في وضع allowlist، وأن 00:00 و24:00 يظلان مفهوميْن في الـ UI وعلى Child.
- Tests to add/update: لا تضف test إلا إذا كشف walkthrough سلوكاً محدداً قابلاً لإعادة الإنتاج.
- Suggested commit breakdown: commit محلي scoped لملفات Parent UI وpolicy messages الجديدة فقط، بعد عزلها عن الأعمال المتزامنة.
