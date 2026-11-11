package app.melova.ui

import app.melova.ui.theme.MelovaOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.SectionHeader
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted

private class FaqGroup(val title: String, val entries: List<Pair<String, String>>)

private val FAQ = listOf(
    FaqGroup(
        "شروع کار",
        listOf(
            "ملووا چیست؟" to
                "ملووا برنامه‌ی تمرین آواز است. صدای استاد را می‌شنوید، همان جمله را می‌خوانید و برنامه اجرای شما را با اجرای استاد مقایسه می‌کند و می‌گوید کجا زیر یا بم خوانده‌اید.",
            "آیا برای استفاده باید وارد حساب شوم؟" to
                "برای تمرین نه. برای افزودن درس به درس‌های خودتان، خرید درس، فرستادن تمرین برای استاد و گفتگو با استاد باید وارد شوید.",
            "چطور درس پیدا کنم؟" to
                "از زبانه‌ی «جستجو» درس‌ها و استادها را پیدا کنید. درس‌هایی که اضافه کرده‌اید در صفحه‌ی اصلی، زیر «درس‌های من» می‌مانند.",
            "نام و عکسم را چطور عوض کنم؟" to
                "در پروفایل روی کارت نام خودتان بزنید."
        )
    ),
    FaqGroup(
        "درس‌ها و خرید",
        listOf(
            "چطور درس را به درس‌های خودم اضافه کنم؟" to
                "صفحه‌ی درس را باز کنید. اگر درس رایگان باشد «افزودن به درس‌های من» را بزنید و اگر پولی باشد یکی از دو نسخه را بخرید. بخش اول هر درس رایگان است.",
            "«درس» و «درس با استاد» چه فرقی دارند؟" to
                "هر دو همه‌ی بخش‌ها را برای همیشه باز می‌کنند و تمرین بی‌حد است. فرقشان در تعداد پیام به استاد است: «درس» چند پیام دارد و «درس با استاد» خیلی بیشتر. اگر «درس» را دارید، برای ارتقا فقط مابه‌التفاوت را می‌پردازید.",
            "پیام به استاد یعنی چه؟" to
                "هر اجرایی که برای استاد می‌فرستید یک پیام است. پیام‌ها تا ۳ ماه بعد از خرید می‌مانند. نوشتن در گفتگو و جواب‌های استاد پیام حساب نمی‌شود. اگر استاد تا ۳ روز جواب ندهد، پیام به شما برمی‌گردد.",
            "خرید درس چطور انجام می‌شود؟" to
                "در صفحه‌ی درس نسخه را انتخاب کنید و «خرید» را بزنید. بعد از پرداخت، درس فوراً باز می‌شود.",
            "اگر از حسابم خارج شوم درس‌هایم از بین می‌رود؟" to
                "نه. درس‌ها و پاسخ‌های استاد روی حسابتان می‌مانند و با ورود دوباره برمی‌گردند."
        )
    ),
    FaqGroup(
        "تمرین و تحلیل",
        listOf(
            "چطور بهترین نتیجه را بگیرم؟" to
                "در جای آرام و با هدفون تمرین کنید، اول نمونه‌ی استاد را خوب گوش دهید و جمله‌ها را عیناً و کامل بخوانید؛ بدون تکرار یا اصلاح وسط اجرا.",
            "آیا باید در همان اکتاو استاد بخوانم؟" to
                "نه. اکتاو آزاد است و بالاتر یا پایین‌تر هم قبول است. شکل ملودی و نام نت‌ها مقایسه می‌شود.",
            "آیا سرعتم باید دقیقاً مثل استاد باشد؟" to
                "نه، لازم نیست سرعت شما دقیقاً مثل استاد باشد. مهم این است که همان ملودی را کامل بخوانید.",
            "نمره چطور حساب می‌شود؟" to
                "نمره نشان می‌دهد نت‌های شما چقدر با ملودی استاد یکی است. اختلاف کمتر از نصف ربع‌پرده عالی و تا ربع‌پرده قابل قبول است و خطای بیشتر در نمره اثر می‌گذارد.",
            "چرا برنامه می‌گوید ملودی استاد دیده نشد؟" to
                "یعنی بخش زیادی از ملودی در اجرای شما پیدا نشد. معمولاً وقتی پیش می‌آید که فقط بخشی از جمله خوانده شده یا صدا ضعیف بوده است. یک بار دیگر کامل و نزدیک‌تر به میکروفون بخوانید.",
            "آیا اجرایم قبل از تحلیل فرستاده می‌شود؟" to
                "نه. بعد از ضبط اول صدایتان را گوش می‌دهید و اگر راضی بودید «ارسال برای تحلیل» را می‌زنید. می‌توانید دوباره هم ضبط کنید."
        )
    ),
    FaqGroup(
        "تحریر و تکنیک‌های آوازی",
        listOf(
            "تحریر چیست؟" to
                "تحریر چرخش سریع و پی‌درپی صدا میان چند نت روی یک مصوت است.",
            "ویبراتو چیست؟" to
                "ویبراتو لرزش منظم و کوچک زیر و بمی صدا حول یک نت است.",
            "گذر چیست؟" to
                "گذر حرکت پیوسته‌ی صدا از یک نت به نت دیگر است، بدون پرش میان آن‌ها.",
            "چطور تحریرهای استاد را از نزدیک ببینم؟" to
                "در صفحه‌ی تمرین «جزئیات اجرای استاد» را باز کنید. آنجا هر تحریر و ویبراتو را جدا و با سرعت کمتر می‌شنوید و می‌توانید موج را با دو انگشت بزرگ کنید."
        )
    ),
    FaqGroup(
        "استاد و گفتگو",
        listOf(
            "چطور تمرینم را برای استاد بفرستم؟" to
                "بعد از دیدن نتیجه، «ارسال برای استاد» را بزنید. اگر سؤالی دارید می‌توانید یادداشت هم بگذارید.",
            "پاسخ استاد را کجا ببینم؟" to
                "تمرین و پاسخ استاد در گفتگوی شما با استاد می‌ماند. دکمه‌ی گفتگوها بالای صفحه‌ی اصلی است و تعداد پیام‌های خوانده‌نشده را نشان می‌دهد.",
            "استاد شماره یا ایمیل مرا می‌بیند؟" to
                "نه. استاد شما را فقط با نامتان می‌بیند."
        )
    )
)

/** Common questions, opened from Profile. Each answer opens under its question. */
@Composable
fun FaqScreen(onBack: () -> Unit) {
    MelovaBackground {
        RtlContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentPadding = PagePadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { MelovaTopBar(title = "پرسش و پاسخ", onBack = onBack) }
                FAQ.forEach { group ->
                    item(key = group.title) { SectionHeader(title = group.title) }
                    item(key = group.title + "-entries") { FaqGroupCard(group) }
                }
            }
        }
    }
}

/** A topic's questions in one card, each answer opening under its own question. */
@Composable
private fun FaqGroupCard(group: FaqGroup) {
    MelovaSurfaceCard(contentPadding = 0.dp) {
        Column {
            group.entries.forEachIndexed { index, (question, answer) ->
                if (index > 0) HorizontalDivider(thickness = 1.dp, color = MelovaOutline)
                FaqRow(question, answer)
            }
        }
    }
}

@Composable
private fun FaqRow(question: String, answer: String) {
    var isOpen by rememberSaveable(question) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isOpen = !isOpen }
            .padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = question,
                style = MaterialTheme.typography.titleSmall,
                color = if (isOpen) MelovaAccentSoft else MelovaText,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isOpen) MelovaAccentSoft else MelovaTextMuted
            )
        }
        if (isOpen) {
            Text(text = answer, style = MaterialTheme.typography.bodySmall, color = MelovaTextMuted)
        }
    }
}
