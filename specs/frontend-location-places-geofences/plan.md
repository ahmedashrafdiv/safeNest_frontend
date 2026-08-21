# Implementation Plan: Layngo Location Places and Geofences

## Scope Boundary

يبقى external GPS/ThingSpeak وملفات IoT خارج النطاق. تنفذ الميزة Backend Place v2، واجهة Parent ومزامنة Child geofence، ثم استخدام Parent Alerts الموجود لتسليم التنبيهات. لا تُنشئ مسار حركة أو tracking history.

## Technical Design

| Layer | Planned Change |
|---|---|
| Backend | Place v2 schema/model/controller/routes، device-scoped active-place read، transition ingestion idempotent، typed Parent Alerts. |
| Parent | Location overview جديد، إدارة أماكن grouped، flow اختيار/marker/config/success، وtyped place-alert cards. |
| Child | places sync، geofence registration receiver، transition upload، truthful permission/health state. |
| Tests | Backend ownership/replay/preferences/legacy cases؛ Parent/Child pure JVM helpers؛ كامل builds ومراجعات. |

## Constraints

تنطبق radii 100/200/300 فقط. يسمح لكل place بحد انتقال واحد وفق نوعه إلا safe الذي يمكن أن يفعل دخولًا وخروجًا. لا تظهر coordinates في Parent. لا يتم إجراء deploy أو push.
