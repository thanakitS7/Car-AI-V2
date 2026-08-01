package com.example.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double)

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Haversine distance in meters between two lat/lng coordinates
     */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculate heading/bearing in degrees (0..360) from point A to B
     */
    fun calculateBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val dLng = Math.toRadians(lng2 - lng1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val y = sin(dLng) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLng)

        var bearing = Math.toDegrees(atan2(y, x)).toFloat()
        if (bearing < 0) {
            bearing += 360f
        }
        return bearing
    }

    /**
     * Minimum distance in meters from point P to a polyline route defined by waypoints
     */
    fun minDistanceFromRoute(pLat: Double, pLng: Double, waypoints: List<LatLng>): Double {
        if (waypoints.isEmpty()) return 0.0
        if (waypoints.size == 1) {
            return distanceMeters(pLat, pLng, waypoints[0].lat, waypoints[0].lng)
        }

        var minDistance = Double.MAX_VALUE
        for (i in 0 until waypoints.size - 1) {
            val dist = distanceToSegment(
                pLat, pLng,
                waypoints[i].lat, waypoints[i].lng,
                waypoints[i + 1].lat, waypoints[i + 1].lng
            )
            if (dist < minDistance) {
                minDistance = dist
            }
        }
        return minDistance
    }

    private fun distanceToSegment(
        px: Double, py: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double
    ): Double {
        val dx = x2 - x1
        val dy = y2 - y1

        if (dx == 0.0 && dy == 0.0) {
            return distanceMeters(px, py, x1, y1)
        }

        // Project point P onto segment
        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        val clampedT = t.coerceIn(0.0, 1.0)

        val projLat = x1 + clampedT * dx
        val projLng = y1 + clampedT * dy

        return distanceMeters(px, py, projLat, projLng)
    }

    /**
     * Format meters nicely (e.g. 150m or 2.4 km)
     */
    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.1f กม.", meters / 1000)
        } else {
            "${meters.toInt()} ม."
        }
    }

    /**
     * Convert Lat / Lng to human-readable Thai location / place name
     */
    fun getPlaceName(context: android.content.Context, lat: Double, lng: Double): String {
        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale("th", "TH"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val thoroughfare = addr.thoroughfare ?: ""
                val subLocality = addr.subLocality ?: addr.subAdminArea ?: ""
                val locality = addr.locality ?: addr.adminArea ?: ""
                val featureName = addr.featureName ?: ""

                val parts = listOf(featureName, thoroughfare, subLocality, locality)
                    .filter { it.isNotBlank() && !it.matches(Regex("^[0-9+.\\s-]+$")) }
                    .distinct()

                if (parts.isNotEmpty()) {
                    return parts.take(2).joinToString(" ")
                }
            }
        } catch (e: Exception) {
            // Geocoder service offline or unavailable
        }
        return getFallbackThaiLandmark(lat, lng)
    }

    fun getFallbackThaiLandmark(lat: Double, lng: Double): String {
        return when {
            lat in 13.72..13.76 && lng in 100.60..100.66 -> "ถ.พระราม 9 กรุงเทพฯ"
            lat in 13.70..13.74 && lng in 100.66..100.73 -> "ถ.มอเตอร์เวย์ กรุงเทพฯ"
            lat in 13.67..13.72 && lng in 100.73..100.78 -> "เขตลาดกระบัง กรุงเทพฯ"
            lat in 13.68..13.71 && lng in 100.74..100.82 -> "สนามบินสุวรรณภูมิ"
            lat in 13.63..13.68 && lng in 100.82..100.95 -> "อ.บางบ่อ สมุทรปราการ"
            lat in 13.50..13.62 && lng in 100.95..101.10 -> "อ.เมืองฉะเชิงเทรา"
            lat in 13.30..13.50 && lng in 100.90..101.05 -> "อ.เมืองชลบุรี"
            lat in 13.70..13.78 && lng in 100.48..100.58 -> "ปทุมวัน / สยาม กรุงเทพฯ"
            else -> String.format("พิกัด %.3f, %.3f", lat, lng)
        }
    }
}
