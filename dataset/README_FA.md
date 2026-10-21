# دیتاست صدای Melova

دیتاست آواز فارسی برای آموزش مدل‌هایی که چهار چیز را ارزیابی می‌کنند: **کوک** (فالش)، **گرفتگی صدا**، **قدرت صدا** و **حس خواندن**.

| بعد | منبع برچسب |
|---|---|
| کوک | **مصنوعی** (خطای کنترل‌شده با برچسب دقیق فریم‌به‌فریم) + اجرای واقعی هنرجوها |
| گرفتگی | ارزیابی انسانی با **GRBAS** + نمونه‌ی مصنوعی کمکی (نفس‌دار و زبر) |
| قدرت | ارزیابی انسانی (۱ تا ۷) + شاخص‌های آکوستیک |
| حس | فقط **مقایسه‌ی جفتی انسانی** → امتیاز Bradley–Terry |

## ساختار

```
dataset/
├── protocol/
│   ├── recording_protocol_FA.md   ← چطور ضبط کنیم
│   ├── consent_form_FA.md         ← فرم رضایت (پیش‌نویس، وکیل بررسی کند)
│   └── rating_guide_FA.md         ← راهنمای اساتید ارزیاب
├── metadata/
│   ├── singers.csv                ← خواننده‌ها (بدون اسم)
│   ├── singers_private.csv        ← اسم و تماس — وارد git نمی‌شود
│   ├── raters.csv                 ← ارزیاب‌ها
│   └── prompts.csv                ← مجموعه‌ی ثابت تمرین‌ها و جمله‌ها
├── melova_dataset/                ← کد
├── tests/
└── data/                          ← همه‌ی صداها؛ وارد git نمی‌شود
    ├── raw/<singer>/<session>/<prompt>__<condition>__<take>.wav
    ├── clips/                     ← خروجی ingest
    ├── manifest.jsonl
    ├── features.csv
    ├── synthetic/ + synthetic_manifest.jsonl
    └── ratings/<batch>/
```

## نصب (macOS)

```bash
cd dataset
python3.11 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
brew install ffmpeg          # برای فایل‌های m4a آیفون
python -m pytest tests -q    # باید ۵ تست پاس شود
```

## گردش کار

```bash
# ۱. ضبط‌ها را در data/raw بگذارید و singers.csv را پر کنید، بعد:
python -m melova_dataset.ingest --root .
#    → فایل‌های تمیز در data/clips + manifest.jsonl
#    → فایل‌های با نام اشتباه یا بدون رضایت رد می‌شوند و دلیلش چاپ می‌شود
#    → فایل‌های clip شده، پرنویز یا کم‌صدا status=review می‌گیرند

# ۲. شاخص‌های آکوستیک
python -m melova_dataset.features --root .
#    → data/features.csv (jitter, shimmer, HNR, CPP, SPR, دینامیک، ویبراتو، ...)

# ۳. خطای مصنوعی (فقط از اجرای خواننده‌های advanced/professional)
python -m melova_dataset.synth_errors --root . --variants 6
#    → برای هر کلیپ: clean.wav + v01..v06.wav + برچسب JSON فریم‌به‌فریم

# ۴. ارزیابی انسانی
python -m melova_dataset.ratings make --root . --batch round1
#    → یک CSV برای هر ارزیاب در data/ratings/round1/
#    ... اساتید فایل‌ها را پر می‌کنند ...
python -m melova_dataset.ratings analyze --root . --batch round1
#    → گزارش توافق + labels_quality.csv + labels_expression.csv
```

برای امتحان سریع روی یک فایل:

```bash
python -m melova_dataset.features --file ../backend/assets/teacher_vocal_demo.wav
python -m melova_dataset.synth_errors --file ../backend/assets/teacher_vocal_demo.wav --out /tmp/demo --variants 4
```

## نکته‌های مهم

- **مدل را روی `clean.wav` در برابر `vNN.wav` آموزش دهید، نه روی فایل اصلی.** در غیر این صورت مدل یاد می‌گیرد «صدای vocoder = خطا».
- خطای مصنوعی فقط روی اجرای خواننده‌های خوب ساخته می‌شود. اگر اجرای اصلی خودش فالش باشد، برچسب «بدون خطا» دروغ می‌شود.
- **Split بر اساس خواننده است، نه فایل.** هیچ خواننده‌ای نباید هم در train باشد و هم در test.
- **ارزیابی نهایی فقط روی داده‌ی واقعی** انجام می‌شود. داده‌ی مصنوعی فقط برای آموزش است.
- jitter، shimmer و HNR روی واکه‌های کشیده (V01–V03) معتبرترند. در جمله‌ها ویبراتو و تحریر آن‌ها را بالا می‌برد. ویبراتو و تحریر در `vibrato_*` هم دیده می‌شوند.
- فشار (Strain) و حس قابل شبیه‌سازی نیستند و فقط از ارزیابی انسانی می‌آیند.
- اپ ادعای **تشخیص پزشکی** نکند. گرفتگی فقط یعنی «استراحت کن؛ اگر ادامه داشت به پزشک گوش‌وحلق‌وبینی مراجعه کن».

## نقشه‌ی راه

| فاز | هدف | خروجی |
|---|---|---|
| ۰ — پایلوت | ضبط خودتان + ۵ نفر، یک دور ارزیابی کوچک | اطمینان از اینکه پروتکل و ابزارها کار می‌کنند |
| ۱ | ۵۰ تا ۱۰۰ خواننده، ۲ تا ۳ ارزیاب | حدود ۲۰۰۰ تا ۳۰۰۰ کلیپ + برچسب گرفتگی/قدرت/حس |
| ۲ | مدل گرفتگی و قدرت (ساده‌ترین‌ها) | ویژگی‌های WavLM/MERT + سر رگرسیون |
| ۳ | مدل کوک روی مصنوعی + تست روی واقعی | مقایسه با موتور فعلی Melova |
| ۴ | مدل حس (رتبه‌بندی) | |
