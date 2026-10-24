package com.narendar.hub

import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val LocalAppLanguage = compositionLocalOf { "English" }

private val ur = mapOf(
 "Choose class" to "اپنی جماعت منتخب کریں","Class 8 includes Class 1–8 learning." to "جماعت 8 میں جماعت 1 سے 8 تک کی تعلیم شامل ہے۔",
 "🌎 All Classes" to "🌎 تمام جماعتیں","Learn • Practice • Master" to "سیکھیں • مشق کریں • مہارت حاصل کریں",
 "Welcome back 👋" to "خوش آمدید 👋","All Classes" to "تمام جماعتیں","Your learning journey" to "آپ کا تعلیمی سفر",
 "Explore" to "دریافت کریں","Quiz Arena" to "کوئز میدان","Practice MCQs • Easy / Medium / Hard" to "MCQs کی مشق • آسان / درمیانہ / مشکل",
 "Study Library" to "مطالعے کی لائبریری","Subjects + explanations" to "مضامین اور وضاحتیں","Questions" to "سوالات","Notes" to "نوٹس","Tasks" to "کام",
 "Revision Hub" to "دہرائی مرکز","Notes + planner" to "نوٹس اور منصوبہ بندی","Stats + Settings" to "اعداد و شمار اور ترتیبات",
 "Profile & Stats" to "پروفائل اور اعداد و شمار","Dark theme" to "ڈارک تھیم","CHANGE CLASS" to "جماعت تبدیل کریں",
 "Choose language" to "زبان منتخب کریں","App language" to "ایپ کی زبان","English" to "انگریزی","Urdu" to "اردو","Sindhi" to "سنڌي",
 "Study → revise → practice → test" to "مطالعہ → دہرائی → مشق → ٹیسٹ","MCQ Study Library" to "MCQ مطالعہ لائبریری",
 "Every test question has a unique ID and an explanation." to "ہر سوال کی منفرد شناخت اور وضاحت ہے۔","START PRACTICE" to "مشق شروع کریں",
 "Question class range" to "جماعتوں کی حد","Question language" to "سوال کی زبان","Show Correct Answer" to "درست جواب دکھائیں",
 "🔊 Read question + all options" to "🔊 سوال اور تمام اختیارات پڑھیں","🔊 Read AI explanation" to "🔊 AI کی وضاحت پڑھیں",
 "✨ Explain this question with AI" to "✨ AI سے سوال سمجھیں","✨ Why is my answer right or wrong?" to "✨ میرا جواب درست یا غلط کیوں ہے؟",
 "✨ Why is my answer wrong?" to "✨ میرا جواب غلط کیوں ہے؟","✨ Explain why this is correct" to "✨ یہ جواب درست کیوں ہے؟",
 "Ask a study question" to "تعلیمی سوال پوچھیں","ASK STUDYPILOT" to "اسٹڈی پائلٹ سے پوچھیں",
 "Offline preview • typo-tolerant • no API key" to "آف لائن پیش منظر • املا کی غلطیاں سمجھتا ہے • API key نہیں",
 "This first version answers from a small curated knowledge set. The separately trained Transformer is experimental and is not connected to this chat yet." to "یہ ابتدائی ورژن محدود تعلیمی معلومات سے جواب دیتا ہے۔ تجرباتی Transformer ابھی اس چیٹ سے منسلک نہیں۔",
 "Formula Cards" to "فارمولوں کے کارڈ","Rectangle area" to "مستطیل کا رقبہ","Rectangle perimeter" to "مستطیل کا محیط",
 "Speed" to "رفتار","Density" to "کثافت","Ohm’s law" to "اوہم کا قانون","Circle circumference" to "دائرے کا محیط","Average" to "اوسط",
 "Search the web" to "ویب پر تلاش کریں","Searching trusted sources…" to "معتبر ذرائع میں تلاش جاری ہے…",
 "No trusted summary found. Open Google results to check the question." to "معتبر خلاصہ نہیں ملا۔ سوال دیکھنے کے لیے گوگل نتائج کھولیں۔"
)
private val sd = mapOf(
 "Choose class" to "پنهنجو ڪلاس چونڊيو","Class 8 includes Class 1–8 learning." to "ڪلاس 8 ۾ ڪلاس 1 کان 8 تائين پڙهائي شامل آهي.",
 "🌎 All Classes" to "🌎 سڀ ڪلاس","Learn • Practice • Master" to "سکو • مشق ڪريو • مهارت حاصل ڪريو",
 "Welcome back 👋" to "ڀلي ڪري آيا 👋","All Classes" to "سڀ ڪلاس","Your learning journey" to "توهان جو سکيا وارو سفر",
 "Explore" to "ڳوليو","Quiz Arena" to "ڪوئز ميدان","Practice MCQs • Easy / Medium / Hard" to "MCQ مشق • آسان / وچولو / ڏکيو",
 "Study Library" to "پڙهائي لائبريري","Subjects + explanations" to "مضمون ۽ وضاحتون","Questions" to "سوال","Notes" to "نوٽس","Tasks" to "ڪم",
 "Revision Hub" to "ورجاءُ مرڪز","Notes + planner" to "نوٽس ۽ رٿابندي","Stats + Settings" to "انگ اکر ۽ سيٽنگون",
 "Profile & Stats" to "پروفائل ۽ انگ اکر","Dark theme" to "ڊارڪ ٿيم","CHANGE CLASS" to "ڪلاس تبديل ڪريو",
 "Choose language" to "ٻولي چونڊيو","App language" to "ايپ جي ٻولي","English" to "انگريزي","Urdu" to "اردو","Sindhi" to "سنڌي",
 "Study → revise → practice → test" to "پڙهو → ورجايو → مشق ڪريو → ٽيسٽ","MCQ Study Library" to "MCQ پڙهائي لائبريري",
 "Every test question has a unique ID and an explanation." to "هر سوال جي الڳ سڃاڻپ ۽ وضاحت آهي.","START PRACTICE" to "مشق شروع ڪريو",
 "Question class range" to "ڪلاسن جي حد","Question language" to "سوال جي ٻولي","Show Correct Answer" to "صحيح جواب ڏيکاريو",
 "🔊 Read question + all options" to "🔊 سوال ۽ سڀ اختيار پڙهو","🔊 Read AI explanation" to "🔊 AI وضاحت پڙهو",
 "✨ Explain this question with AI" to "✨ AI سان سوال سمجهو","✨ Why is my answer right or wrong?" to "✨ منهنجو جواب صحيح يا غلط ڇو آهي؟",
 "✨ Why is my answer wrong?" to "✨ منهنجو جواب غلط ڇو آهي؟","✨ Explain why this is correct" to "✨ هي جواب صحيح ڇو آهي؟",
 "Ask a study question" to "پڙهائيءَ جو سوال پڇو","ASK STUDYPILOT" to "اسٽڊي پائلٽ کان پڇو",
 "Offline preview • typo-tolerant • no API key" to "آف لائن ڏيک • املا جون غلطيون سمجهي ٿو • API key ناهي",
 "Search the web" to "ويب تي ڳوليو","Searching trusted sources…" to "معتبر ذريعن ۾ ڳولا جاري آهي…",
 "No trusted summary found. Open Google results to check the question." to "معتبر خلاصو نه مليو. سوال ڏسڻ لاءِ گوگل نتيجا کوليو."
)

fun translateAppText(text: String, language: String): String {
    if (language == "English") return text
    val table = if (language == "Urdu") ur else sd
    table[text]?.let { return it }
    Regex("^Class (\\d+)–(\\d+)$").find(text)?.let { m ->
        return if (language == "Urdu") "جماعت ${m.groupValues[1]} تا ${m.groupValues[2]}" else "ڪلاس ${m.groupValues[1]} کان ${m.groupValues[2]}"
    }
    Regex("^Class (\\d+)$").find(text)?.let { m ->
        return if (language == "Urdu") "جماعت ${m.groupValues[1]}" else "ڪلاس ${m.groupValues[1]}"
    }
    if (text.startsWith("Class ")) return if (language == "Urdu") text.replace("Class ", "جماعت ") else text.replace("Class ", "ڪلاس ")
    if (text.startsWith("LEVEL ")) return text
    if (text.startsWith("Correct answer:")) return if (language == "Urdu") "درست جواب: " + text.removePrefix("Correct answer:").trim() else "صحيح جواب: " + text.removePrefix("Correct answer:").trim()
    return text
}

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val language = androidx.compose.runtime.staticCompositionLocalOf { "English" }
    MaterialText(
        text = translateAppText(text, LocalAppLanguage.current),
        modifier = modifier, color = color, fontSize = fontSize, fontStyle = fontStyle,
        fontWeight = fontWeight, fontFamily = fontFamily, letterSpacing = letterSpacing,
        textDecoration = textDecoration, textAlign = textAlign, lineHeight = lineHeight,
        overflow = overflow, softWrap = softWrap, maxLines = maxLines, minLines = minLines,
        onTextLayout = onTextLayout, style = style
    )
}

@Composable
fun LanguagePicker(language: String, onLanguageChange: (String) -> Unit) {
    androidx.compose.foundation.layout.Column {
        AppText("App language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.layout.Row {
            listOf("English", "Urdu", "Sindhi").forEach { option ->
                FilterChip(
                    selected = language == option,
                    onClick = { onLanguageChange(option) },
                    label = { MaterialText(when (option) { "Urdu" -> "اردو"; "Sindhi" -> "سنڌي"; else -> "English" }) },
                    modifier = Modifier
                )
            }
        }
    }
}
