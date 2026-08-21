# Implementation Plan: Device Linking via OTP

1. توحيد child-device pairing backend على OTP رقمي سداسي صالح لـ 600 ثانية، مع claim ذري وendpoint حالة مقيد بملكية الطفل.
2. استبدال Toast الإضافة في `MyDevicesFragment` بـ `DeviceLinkingFragment` ذي حالات Ready/Code/Waiting/Success وربطه بـ ViewModel وعداد expiry.
3. تحويل `PairingFragment` وlayout Child إلى خلايا OTP مستقلة واستبدال legacy link call بطلب claim الحديث.
4. إضافة اختبارات Backend وJVM ثم Test Guard وSpec Review وCode Review، وأخيراً توثيق اختبار جهازين عند توافر Realme.
