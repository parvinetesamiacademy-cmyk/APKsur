package com.app.aparatdownloader.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
