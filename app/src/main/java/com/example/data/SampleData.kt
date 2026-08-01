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

    val INITIAL_VEHICLES = emptyList<VehicleEntity>()

    val INITIAL_ROUTES = emptyList<RouteGeofenceEntity>()

    val INITIAL_ALERTS = emptyList<AlertEntity>()
}
