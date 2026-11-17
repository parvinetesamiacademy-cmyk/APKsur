package com.app.aparatdownloader

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.app.aparatdownloader.security.KeyStoreManager
import com.app.aparatdownloader.data.api.ApiClient
import com.app.aparatdownloader.data.api.PurchaseRequest
import com.app.aparatdownloader.ui.auth.AuthScreen
import com.app.aparatdownloader.ui.onboarding.LevelTermScreen
import com.app.aparatdownloader.ui.onboarding.PaymentScreen
import com.app.aparatdownloader.ui.navigation.MainAppScreen
import com.app.aparatdownloader.ui.theme.LuminaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screen capture & recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val keyStoreManager = KeyStoreManager(applicationContext)

        setContent {
            LuminaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val prefs = remember { getSharedPreferences("onboarding", MODE_PRIVATE) }
                    var isLoggedIn by remember {
                        mutableStateOf(keyStoreManager.getToken() != null || keyStoreManager.getLicense() != null)
                    }
                    var term by remember { mutableStateOf(prefs.getString("term", null)) }
                    var paid by remember { mutableStateOf(prefs.getBoolean("paid", false)) }

                    when {
                        // ۱) انتخاب سطح و ترم (اولین صفحه)
                        term == null && !paid -> LevelTermScreen(onTermChosen = {
                            prefs.edit().putString("term", it).apply(); term = it
                        })
                        // ۲) ورود با موبایل و کدملی
                        !isLoggedIn -> AuthScreen(onLoginSuccess = { isLoggedIn = true })
                        // ۳) پرداخت (فعلاً جعلی)؛ بعد از موفقیت ترم برای کاربر ثبت می‌شود
                        !paid -> PaymentScreen(
                            term = term ?: "",
                            onPay = {
                                val api = ApiClient.create { keyStoreManager.getToken() }
                                val ok = api.purchase(PurchaseRequest(term ?: "")).isSuccessful
                                if (ok) { prefs.edit().putBoolean("paid", true).apply(); paid = true }
                                ok
                            },
                            onCancel = { prefs.edit().remove("term").apply(); term = null }
                        )
                        // ۴) نمایش ویدیوهای ترم
                        else -> MainAppScreen(onLogout = {
                            keyStoreManager.clearAll()
                            java.io.File(filesDir, "cache").deleteRecursively()
                            prefs.edit().clear().apply()
                            term = null; paid = false; isLoggedIn = false
                        })
                    }
                }
            }
        }
    }
}
