# Implementation Plan: Parent Alerts and Requests Inbox

## Architecture

سيبقى `ParentInboxRepository` المصدر الوحيد للبيانات، و`ParentInboxViewModel` مسؤولاً عن load/approve/reject/resolve. ستضاف طبقة pure باسم `ParentInboxPresentation` بين DTO والواجهة لخفض التشعب داخل Fragment وتسهيل اختبار النسخ العربية والحالات. ستعاد صياغة `ParentInboxFragment` و`fragment_parent_inbox.xml` فقط، مع vector drawables وstrings مخصصة عند الحاجة.

## Phases

1. **Specification and baseline:** تثبيت النطاق وتشغيل Parent tests/build قبل التعديل.
2. **Presentation model:** كتابة mapper واختبارات JVM للطلبات والتنبيهات والfallbacks.
3. **RTL inbox UI:** إعادة بناء layout والبطاقات والحالات وأيقونات Layngo.
4. **Interactions:** ربط approve/reject، mark-all-read، review/location navigation، وfeedback داخل البطاقة.
5. **Quality and device verification:** تشغيل tests/build/Spec Review/Test Guard/Code Review ثم اختبار Realme عند توفره.
