package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object SupabaseSyncManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    const val DEFAULT_SUPABASE_URL = "https://rhzglphlfzhautvwpnae.supabase.co"
    const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJoemdscGhsZnpoYXV0dndwbmFlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU4OTQzNDUsImV4cCI6MjEwMTQ3MDM0NX0.JMx8DvXgES0x9N7YmjW2n_7mgMYm_bgAkv39xb_Q2Jo"

    /**
     * Send telemetry data to Supabase table (telemetry / vehicles)
     */
    suspend fun sendTelemetryToSupabase(
        baseUrl: String = DEFAULT_SUPABASE_URL,
        anonKey: String = DEFAULT_ANON_KEY,
        vehicleId: String,
        vehicleName: String,
        licensePlate: String,
        status: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Int,
        fuelPercent: Int,
        batteryVoltage: Double,
        driverName: String = "",
        officeName: String = "",
        provinceGroup: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanedUrl = baseUrl.trim().removeSuffix("/").removeSuffix("/rest/v1")
        val timeStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

        try {
            val jsonPayload = JSONObject().apply {
                put("vehicle_id", vehicleId)
                put("vehicle_name", vehicleName)
                put("license_plate", licensePlate)
                put("driver_name", driverName)
                put("office_name", officeName)
                put("province_group", provinceGroup)
                put("status", status)
                put("speed_kmh", speedKmh)
                put("latitude", latitude)
                put("longitude", longitude)
                put("fuel_percent", fuelPercent)
                put("battery_voltage", batteryVoltage)
                put("updated_at", timeStr)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toRequestBody(mediaType)

            // Try 1: UPSERT (POST with merge-duplicates)
            val requestUpsert = Request.Builder()
                .url("$cleanedUrl/rest/v1/vehicles")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(body)
                .build()

            var isSuccess = false
            var lastResponseBody = ""
            var lastCode = 0

            client.newCall(requestUpsert).execute().use { response ->
                lastCode = response.code
                lastResponseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    isSuccess = true
                }
            }

            // Try 2: If Upsert failed (e.g. no primary key constraint), try PATCH update
            if (!isSuccess) {
                val patchUrl = "$cleanedUrl/rest/v1/vehicles?vehicle_id=eq.$vehicleId"
                val requestPatch = Request.Builder()
                    .url(patchUrl)
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .patch(body)
                    .build()

                client.newCall(requestPatch).execute().use { response ->
                    lastCode = response.code
                    val patchBody = response.body?.string() ?: ""
                    if (response.isSuccessful && patchBody != "[]" && patchBody.isNotEmpty()) {
                        isSuccess = true
                        lastResponseBody = patchBody
                    }
                }
            }

            // Try 3: If PATCH didn't update anything, try standard INSERT (POST without resolution header)
            if (!isSuccess) {
                val requestInsert = Request.Builder()
                    .url("$cleanedUrl/rest/v1/vehicles")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(body)
                    .build()

                client.newCall(requestInsert).execute().use { response ->
                    lastCode = response.code
                    lastResponseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        isSuccess = true
                    }
                }
            }

            if (isSuccess) {
                Log.d("SupabaseSync", "Successfully synced to Supabase: $lastResponseBody")
                logTelemetryHistory(cleanedUrl, anonKey, vehicleId, latitude, longitude, speedKmh, status, timeStr)
                Result.success("ส่งข้อมูลไปยัง Supabase สำเร็จ ($timeStr)")
            } else {
                Log.e("SupabaseSync", "Failed Supabase request ($lastCode): $lastResponseBody")
                Result.failure(Exception("Supabase Error ($lastCode): $lastResponseBody"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Error sending to Supabase", e)
            Result.failure(e)
        }
    }

    private fun logTelemetryHistory(
        baseUrl: String,
        anonKey: String,
        vehicleId: String,
        lat: Double,
        lng: Double,
        speed: Int,
        status: String,
        timestamp: String
    ) {
        try {
            val jsonPayload = JSONObject().apply {
                put("vehicle_id", vehicleId)
                put("latitude", lat)
                put("longitude", lng)
                put("speed_kmh", speed)
                put("status", status)
                put("created_at", timestamp)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/telemetry_history")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonPayload.toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w("SupabaseSync", "Optional telemetry_history log omitted or table not present")
        }
    }

    /**
     * Fetch all vehicles from Supabase
     */
    suspend fun fetchVehiclesFromSupabase(
        baseUrl: String = DEFAULT_SUPABASE_URL,
        anonKey: String = DEFAULT_ANON_KEY
    ): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        val cleanedUrl = baseUrl.trim().removeSuffix("/").removeSuffix("/rest/v1")

        try {
            val request = Request.Builder()
                .url("$cleanedUrl/rest/v1/vehicles?select=*")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(body)
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until jsonArray.length()) {
                        list.add(jsonArray.getJSONObject(i))
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user/driver data in Supabase 'users' or 'profiles' table
     */
    suspend fun updateUserInSupabase(
        baseUrl: String = DEFAULT_SUPABASE_URL,
        anonKey: String = DEFAULT_ANON_KEY,
        userId: String,
        name: String,
        role: String,
        email: String = "",
        phone: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanedUrl = baseUrl.trim().removeSuffix("/").removeSuffix("/rest/v1")

        try {
            val payload = JSONObject().apply {
                put("id", userId)
                put("name", name)
                put("role", role)
                put("email", email)
                put("phone", phone)
                put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("$cleanedUrl/rest/v1/users")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(payload.toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success("อัปเดตผู้ใช้ใน Supabase สำเร็จ")
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
