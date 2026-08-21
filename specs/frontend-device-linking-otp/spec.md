# Feature Specification: Device Linking via OTP

## Purpose

توفير تدفق قصير وآمن لربط جهاز Android جديد للطفل من تطبيق Layngo Parent إلى تطبيق Layngo Kids بواسطة رمز OTP رقمي مؤقت من ست خانات. يبدأ الأب من صفحة أجهزة الطفل وينتهي Child إلى إعداد أذونات الحماية بعد المطالبة الناجحة.

## Functional Requirements

- FR-001: ينشئ Parent رمزاً رقمياً من ست خانات، صالحاً لعشر دقائق، لطفل يملكه الأب المصادق عليه.
- FR-002: لا يخزن Backend الرمز كنص صريح، ويحده بثلاث محاولات افتراضية، ولا يقبل المطالبة به أكثر من مرة.
- FR-003: يدعم Backend endpoint أب موثقاً لاستعلام حالة pairing من دون إعادة الرمز أو token.
- FR-004: Parent يعرض Ready → Code → Waiting → Success وفق النصوص العربية المحددة، ويوقف الاستعلام عند مغادرة الواجهة.
- FR-005: Child يعرض ست خلايا إدخال رقمية LTR داخل واجهة RTL، مع انتقال تركيز تلقائي ودعم لصق الرمز.
- FR-006: نجاح Child يحفظ جلسة الجهاز الحالية وينتقل إلى `PermissionsFragment` القائم.
- FR-007: خطأ Child يعرض: «الرمز غير صحيح أو انتهت صلاحيته. اطلب رمزًا جديدًا من هاتف الوالد.» من دون مسح الإدخال تلقائياً.
- FR-008: لا تمس الميزة مسارات IoT/sensor pairing أو GPS أو سياسات الحماية الحالية، ولا تنفذ GitHub push أو deploy.

## Acceptance Criteria

| Scenario | Expected Result |
|---|---|
| Parent starts linking | تظهر شاشة جاهز للربط، ثم رمز Navy من ست خانات مع عداد عشر دقائق. |
| Child claims valid OTP | ينشأ/يفعل جهاز الطفل مرة واحدة، ويحفظ token، وينتقل إلى إعداد الحماية. |
| Parent confirms entry | يعرض Waiting ثم Success فقط عند حالة `claimed` الخادمية. |
| OTP reused or expired | لا ينتج جهازاً ثانياً؛ Child يعرض الرسالة العربية ومسار التصحيح. |
| User leaves waiting state | يتوقف polling ولا يستمر خارج lifecycle. |

## Clarifications

- [ASSUMED] يستخدم التدفق الجديد child-device management API الحديث، لأن واجهة الأجهزة الحالية متعددة الأجهزة بالفعل؛ يبقى `/api/devices/*` legacy بلا تعديل.
- [ASSUMED] لا توجد push event لحالة pairing حالياً؛ يستخدم Parent polling قصيراً ومقيداً بعمر `DeviceLinkingFragment`.
- [ASSUMED] رمز الست خانات ليس وسيلة مصادقة دائمة؛ هو secret قصير العمر، محمي بالتجزئة وحد المحاولات والاستعمال الأحادي.
- [ASSUMED] نجاح Child ينتقل إلى شاشة الأذونات الموجودة ولا يحتاج شاشة نجاح منفصلة، لأن Parent وحده يعرض تأكيد الربط المصور.
