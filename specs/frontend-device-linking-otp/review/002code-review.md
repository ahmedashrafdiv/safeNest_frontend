# Code Review: Device Linking via OTP

## Scope

راجعت فقط ملفات OTP الجديدة أو المعدلة في Parent وChild وBackend. لم أبلغ عن التغييرات المتزامنة أو الملفات غير المرتبطة في المستودعين.

## Outcome

لم تظهر ملاحظات وظيفية مؤكدة بدرجة ثقة مرتفعة بعد تنفيذ إصلاحات المراجعة. التحقق يشمل حماية endpoint claim العام من إرسال token قديم، وOTP سداسي مخزّن كـ hash، وحالة Parent لا تعيد secret، وclaim ذري في Firestore مع fallback مخصص لبيئة FakeDB الاختبارية فقط، وإلغاء polling عند تدمير Parent Fragment.

| مجال | الدليل |
|---|---|
| استعمال واحد | `DeviceManagementPairingService.claim()` يرفض `status != pending` ثم يحول الحالة إلى `claimed` ضمن transaction في Firestore. |
| التخمين | `ChildDevicePairingAttempts` يسجل hash لمعرف الجهاز فقط ويغلق المطالبات بعد ثلاث محاولات خاطئة خلال نافذة عشر دقائق. |
| عدم كشف السر | `get_pairing_status()` يعيد status/expiry/attempts فقط، واختبار Parent status يثبت غياب `pairing_code`. |
| مصادقة Child | `ApiClient.UNAUTHENTICATED_PATHS` يشمل claim العام فلا يرسل token سابقاً قبل الربط. |
| lifecycle | `DeviceLinkingFragment.onDestroyView()` يلغي countdown وpolling. |

## Verification Evidence

نجح `python -m pytest -q` في Backend بـ **273 اختباراً**. كما نجحت أوامر `testDebugUnitTest assembleDebug --no-daemon --console=plain` لتطبيقَي Parent وChild بعد آخر تعديل.

## Deferred Evidence

اختبار لمس الست خلايا والتنقل الفعلي بين جهازَي Parent/Child وحالة Waiting/Success ما زال يحتاج هاتفين Android متصلين. هذا قيد تحقق فيزيائي، وليس عيباً مثبتاً في الكود أو البناء الحالي.
