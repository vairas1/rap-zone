# نشر Rap Zone API على Render (دائم — يشتغل من أي جهاز / أي شبكة)

## اللي هيتعمل
- الخلفية هتبقى على عنوان عمومي دائم: `https://rapzone-api.onrender.com`
- التطبيق (APK) بيستطيع يوصل ليها من **أي** موبايل على **أي** شبكة — مش لازم جهازك يكون شغّال.
- البيانات (قاعدة البيانات + الملفات المرفوعة) محفوظة على **Disks** (1 GB) فبتفضل بعد الـ redeploy.

## خطوات النشر (عندك على الجهاز)

### 1) ارفع المشروع على GitHub
```bash
cd "C:\Users\eslam\Downloads\Rap Zone Open Code"
git init
git add .
git commit -m "Rap Zone: Android app + NestJS API"
# اعمل حساب على github.com ولو فيه حساب (مجاني)
# اعمل repo جديد باسم rap-zone (Public أو Private)
git branch -M main
git remote add origin https://github.com/USERNAME/rap-zone.git
git push -u origin main
```
> ملف `.gitignore` موجود وبيخفي الملفات الضخمة (node_modules، build، dev.db).

### 2) اربطه بـ Render
1. روح [render.com](https://render.com) → **New** → **Blueprint**
2. اختار الـ repo بتاعك (`rap-zone`) — هيقرا ملف `server/render.yaml` تلقائياً
3. هتظهرلك خدمة **rapzone-api** → **Create**
4. انتظر الدقايق لحد ما الـ deploy يخلص

### 3) جرّبه
- بعد النشر افتح: `https://rapzone-api.onrender.com/artists`
- لو طلع JSON فيه فنانين → النجاح ✅

### 4) حط العنوان في التطبيق
في **أي** موبايل على **أي** شبكة:
1. افتح **Rap Zone** → **الإعدادات** (Settings)
2. قسم **رابط الخادم (API)** → امسح `192.168.1.5` واكتب:
   ```
   https://rapzone-api.onrender.com
   ```
3. اضغط **حفظ وتطبيق** → الشاشة الرئيسية هتبدأ تعمل فوراً.

> ملاحظة: عنوان Render بالـ default هو `https://rapzone-api.onrender.com`.
> لو الـ blueprint عمل اسم تاني (e.g. `rapzone-api-xxxx`)، انسخه من صفحة الـ service في Render.

## بيانات الدخول (لو هتستخدم لوحة الأدمن)
| الحقل | القيمة |
|-------|--------|
| البريد | `admin@rapzone.app` |
| كلمة المرور | `Admin@123` |

> **غيّرهم!** في Render → الخدمة → **Environment** → عدّل `ADMIN_EMAIL` / `ADMIN_PASSWORD` → هوّيد.

## ملاحظات مهمة
- **الخطوة 3 (حفظ رابط الخادم)** لازم تسويها من الموبايل بعد ما النشر يخلص — لأن عنوان الـ URL مش معروف إلا بعد النشر.
- **نوع المحتوى**: الأغاني في الـ demo بتروح على روابط صوت عمومية (SoundHelix) عشان تشتغل من أي مكان — مش محتاجة الملفات تكون على السيرفر.
- **الرفع (Upload)**: الملفات المرفوعة (موسيقى/صور) بتتحفظ على الـ disk (1GB) وبتفضل بعد الـ redeploy.
- **Render Free**: ممكن يهجع (sleep) بعد فترة خمول — أول طلب هيقومّو خلال ~15 ثانية.
