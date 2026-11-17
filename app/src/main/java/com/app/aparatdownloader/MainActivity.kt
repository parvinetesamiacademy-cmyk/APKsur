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
import com.app.aparatdownloader.data.api.RegisterRequest
import com.app.aparatdownloader.ui.onboarding.IntroScreen
import com.app.aparatdownloader.ui.onboarding.RegisterScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                    var started by remember { mutableStateOf(false) }
                    var showLogin by remember { mutableStateOf(false) }
                    var term by remember { mutableStateOf(prefs.getString("term", null)) }
                    var paid by remember { mutableStateOf(prefs.getBoolean("paid", false)) }
                    val ctx = this@MainActivity

                    when {
                        isLoggedIn -> MainAppScreen(onLogout = {
                            keyStoreManager.clearAll()
                            java.io.File(filesDir, "cache").deleteRecursively()
                            prefs.edit().clear().apply()
                            term = null; paid = false; started = false; showLogin = false; isLoggedIn = false
                        })
                        showLogin -> AuthScreen(onLoginSuccess = { isLoggedIn = true })
                        !started && term == null -> IntroScreen(onStart = { started = true }, onLogin = { showLogin = true })
                        term == null -> LevelTermScreen(onTermChosen = { prefs.edit().putString("term", it).apply(); term = it })
                        !paid -> PaymentScreen(
                            term = term ?: "",
                            onPay = { prefs.edit().putBoolean("paid", true).apply(); paid = true; true }, // پرداخت جعلی
                            onCancel = { prefs.edit().remove("term").apply(); term = null }
                        )
                        else -> RegisterScreen(term = term ?: "", onSubmit = { n, p, d ->
                            val api = ApiClient.create { null }
                            val id = android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"
                            val res = api.register(RegisterRequest(n, p, d, term ?: "", id, android.os.Build.MODEL))
                            val body = res.body()
                            if (res.isSuccessful && body != null) {
                                keyStoreManager.saveToken(body.token)
                                keyStoreManager.saveLicense(com.google.gson.Gson().toJson(body.license))
                                isLoggedIn = true; null
                            } else {
                                val code = ApiClient.errorCodeOf(withContext(Dispatchers.IO) { res.errorBody()?.string() })
                                if (code == "DUPLICATE_PHONE_OR_NID") "این شماره یا کد ملی قبلاً ثبت شده؛ از «ورود» استفاده کنید" else "ثبت‌نام انجام نشد؛ دوباره تلاش کنید"
                            }
                        })
                    }
                }
            }
        }
    }
}
