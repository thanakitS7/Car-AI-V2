package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object GoogleSheetsSyncManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    const val DEFAULT_WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbzdZ539qcXHrTY66hrdjXBeecYVH3Q14-3tBwQBerIEzabPDPFiJEwv47nHDYYIzTZj/exec"

    suspend fun sendTelemetryToGoogleSheets(
        webhookUrl: String,
        vehicleId: String,
        vehicleName: String,
        licensePlate: String,
        status: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Int,
        fuelPercent: Int,
        batteryVoltage: Double,
        driverName: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val targetUrl = webhookUrl.ifBlank { DEFAULT_WEBHOOK_URL }
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        try {
            // Build JSON payload
            val jsonPayload = JSONObject().apply {
                put("timestamp", timeStr)
                put("vehicleId", vehicleId)
                put("vehicleName", vehicleName)
                put("licensePlate", licensePlate)
                put("driverName", driverName)
                put("driver", driverName)
                put("status", status)
                put("latitude", latitude)
                put("longitude", longitude)
                put("speedKmh", speedKmh)
                put("fuelPercent", fuelPercent)
                put("batteryVoltage", batteryVoltage)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 302 || response.code == 200) {
                    val respBody = response.body?.string() ?: "OK"
                    Log.d("GoogleSheetsSync", "Success sending to Google Sheet: $respBody")
                    Result.success("ส่งข้อมูลสำเร็จเวลา $timeStr")
                } else {
                    // Fallback to GET query parameters if POST requires redirect handling in Apps Script
                    val fallbackGetResult = sendGetFallback(
                        targetUrl, timeStr, vehicleId, vehicleName, licensePlate,
                        status, latitude, longitude, speedKmh, fuelPercent, batteryVoltage, driverName
                    )
                    fallbackGetResult
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleSheetsSync", "Error sending POST to Google Sheet", e)
            // Attempt GET fallback as Apps Script Web Apps handle doGet gracefully
            sendGetFallback(
                targetUrl, timeStr, vehicleId, vehicleName, licensePlate,
                status, latitude, longitude, speedKmh, fuelPercent, batteryVoltage, driverName
            )
        }
    }

    private fun sendGetFallback(
        baseUrl: String,
        timeStr: String,
        vehicleId: String,
        vehicleName: String,
        licensePlate: String,
        status: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Int,
        fuelPercent: Int,
        batteryVoltage: Double,
        driverName: String = ""
    ): Result<String> {
        return try {
            val urlBuilder = baseUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return Result.failure(Exception("URL ไม่ถูกต้อง"))

            urlBuilder.addQueryParameter("timestamp", timeStr)
            urlBuilder.addQueryParameter("vehicleId", vehicleId)
            urlBuilder.addQueryParameter("vehicleName", vehicleName)
            urlBuilder.addQueryParameter("licensePlate", licensePlate)
            urlBuilder.addQueryParameter("driverName", driverName)
            urlBuilder.addQueryParameter("driver", driverName)
            urlBuilder.addQueryParameter("status", status)
            urlBuilder.addQueryParameter("latitude", latitude.toString())
            urlBuilder.addQueryParameter("longitude", longitude.toString())
            urlBuilder.addQueryParameter("speedKmh", speedKmh.toString())
            urlBuilder.addQueryParameter("fuelPercent", fuelPercent.toString())
            urlBuilder.addQueryParameter("batteryVoltage", batteryVoltage.toString())

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200 || response.code == 302) {
                    Result.success("ซิงค์สำเร็จเวลา $timeStr (GET)")
                } else {
                    Result.failure(Exception("HTTP Error Code: ${response.code}"))
                }
            }
        } catch (ex: Exception) {
            Log.e("GoogleSheetsSync", "GET fallback failed", ex)
            Result.failure(ex)
        }
    }

    suspend fun fetchVehiclesFromCloud(webhookUrl: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        val targetUrl = webhookUrl.ifBlank { DEFAULT_WEBHOOK_URL }
        try {
            val urlBuilder = targetUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(Exception("URL ไม่ถูกต้อง"))

            urlBuilder.addQueryParameter("action", "getVehicles")

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200) {
                    val rawBody = response.body?.string() ?: ""
                    val jsonArray = org.json.JSONArray(rawBody)
                    val resultList = mutableListOf<JSONObject>()
                    for (i in 0 until jsonArray.length()) {
                        resultList.add(jsonArray.getJSONObject(i))
                    }
                    Result.success(resultList)
                } else {
                    Result.failure(Exception("Cloud return code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleSheetsSync", "Failed to fetch vehicles from cloud", e)
            Result.failure(e)
        }
    }
}
