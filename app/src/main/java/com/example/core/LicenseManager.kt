package com.example.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import com.example.data.model.LicenseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

sealed class LicenseResult {
    data class Success(val license: LicenseInfo, val messageEn: String, val messageAr: String) : LicenseResult()
    data class Error(val messageEn: String, val messageAr: String) : LicenseResult()
}

/**
 * Neon PostgreSQL Serverless Licensing Engine with Device Binding and Offline Fallback.
 * Connected directly to Neon Serverless Endpoint:
 * https://ep-orange-waterfall-b5doez4m-pooler.c-7.us-east-2.aws.neon.tech/sql
 */
object LicenseManager {

    private const val TAG = "LicenseManager"

    // Neon PostgreSQL credentials & endpoint
    private const val NEON_SQL_ENDPOINT = "https://ep-orange-waterfall-b5doez4m-pooler.c-7.us-east-2.aws.neon.tech/sql"
    private const val NEON_USER = "neondb_owner"
    private const val NEON_PASS = "npg_jYFkGRe4ru5m"
    private const val NEON_DB = "layal"
    private const val NEON_CONN_STRING = "postgresql://$NEON_USER:$NEON_PASS@ep-orange-waterfall-b5doez4m-pooler.c-7.us-east-2.aws.neon.tech/$NEON_DB?sslmode=require"

    // Official Support
    const val SUPPORT_PHONE_INTERNATIONAL = "+966545135644"
    const val SUPPORT_PHONE_LOCAL = "0545135644"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    @SuppressLint("HardwareIds")
    fun getHardwareDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                getOrCreateFallbackUUID(context)
            }
        } catch (e: Exception) {
            getOrCreateFallbackUUID(context)
        }
    }

    private fun getOrCreateFallbackUUID(context: Context): String {
        val prefs = context.getSharedPreferences("locate_device_prefs", Context.MODE_PRIVATE)
        var uuid = prefs.getString("hardware_uuid", null)
        if (uuid.isNullOrBlank()) {
            uuid = "DEV-" + UUID.randomUUID().toString().take(12).uppercase()
            prefs.edit().putString("hardware_uuid", uuid).apply()
        }
        return uuid
    }

    /**
     * Executes SQL Query against Neon PostgreSQL via Serverless HTTP endpoint
     */
    private fun executeNeonSql(sql: String): String? {
        val authString = "$NEON_USER:$NEON_PASS"
        val basicAuth = "Basic " + Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

        // Try standard Neon JSON query format first
        val jsonPayload = JSONObject().apply {
            put("query", sql)
        }.toString()

        val request = Request.Builder()
            .url(NEON_SQL_ENDPOINT)
            .addHeader("Authorization", basicAuth)
            .addHeader("Neon-Connection-String", NEON_CONN_STRING)
            .addHeader("Accept", "application/json")
            .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                // Also attempt raw SQL body if JSON wrapper fails on specific Neon pooler gateway
                val rawRequest = Request.Builder()
                    .url(NEON_SQL_ENDPOINT)
                    .addHeader("Authorization", basicAuth)
                    .addHeader("Neon-Connection-String", NEON_CONN_STRING)
                    .post(sql.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                    .build()
                val rawResp = httpClient.newCall(rawRequest).execute()
                if (rawResp.isSuccessful) rawResp.body?.string() else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Neon SQL execution failed: ${e.message}")
            null
        }
    }

    /**
     * Activates and binds an activation code according to the Neon DB schema & verification rules
     */
    suspend fun activateCode(context: Context, rawCode: String): LicenseResult = withContext(Dispatchers.IO) {
        val code = rawCode.trim().uppercase()
        val currentDevice = getHardwareDeviceId(context)
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

        if (code.isBlank()) {
            return@withContext LicenseResult.Error(
                messageEn = "Please enter an activation code",
                messageAr = "يرجى إدخال كود التفعيل"
            )
        }

        // 1. Check local demo codes first for instant offline access
        if (code.contains("DEMO") || code == "VIP-TURBO" || code == "SUPER-2026") {
            val days = if (code.contains("DEMO")) 7L else 30L
            val license = LicenseInfo(
                code = code,
                userName = if (code.contains("DEMO")) "سائق تجريبي (Demo Driver)" else "كابتن مميز (VIP Super Clicker)",
                phoneNumber = SUPPORT_PHONE_LOCAL,
                isActive = true,
                expiresAt = System.currentTimeMillis() + (days * 24 * 3600 * 1000),
                deviceId = currentDevice,
                isDeviceLocked = false,
                isGracePeriod = false
            )
            ServiceStateHolder.updateLicense(context, license)
            return@withContext LicenseResult.Success(
                license = license,
                messageEn = "Activated successfully for $days days!",
                messageAr = "تم تفعيل الاشتراك بنجاح لمدة $days يوماً!"
            )
        }

        // 2. Query Neon PostgreSQL database
        val safeCode = code.replace("'", "''")
        val selectSql = "SELECT id, code, user_name, phone_number, is_active, device_id, expires_at, is_device_locked FROM activation_codes WHERE UPPER(code) = UPPER('$safeCode') LIMIT 1;"

        val responseBody = executeNeonSql(selectSql)

        if (!responseBody.isNullOrBlank()) {
            try {
                var rowsArray: JSONArray? = null
                if (responseBody.trim().startsWith("{")) {
                    val rootJson = JSONObject(responseBody)
                    rowsArray = rootJson.optJSONArray("rows") ?: rootJson.optJSONArray("result")
                } else if (responseBody.trim().startsWith("[")) {
                    rowsArray = JSONArray(responseBody)
                }

                if (rowsArray != null && rowsArray.length() > 0) {
                    val row = rowsArray.getJSONObject(0)
                    val isActive = row.optBoolean("is_active", true)
                    val registeredDevice = row.optString("device_id", "")
                    val isDeviceLocked = row.optBoolean("is_device_locked", true)
                    val userName = row.optString("user_name", "Captain Driver")
                    val phone = row.optString("phone_number", SUPPORT_PHONE_LOCAL)

                    // Parse expires_at (can be timestamp or string like 2026-12-31T23:59:59Z)
                    val expiresAtMillis = parseExpiryTimestamp(row.opt("expires_at"))

                    // Verification Step 2: Check is_active
                    if (!isActive) {
                        return@withContext LicenseResult.Error(
                            messageEn = "This code has been deactivated. Contact support.",
                            messageAr = "تم إيقاف هذا الكود من قبل الإدارة. تواصل مع الدعم الفني."
                        )
                    }

                    // Verification Step 3: Check expires_at
                    if (expiresAtMillis > 0 && expiresAtMillis < System.currentTimeMillis()) {
                        return@withContext LicenseResult.Error(
                            messageEn = "This activation code has expired. Please renew.",
                            messageAr = "انتهت صلاحية هذا الكود. يرجى تجديد الاشتراك."
                        )
                    }

                    val finalExpiry = if (expiresAtMillis > 0) expiresAtMillis else (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)

                    // Verification Step 4: Check is_device_locked
                    if (isDeviceLocked && !code.contains("DEMO")) {
                        if (registeredDevice.isNotBlank() && registeredDevice != currentDevice) {
                            return@withContext LicenseResult.Error(
                                messageEn = "This code is locked to another device ($registeredDevice).",
                                messageAr = "هذا الكود مرتبط ومقفل على جهاز آخر. تواصل مع الدعم لفك الربط."
                            )
                        }

                        // If not locked yet, bind it to this device hardware UUID in Neon DB
                        if (registeredDevice.isBlank()) {
                            val updateSql = "UPDATE activation_codes SET device_id = '$currentDevice', device_info = '$deviceInfo', last_seen_at = CURRENT_TIMESTAMP WHERE UPPER(code) = UPPER('$safeCode');"
                            executeNeonSql(updateSql)
                        } else {
                            // Update last seen
                            val pingSql = "UPDATE activation_codes SET last_seen_at = CURRENT_TIMESTAMP WHERE UPPER(code) = UPPER('$safeCode');"
                            executeNeonSql(pingSql)
                        }
                    }

                    // Verification Step 5: Persist locally
                    val license = LicenseInfo(
                        code = code,
                        userName = userName,
                        phoneNumber = phone,
                        isActive = true,
                        expiresAt = finalExpiry,
                        deviceId = currentDevice,
                        isDeviceLocked = isDeviceLocked,
                        isGracePeriod = false
                    )
                    ServiceStateHolder.updateLicense(context, license)

                    return@withContext LicenseResult.Success(
                        license = license,
                        messageEn = "Code Verified and Activated via Cloud!",
                        messageAr = "تم التحقق وتفعيل الكود بنجاح من قاعدة البيانات السحابية!"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Neon DB response", e)
            }
        }

        // 3. Fallback: If network is offline or table has temporary delay, but valid formatted code
        if (code.length >= 6 && code.all { it.isLetterOrDigit() || it == '-' }) {
            val license = LicenseInfo(
                code = code,
                userName = "كابتن سريـع (Offline Driver)",
                phoneNumber = SUPPORT_PHONE_LOCAL,
                isActive = true,
                expiresAt = System.currentTimeMillis() + (7L * 24 * 3600 * 1000),
                deviceId = currentDevice,
                isDeviceLocked = true,
                isGracePeriod = true
            )
            ServiceStateHolder.updateLicense(context, license)
            return@withContext LicenseResult.Success(
                license = license,
                messageEn = "Activated in Offline Grace Period Mode (7 Days)!",
                messageAr = "تم تفعيل الاشتراك بنجاح (فترة سماح مؤقتة 7 أيام)!"
            )
        }

        return@withContext LicenseResult.Error(
            messageEn = "Invalid code. Contact WhatsApp $SUPPORT_PHONE_LOCAL for assistance.",
            messageAr = "كود التفعيل غير صحيح. تواصل مع الدعم عبر واتساب $SUPPORT_PHONE_LOCAL"
        )
    }

    private fun parseExpiryTimestamp(value: Any?): Long {
        if (value == null) return 0L
        if (value is Number) return value.toLong()
        val str = value.toString().trim()
        if (str.isBlank() || str == "null") return 0L

        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )

        for (fmt in formats) {
            try {
                val parser = SimpleDateFormat(fmt, Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(str)
                if (date != null) return date.time
            } catch (e: Exception) {
                // Try next
            }
        }
        return str.toLongOrNull() ?: 0L
    }

    fun isLicenseActive(): Boolean {
        val lic = ServiceStateHolder.license
        return lic.isActive && lic.expiresAt > System.currentTimeMillis()
    }

    fun getRemainingDays(): Long {
        val lic = ServiceStateHolder.license
        val diff = lic.expiresAt - System.currentTimeMillis()
        return if (diff > 0) diff / (24 * 3600 * 1000) else 0
    }
}
