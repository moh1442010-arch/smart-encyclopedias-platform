#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ISSUER="$ROOT/tools/bakery-license-issuer.mjs"
KEY="${BAKERY_LICENSE_PRIVATE_KEY_FILE:-$HOME/al-mizan-signing/bakery-license-private.pem}"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js غير مثبت. ثبّته في Termux أولاً: pkg install nodejs"
  exit 1
fi
if [[ ! -f "$ISSUER" ]]; then
  echo "ملف إصدار الترخيص غير موجود داخل مجلد المشروع."
  exit 1
fi
if [[ ! -f "$KEY" ]]; then
  echo "لم أجد مفتاح الترخيص الخاص في المسار:"
  echo "$KEY"
  echo "احفظ ملف المفتاح الخاص في هذا المسار، ولا ترفعه إلى GitHub أو ترسله عبر واتساب."
  exit 1
fi

echo "إصدار رمز ترخيص الميزان للمخابز 2.6.17"
echo "يجب التأكد من وصول المبلغ وموافقة المالك قبل المتابعة."
read -r -p "رقم الطلب أو مرجعه الداخلي: " order_ref
[[ -n "$order_ref" ]] || { echo "رقم الطلب مطلوب."; exit 1; }

read -r -p "معرّف الجهاز كما يظهر داخل التطبيق: " device
[[ "$device" =~ ^[A-Za-z0-9-]{8,100}$ ]] || { echo "معرّف الجهاز غير صالح."; exit 1; }

echo "اختر الخطة:"
echo "1) شهري — 55,000 جنيه"
echo "2) سنوي — 550,000 جنيه"
echo "3) دائم — 1,050,000 جنيه"
read -r -p "رقم الخطة (1/2/3): " choice
case "$choice" in
  1) plan=monthly; amount=55000 ;;
  2) plan=annual; amount=550000 ;;
  3) plan=lifetime; amount=1050000 ;;
  *) echo "اختيار غير صالح."; exit 1 ;;
esac

echo
echo "الطلب: $order_ref"
echo "الجهاز: $device"
echo "الخطة: $plan"
echo "المبلغ الواجب التحقق منه: $amount جنيه سوداني"
read -r -p "هل تحققت من وصول المبلغ ووافقت بنفسك على إصدار الترخيص؟ اكتب YES للمتابعة: " approval
[[ "$approval" == "YES" ]] || { echo "أُلغي الإصدار. لم يُنشأ ترخيص."; exit 1; }

echo
echo "رمز التفعيل الخاص بالعميل:"
node "$ISSUER" --key "$KEY" --device "$device" --plan "$plan"
