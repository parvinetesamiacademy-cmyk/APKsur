package com.app.aparatdownloader.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("jwt_token", null)
    }

    fun saveLicense(licenseJson: String) {
        sharedPreferences.edit().putString("offline_license", licenseJson).apply()
    }

    fun getLicense(): String? {
        return sharedPreferences.getString("offline_license", null)
    }

    fun saveVideoKey(id: String, key: String) {
        sharedPreferences.edit().putString("vk_$id", key).apply()
    }

    fun getVideoKey(id: String): String? = sharedPreferences.getString("vk_$id", null)

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
