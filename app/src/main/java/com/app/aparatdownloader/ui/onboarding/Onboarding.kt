package com.app.aparatdownloader.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.aparatdownloader.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Term(val value: String, val label: String)

private val grades = listOf("هفتم", "هشتم", "نهم", "دهم", "یازدهم", "دوازدهم")
private fun n(a: String, r: IntRange, sep: String = "") = r.map { Term("$a$sep$it", "$a$sep$it") }

// مقدار value همان رشته‌ای است که در پنل ادمین برای سطح دوره/کاربر وارد می‌شود
val LEVELS: Map<String, List<Term>> = linkedMapOf(
    "School" to grades.mapIndexed { i, g -> Term("School ${i + 7}", g) },
    "P" to n("P", 1..10),
    "B" to n("B", 1..5),
    "J" to n("J", 1..13),
    "Pre IELTS" to (1..10).map { Term("Pre IELTS $it", "Pre IELTS $it") },
    "C" to (1..20).map { Term("C $it", "$it") }
)

@Composable
private fun Header(title: String) = Text(
    title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
)

@Composable
private fun Row(text: String, onClick: () -> Unit) = Card(
    modifier = Modifier.fillMaxWidth().clickable { onClick() },
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Surface)
) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(18.dp)) }

/** صفحه اول: انتخاب سطح ← انتخاب ترم. خروجی: مقدار ترم (مثلاً "P3") */
@Composable
fun LevelTermScreen(onTermChosen: (String) -> Unit) {
    var level by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = level != null) { level = null }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Header(if (level == null) "سطح خود را انتخاب کنید" else "ترم خود را انتخاب کنید")
        Text(
            if (level == null) "سطح کلاس فعلی خود را بزنید؛ در مرحله بعد ترم دقیق را انتخاب می‌کنید."
            else "فقط ویدیوهای همین ترم برای شما باز می‌شود. اشتباه زدید؟ دکمه برگشت گوشی را بزنید.",
            fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (level == null) items(LEVELS.keys.toList()) { k -> Row(k) { level = k } }
            else items(LEVELS[level] ?: emptyList()) { t -> Row(t.label) { onTermChosen(t.value) } }
        }
    }
}

/** پرداخت جعلی (موقت)؛ بعداً به درگاه واقعی وصل می‌شود. onPay باید ثبت خرید را انجام دهد. */
@Composable
fun PaymentScreen(term: String, onPay: suspend () -> Boolean, onCancel: () -> Unit) {
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(1200) // شبیه‌سازی درگاه
            val ok = try { onPay() } catch (e: Exception) { false }
            if (!ok) error = "ثبت پرداخت انجام نشد؛ دوباره تلاش کنید"
            loading = false
        }
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("پرداخت ترم", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        Text("ترم انتخابی: $term", fontSize = 15.sp, color = TextSecondary)
        Text("(درگاه پرداخت آزمایشی است)", fontSize = 12.sp, color = TextMuted)
        Spacer(Modifier.height(14.dp))
        Text("بعد از پرداخت، نام و مشخصات خود را وارد می‌کنید و ویدیوهای ترم برایتان باز می‌شود. اعتبار حساب ۱۰۰ روز است.",
            fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Danger, fontSize = 12.sp) }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { error = null; loading = true }, enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Surface, strokeWidth = 2.dp)
            else Text("پرداخت", fontWeight = FontWeight.Bold, color = Surface)
        }
        TextButton(onClick = onCancel, enabled = !loading) { Text("تغییر ترم", color = TextSecondary) }
    }
}

/** پیام راهنمای صفحه‌های خالی */
@Composable
fun EmptyHint(title: String, hint: String, modifier: Modifier = Modifier) = Column(
    modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(hint, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
}

/** صفحه خوش‌آمدگویی قبل از انتخاب سطح */
@Composable
fun IntroScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.School, null, tint = Primary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text("به آکادمی زبان پروین اعتصامی خوش آمدید", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("ویدیوهای کلاس خود را آنلاین ببینید یا دانلود کنید و بدون اینترنت تماشا کنید.", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        listOf("۱  سطح و ترم خود را انتخاب کنید", "۲  هزینه ترم را پرداخت کنید", "۳  نام و مشخصات خود را ثبت کنید", "۴  ویدیوهای ترم برایتان باز می‌شود").forEach {
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Text(it, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.padding(14.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Text("شروع", fontWeight = FontWeight.Bold, color = Surface)
        }
        TextButton(onClick = onLogin) { Text("قبلاً ثبت‌نام کرده‌ام (ورود)", color = TextSecondary) }
    }
}

private fun digits(x: String) = x.map { c -> when (c) { in '۰'..'۹' -> '0' + (c - '۰'); in '٠'..'٩' -> '0' + (c - '٠'); else -> c } }.joinToString("").trim()

/** ثبت‌نام بعد از پرداخت؛ onSubmit خطا را به صورت متن (یا null برای موفقیت) برمی‌گرداند */
@Composable
fun RegisterScreen(term: String, onSubmit: suspend (name: String, phone: String, nid: String) -> String?) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nid by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ثبت‌نام", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("پرداخت ترم $term انجام شد. مشخصات خود را وارد کنید؛ بعداً با همین شماره موبایل و کد ملی وارد می‌شوید.",
            fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
        OutlinedTextField(name, { name = it; error = null }, label = { Text("نام و نام خانوادگی") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(phone, { phone = it; error = null }, label = { Text("شماره موبایل") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(nid, { nid = it; error = null }, label = { Text("کد ملی (۱۰ رقم)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Danger, fontSize = 12.sp) }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                val n = name.trim(); val p = digits(phone); val d = digits(nid)
                if (n.isEmpty() || p.length < 10 || d.length != 10) { error = "نام، شماره موبایل و کد ملی ۱۰ رقمی را درست وارد کنید"; return@Button }
                loading = true
                scope.launch { error = try { onSubmit(n, p, d) } catch (e: Exception) { "اتصال به سرور برقرار نشد؛ اینترنت را بررسی کنید" }; loading = false }
            },
            enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) { if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Surface, strokeWidth = 2.dp) else Text("ثبت‌نام و شروع", fontWeight = FontWeight.Bold, color = Surface) }
    }
}
