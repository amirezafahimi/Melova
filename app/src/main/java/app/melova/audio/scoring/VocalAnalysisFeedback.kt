package app.melova.audio.scoring

object VocalAnalysisFeedback {
    val analysisLimitations: List<String> = listOf(
        "تحلیل الگوریتمی است (RMVPE + DTW)؛ چت‌بات یا AI زبانی در کار نیست.",
        "اکتاو آزاد است — فقط شکل ملودی و pitch class با استاد مقایسه می‌شود.",
        "اختلاف کمتر از نصف ربع‌پرده عالی، تا ربع‌پرده قابل قبول؛ خطای بیشتر در نمره منعکس می‌شود.",
        "سرعت و ریتم آزاد؛ کیفیت صدا، ویبراتو و بیان در این نمره لحاظ نمی‌شود."
    )
}
