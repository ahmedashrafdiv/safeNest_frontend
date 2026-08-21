# Tasks: Layngo Location Places and Geofences

## Phase 1: Specification and Contracts
- [x] T001 إنشاء Spec Kit وتسجيل الحدود والافتراضات.
- [ ] T002 إضافة نماذج وأخطاء تطبيع للأماكن مع اختبار schema/legacy baseline.

## Phase 2: Backend Places and Transitions
- [ ] T003 توسيع zone CRUD إلى Place v2 وآلية version/migration.
- [ ] T004 إضافة child active-places وtransition event idempotency/ownership.
- [ ] T005 إنشاء Parent Alerts typed وربطها بالتفضيلات وFCM lifecycle.
- [ ] T006 إضافة اختبارات Backend للأماكن والانتقالات.

## Phase 3: Parent Overview and Management
- [ ] T007 إعادة بناء موقع ليان مع freshness/refresh/privacy وmap مبسط.
- [ ] T008 إعادة بناء أماكن ليان grouped مع cards/edit states والشعار المرفوع.

## Phase 4: Parent Place Form Flow
- [ ] T009 تنفيذ اختيار النوع والmarker/search مع حفظ state.
- [ ] T010 تنفيذ إعداد safe/attention/risk وedit/success.

## Phase 5: Child Geofence Handling
- [ ] T011 إضافة contracts ومزامنة الأماكن المصادق عليها في Child.
- [ ] T012 إضافة geofence registration/receiver/event uploader والصلاحيات/health.

## Phase 6: Alerts and Quality
- [ ] T013 إضافة عرض تنبيهات المكان في Parent Inbox.
- [ ] T014 إضافة اختبارات JVM وتشغيل Backend/Parent/Child builds.
- [ ] T015 تطبيق test-guard وspec-review وreview-fix وcode-review.

## Phase 7: Device Verification
- [ ] T016 توثيق أو تنفيذ اختبار Realme ثنائي الجهاز عند توفره.
