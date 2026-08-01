package com.example.data

import com.example.util.LatLng

object SampleData {

    // Bangkok to Chonburi Motorway Waypoints
    val MOTORWAY_WAYPOINTS = listOf(
        LatLng(13.7381, 100.6283), // Rama IX Expressway Junction
        LatLng(13.7292, 100.6782), // Lat Krabang Toll Gate
        LatLng(13.7125, 100.7421), // Suvarnabhumi Curve
        LatLng(13.6821, 100.8251), // Bang Bo Service Area
        LatLng(13.6120, 100.9320), // Bang Pakong River Bridge
        LatLng(13.5220, 100.9950), // Chonburi Bypass Interchange
        LatLng(13.3611, 100.9847)  // Chonburi City Center
    )

    // Inner Bangkok Safe Zone Waypoints (Vibhavadi - Din Daeng Corridor)
    val VIBHAVADI_WAYPOINTS = listOf(
        LatLng(13.7628, 100.5511), // Victory Monument / Din Daeng
        LatLng(13.7885, 100.5601), // Sutthisan Intersection
        LatLng(13.8122, 100.5609), // Lat Phrao Intersection
        LatLng(13.8540, 100.5732), // Kasetsart Intersection
        LatLng(13.9130, 100.6010), // Don Mueang Airport
        LatLng(13.9650, 100.6170)  // Rangsit Tollway
    )

    val INITIAL_VEHICLES = listOf(
        VehicleEntity(
            id = "V001",
            name = "Toyota Camry Hybrid",
            licensePlate = "กข-1234 กรุงเทพฯ",
            modelYear = "2023 Fleet Black",
            status = "MOVING",
            currentLat = 13.7292,
            currentLng = 100.6782,
            speedKmh = 82,
            headingBearing = 105f,
            fuelPercent = 78,
            batteryVoltage = 12.8,
            activeRouteId = "R001",
            isEngineLocked = false
        ),
        VehicleEntity(
            id = "V002",
            name = "Honda CR-V Turbo",
            licensePlate = "ฮฮ-5678 ชลบุรี",
            modelYear = "2024 Pearl White",
            status = "ALERT_OUT_OF_ROUTE",
            currentLat = 13.7450, // Intentional deviation point off Motorway
            currentLng = 100.7600,
            speedKmh = 64,
            headingBearing = 45f,
            fuelPercent = 62,
            batteryVoltage = 12.6,
            activeRouteId = "R001",
            isEngineLocked = false
        ),
        VehicleEntity(
            id = "V003",
            name = "Isuzu D-Max Cargo",
            licensePlate = "รร-9999 สมุทรปราการ",
            modelYear = "2022 Silver Commercial",
            status = "IDLE",
            currentLat = 13.7628,
            currentLng = 100.5511,
            speedKmh = 0,
            headingBearing = 0f,
            fuelPercent = 91,
            batteryVoltage = 12.9,
            activeRouteId = "R002",
            isEngineLocked = false
        )
    )

    val INITIAL_ROUTES = listOf(
        RouteGeofenceEntity(
            id = "R001",
            vehicleId = "V001",
            name = "เส้นทางหลัก: Motorway กรุงเทพฯ - ชลบุรี",
            type = "ROUTE_CORRIDOR",
            centerLat = 13.7292,
            centerLng = 100.6782,
            radiusMeters = 300.0, // Corridor width 300m
            maxAllowedSpeed = 120,
            toleranceMeters = 200,
            waypointsJson = "13.7381,100.6283;13.7292,100.6782;13.7125,100.7421;13.6821,100.8251;13.6120,100.9320;13.5220,100.9950;13.3611,100.9847",
            isActive = true,
            startLocationName = "ด่านพระราม 9",
            endLocationName = "เมืองชลบุรี"
        ),
        RouteGeofenceEntity(
            id = "R002",
            vehicleId = "V003",
            name = "โซนปลอดภัย: วิภาวดี - ดอนเมือง",
            type = "ROUTE_CORRIDOR",
            centerLat = 13.7628,
            centerLng = 100.5511,
            radiusMeters = 400.0,
            maxAllowedSpeed = 90,
            toleranceMeters = 150,
            waypointsJson = "13.7628,100.5511;13.7885,100.5601;13.8122,100.5609;13.8540,100.5732;13.9130,100.6010;13.9650,100.6170",
            isActive = true,
            startLocationName = "อนุสาวรีย์ชัยฯ",
            endLocationName = "รังสิต"
        ),
        RouteGeofenceEntity(
            id = "R003",
            vehicleId = "V002",
            name = "รัศมีควบคุม: คลังสินค้าลาดกระบัง (Geofence)",
            type = "CIRCLE_ZONE",
            centerLat = 13.7292,
            centerLng = 100.6782,
            radiusMeters = 1500.0, // 1.5 km radius circle zone
            maxAllowedSpeed = 60,
            toleranceMeters = 100,
            waypointsJson = "13.7292,100.6782",
            isActive = true,
            startLocationName = "ศูนย์กระจายสินค้าลาดกระบัง",
            endLocationName = "รัศมี 1.5 กม."
        )
    )

    val INITIAL_ALERTS = listOf(
        AlertEntity(
            id = 101,
            vehicleId = "V002",
            vehicleName = "Honda CR-V Turbo",
            licensePlate = "ฮฮ-5678 ชลบุรี",
            alertType = "ROUTE_DEPARTURE",
            severity = "CRITICAL",
            title = "⚠️ เบี่ยงออกนอกเส้นทางกำหนด!",
            description = "รถยนต์ตรวจพบการเคลื่อนที่ออกห่างจากเส้นทาง Motorway เกินระยะปลอดภัย (380 ม.) บริเวณเขตลาดกระบัง",
            latitude = 13.7450,
            longitude = 100.7600,
            distanceFromRouteMeters = 380,
            timestamp = System.currentTimeMillis() - (1000 * 60 * 8), // 8 mins ago
            isAcknowledged = false
        ),
        AlertEntity(
            id = 102,
            vehicleId = "V001",
            vehicleName = "Toyota Camry Hybrid",
            licensePlate = "กข-1234 กรุงเทพฯ",
            alertType = "SPEEDING",
            severity = "WARNING",
            title = "⚡ ตรวจพบความเร็วเกินกำหนด",
            description = "ความเร็วปัจจุบัน 124 กม./ชม. เกินลิมิตเส้นทางที่กำหนดไว้ 120 กม./ชม.",
            latitude = 13.6821,
            longitude = 100.8251,
            distanceFromRouteMeters = 0,
            timestamp = System.currentTimeMillis() - (1000 * 60 * 45), // 45 mins ago
            isAcknowledged = true
        )
    )
}
