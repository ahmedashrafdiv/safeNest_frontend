# Feature Specification: Parent Alerts and Requests Inbox

## Purpose

تطوير `ParentInboxFragment` الحالي في تطبيق Layngo Parent ليصبح شاشة عربية RTL بعنوان **«التنبيهات والطلبات»**. تعرض الشاشة الطلبات المعلقة التي تحتاج قرار الأب أولاً، ثم تنبيهات اليوم الهادئة، مع قرارات داخل البطاقة وبدون بناء شاشة مراقبة متطفلة أو مسار بيانات جديد موازٍ.

## Functional Requirements

### Data and privacy

- FR-001: تعتمد الشاشة على `ParentInboxRepository` الحالي الذي يجمع pending access requests وunresolved alerts، ولا تنشئ polling أو backend endpoint جديداً لهذه الدورة.
- FR-002: يحول mapper نقي DTOs الحالية إلى نموذج عرض عربي، مع fallback آمن للنوع غير المعروف أو الحقول الناقصة.
- FR-003: لا تعرض الشاشة إحداثيات دقيقة أو خريطة أو تاريخ حركة مستمر أو تفاصيل غير لازمة للقرار.

### Decision section

- FR-004: يظهر قسم **«يحتاج قرارك»** أولاً، مع ملخص Mint وعدد الطلبات pending.
- FR-005: يظهر طلب الوقت الإضافي بزر Teal **«موافقة»** وزر outline هادئ **«إبقاء الحد»**؛ القرار يرسل approve/reject endpoint الحالي فقط.
- FR-006: يظهر طلب تطبيق جديد بزر Teal **«سماح»** وزر outline **«ليس الآن»**؛ لا تظهر عقوبة أو Coral كخطأ عند الرفض.
- FR-007: تعطل أزرار البطاقة خلال الطلب، وتظهر نتيجة عربية داخل البطاقة أو رسالة عربية ثم تحديث آمن للقائمة. لا يُعرض undo فعال دون endpoint يثبت القدرة على إلغاء القرار.

### Alerts section

- FR-008: يظهر قسم **«تنبيهات اليوم»** بعد الطلبات، ويصنف التنبيهات المعروفة: محاولة تطبيق محظور، مغادرة مكان آمن، وصول آمن. كل تنبيه يبقى هادئاً ويستخدم Coral كaccent صغير فقط للمحاولة المحظورة.
- FR-009: يقدم تنبيه التطبيق المحظور إجراءً واحداً لمراجعة قاعدة التطبيق، ويقدم تنبيه الموقع إجراءً واحداً لعرض الموقع عند توافر السياق. وصول آمن لا يحتاج إجراءً.
- FR-010: action **«تحديد كمقروء»** يحل التنبيهات غير المقروءة فقط، مع progress/failure feedback؛ لا يغير طلبات pending.

### UI and accessibility

- FR-011: كل copy مرئية عربية RTL؛ الحالة لا تعتمد على اللون وحده، وكل هدف لمس لا يقل عن 48dp.
- FR-012: تستخدم الشاشة Ivory/Navy/Teal/Mint/Coral وهوية Layngo، وأيقونات vector موحدة بلا emoji، وحالات loading/partial error/empty/end-of-list.
- FR-013: يحتفظ المسار الحالي بدخول inbox بعد login، وبالعودة الصريحة إلى الرئيسية من `MainActivity.showHomeFromInbox()`.

## Acceptance Criteria

| Scenario | Expected result |
|---|---|
| Pending extra-time request | يظهر أولاً بمدة مفهومة وقرارين فقط؛ لا يمنح التطبيق وقتاً محلياً قبل نجاح approve response. |
| Pending app access request | يظهر باسم تطبيق/نطاق آمن وقرارين محترمين؛ reject لا يستخدم نبرة عقابية. |
| Blocked-app alert | يعرض رسالة هادئة وstatus نصياً وCoral accent صغيراً وزر مراجعة واحداً فقط. |
| Safe-place alert | يعرض وصول/مغادرة من دون خريطة كبيرة أو coordinates؛ زر location يظهر فقط حيث له معنى. |
| Unknown data | لا ينهار ولا يفبرك اسم تطبيق/مكان؛ يعرض بطاقة عامة عربية. |
| Decision in flight | أزرار البطاقة تتعطل وتظهر حالة progress؛ النتيجة تحدث القائمة بعد server response. |
| Mark all read | يطبق على unresolved alerts فقط، ويعرض failure feedback إن فشل عنصر، ولا يمس pending requests. |
| Empty/partial failure | تعرض الشاشة state عربية مفهومة مع retry/continue دون ترك محتوى stale. |

## Clarifications

- [ASSUMED] يعتمد التعرف على أنواع alerts على `alertType` و`message` الحاليين؛ عند عدم كفاية الدليل، يستخدم العرض بطاقة عامة ولا يخمّن حدثاً محدداً.
- [ASSUMED] لا يدعم Backend undo لقرار approve/reject؛ لذلك لا يظهر زر تراجع قابل للتنفيذ في هذه الدورة.
- [ASSUMED] «تحديد كمقروء» يعني resolve للتنبيهات غير المحلولة عبر endpoint الحالي، ويعالج العناصر بالتتابع لتقديم نتيجة دقيقة.
- [ASSUMED] يبقى `ParentInboxFragment` startup inbox، ولا تستبدل هذه الدورة `NotificationsFragment` القديم كي لا تكسر وجهات تنبيهات قائمة خارج نطاق الطلبات.
- [ASSUMED] لا تعدل هذه الميزة ملفات IoT أو Child أو Backend؛ أي نقص حقيقي في backend سيظهر في المراجعة كـ blocker منفصل بدلاً من اختراعه داخل Parent.
