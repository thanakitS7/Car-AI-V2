package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.TrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TrackingForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    private var lastSyncTimeMs = 0L

    companion object {
        const val CHANNEL_ID = "gps_tracking_channel"
        const val NOTIFICATION_ID = 8881
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"

        var isServiceRunning = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TrackingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopTrackingInternal()
            return START_NOT_STICKY
        }

        if (action == ACTION_START || intent == null) {
            startForegroundNotification()
            startLocationUpdates()
            isServiceRunning = true
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ติดตามพิกัด GPS ในภูมิหลัง",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "แสดงการแจ้งเตือนขณะแอปทำงานย้อนหลังต่อเนื่อง"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TrackingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚚 ระบบติดตาม GPS กำลังทำงานในภูมิหลัง")
            .setContentText("ติดตามและส่งพิกัดรถขึ้น Supabase เรียลไทม์ แม้จะพักแอป")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "🏁 ถึงเป้าหมาย (จบการทำงาน)", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startLocationUpdates() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            // Location permissions not granted yet, skip until user grants in UI
            return
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                handleNewLocation(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val listener = locationListener ?: return

        try {
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    listener
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    3f,
                    listener
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNewLocation(location: Location) {
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val repo = TrackingRepository(db.appDao())

                val vehicles = repo.allVehicles.firstOrNull() ?: emptyList()
                val activeVehicle = vehicles.firstOrNull() ?: return@launch

                val speedKmh = if (location.hasSpeed()) (location.speed * 3.6f).toInt() else 0
                val lat = location.latitude
                val lng = location.longitude
                val heading = location.bearing

                // Update position in local DB
                repo.updateVehiclePosition(
                    vehicleId = activeVehicle.id,
                    newLat = lat,
                    newLng = lng,
                    speedKmh = speedKmh,
                    heading = heading
                )

                // Rate limit cloud sync to every 5 seconds
                val now = System.currentTimeMillis()
                if (now - lastSyncTimeMs > 5000L) {
                    lastSyncTimeMs = now
                    
                    // Supabase Cloud Sync
                    try {
                        com.example.util.SupabaseSyncManager.sendTelemetryToSupabase(
                            baseUrl = com.example.util.SupabaseSyncManager.DEFAULT_SUPABASE_URL,
                            anonKey = com.example.util.SupabaseSyncManager.DEFAULT_ANON_KEY,
                            vehicleId = activeVehicle.id,
                            vehicleName = activeVehicle.name,
                            licensePlate = activeVehicle.licensePlate,
                            driverName = activeVehicle.driverName,
                            status = if (speedKmh > 3) "MOVING (GPS สดภูมิหลัง)" else "IDLE (จอดพักภูมิหลัง)",
                            latitude = lat,
                            longitude = lng,
                            speedKmh = speedKmh,
                            fuelPercent = activeVehicle.fuelPercent,
                            batteryVoltage = activeVehicle.batteryVoltage
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopTrackingInternal() {
        try {
            locationListener?.let { locationManager?.removeUpdates(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isServiceRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopTrackingInternal()
        serviceScope.cancel()
        super.onDestroy()
    }
}
