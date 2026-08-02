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

    const val DEFAULT_WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbzvT5UfAPvdeTNdBatWzbaIcnRYTW0ya076B_jD74ReMqUzx2Y-UwuEWaoIPdDPkXRZ/exec"

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
            // Build JSON payload with both English and Thai key aliases for max compatibility
            val jsonPayload = JSONObject().apply {
                put("timestamp", timeStr)
                put("วันเวลา", timeStr)

                put("vehicleId", vehicleId)
                put("รหัสรถ", vehicleId)

                put("vehicleName", vehicleName)
                put("ชื่อรถ", vehicleName)

                put("licensePlate", licensePlate)
                put("ทะเบียนรถ", licensePlate)

                put("driverName", driverName)
                put("driver", driverName)
                put("พนักงานขับรถ", driverName)
                put("ชื่อผู้ใช้/พนักงานขับรถ", driverName)

                put("status", status)
                put("สถานะ", status)

                put("speedKmh", speedKmh)
                put("speed", speedKmh)
                put("ความเร็ว", speedKmh)

                put("latitude", latitude)
                put("lat", latitude)
                put("ละติจูด", latitude)

                put("longitude", longitude)
                put("lng", longitude)
                put("ลองจิจูด", longitude)

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
            urlBuilder.addQueryParameter("วันเวลา", timeStr)

            urlBuilder.addQueryParameter("vehicleId", vehicleId)
            urlBuilder.addQueryParameter("รหัสรถ", vehicleId)

            urlBuilder.addQueryParameter("vehicleName", vehicleName)
            urlBuilder.addQueryParameter("ชื่อรถ", vehicleName)

            urlBuilder.addQueryParameter("licensePlate", licensePlate)
            urlBuilder.addQueryParameter("ทะเบียนรถ", licensePlate)

            urlBuilder.addQueryParameter("driverName", driverName)
            urlBuilder.addQueryParameter("driver", driverName)
            urlBuilder.addQueryParameter("ชื่อผู้ใช้/พนักงานขับรถ", driverName)
            urlBuilder.addQueryParameter("พนักงานขับรถ", driverName)

            urlBuilder.addQueryParameter("status", status)
            urlBuilder.addQueryParameter("สถานะ", status)

            urlBuilder.addQueryParameter("speedKmh", speedKmh.toString())
            urlBuilder.addQueryParameter("speed", speedKmh.toString())
            urlBuilder.addQueryParameter("ความเร็ว", speedKmh.toString())

            urlBuilder.addQueryParameter("latitude", latitude.toString())
            urlBuilder.addQueryParameter("lat", latitude.toString())
            urlBuilder.addQueryParameter("ละติจูด", latitude.toString())

            urlBuilder.addQueryParameter("longitude", longitude.toString())
            urlBuilder.addQueryParameter("lng", longitude.toString())
            urlBuilder.addQueryParameter("ลองจิจูด", longitude.toString())

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
                    val rawBody = response.body?.string()?.trim() ?: ""
                    if (rawBody.startsWith("<!DOCTYPE") || rawBody.startsWith("<html")) {
                        return@withContext Result.failure(
                            Exception("Webhook คืนค่าเป็น HTML: กรุณาเพิ่มการตรวจจับ e.parameter.action ใน doGet(e) เพื่อคืนค่า JSON")
                        )
                    }

                    val resultList = mutableListOf<JSONObject>()
                    try {
                        if (rawBody.startsWith("[")) {
                            val jsonArray = org.json.JSONArray(rawBody)
                            for (i in 0 until jsonArray.length()) {
                                resultList.add(jsonArray.getJSONObject(i))
                            }
                        } else if (rawBody.startsWith("{")) {
                            val jsonObj = org.json.JSONObject(rawBody)
                            if (jsonObj.has("vehicles")) {
                                val arr = jsonObj.getJSONArray("vehicles")
                                for (i in 0 until arr.length()) {
                                    resultList.add(arr.getJSONObject(i))
                                }
                            } else {
                                resultList.add(jsonObj)
                            }
                        } else {
                            return@withContext Result.failure(
                                Exception("รูปแบบตอบกลับไม่ใช่ JSON (โปรดเพิ่มฟังก์ชั่น doGet ใน Google Apps Script)")
                            )
                        }
                        Result.success(resultList)
                    } catch (jsonEx: Exception) {
                        Result.failure(Exception("แปลงข้อมูล JSON ล้มเหลว: ${jsonEx.localizedMessage}"))
                    }
                } else {
                    Result.failure(Exception("Google Sheet คืนค่ารหัสผิดพลาด: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleSheetsSync", "Failed to fetch vehicles from cloud", e)
            Result.failure(e)
        }
    }
}
