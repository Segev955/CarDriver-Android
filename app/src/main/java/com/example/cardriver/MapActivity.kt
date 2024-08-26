package com.example.cardriver

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialize the osmdroid configuration
        Configuration.getInstance().load(this, applicationContext.getSharedPreferences("OSM", Context.MODE_PRIVATE))

        mapView = findViewById(R.id.map)

        // Set the initial view to the given coordinates
        val latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        val longitude = intent.getDoubleExtra("LONGITUDE", 0.0)

        mapView.controller.setZoom(15.0)
        val startPoint = GeoPoint(latitude, longitude)
        mapView.controller.setCenter(startPoint)

        // Add a marker on the location
        val marker = Marker(mapView)
        marker.position = startPoint
        marker.title = "Car Location"
        mapView.overlays.add(marker)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
