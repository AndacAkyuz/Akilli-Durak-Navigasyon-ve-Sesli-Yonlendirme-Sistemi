package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.*
import java.net.URL
import org.json.JSONObject

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var currentLocation: LatLng? = null
    private var searchMarker: Marker? = null
    private var routePolyline: Polyline? = null

    private lateinit var destinationInput: EditText
    private lateinit var routeDetails: TextView

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        destinationInput = findViewById(R.id.destinationInput)
        routeDetails = findViewById(R.id.routeDetails)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initializePlaces()

        val destinationButton: Button = findViewById(R.id.destinationButton)
        destinationButton.setOnClickListener {
            val address = destinationInput.text.toString()
            if (address.isNotEmpty()) {
                searchLocation(address)
            } else {
                Toast.makeText(this, "Please enter an address.", Toast.LENGTH_SHORT).show()
            }
        }

        val createRouteButton: Button = findViewById(R.id.createRouteButton)
        createRouteButton.setOnClickListener {
            searchMarker?.position?.let { destination ->
                currentLocation?.let { origin ->
                    drawRoute(origin, destination, "transit")
                }
            }
        }
    }

    private fun initializePlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyAej1Jp0p05Sjx8laIdIHUmKDnHWFMeZyE")
        }
        placesClient = Places.createClient(this)
    }

    private fun searchLocation(address: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(address)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            for (prediction in response.autocompletePredictions) {
                val placeId = prediction.placeId
                val placeFields = listOf(Place.Field.LAT_LNG)
                val fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build()

                placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener { fetchPlaceResponse ->
                    val place = fetchPlaceResponse.place
                    place.latLng?.let {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                        searchMarker?.remove()
                        searchMarker = mMap.addMarker(MarkerOptions().position(it).title(address))
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Place not found: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error finding place: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawRoute(origin: LatLng, dest: LatLng, mode: String) {
        val url = getDirectionsUrl(origin, dest, mode)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = URL(url).readText()
                withContext(Dispatchers.Main) {
                    parseDirections(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapsActivity, "Error in drawing route: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun getDirectionsUrl(origin: LatLng, dest: LatLng, mode: String): String {
        val originStr = "origin=${origin.latitude},${origin.longitude}"
        val destStr = "destination=${dest.latitude},${dest.longitude}"
        val params = "$originStr&$destStr&sensor=false&mode=$mode"
        val output = "json"
        return "https://maps.googleapis.com/maps/api/directions/$output?$params&key=AIzaSyAej1Jp0p05Sjx8laIdIHUmKDnHWFMeZyE"
    }

    private fun parseDirections(jsonData: String) {
        val jsonObject = JSONObject(jsonData)
        val routes = jsonObject.getJSONArray("routes")
        val legs = routes.getJSONObject(0).getJSONArray("legs")
        val steps = legs.getJSONObject(0).getJSONArray("steps")

        for (i in 0 until steps.length()) {
            val step = steps.getJSONObject(i)
            val travelMode = step.getString("travel_mode")
            val polyline = step.getJSONObject("polyline").getString("points")
            val path = PolyUtil.decode(polyline)

            if (travelMode == "WALKING") {
                mMap.addPolyline(PolylineOptions().addAll(path).color(Color.BLUE).width(10f))
                updateRouteDetails("Yürüyüş mesafesi", path.size)
            } else if (travelMode == "TRANSIT") {
                val transitDetails = step.getJSONObject("transit_details")
                val departureStop = transitDetails.getJSONObject("departure_stop").getString("name")
                val arrivalStop = transitDetails.getJSONObject("arrival_stop").getString("name")
                val vehicleType = transitDetails.getJSONObject("line").getJSONObject("vehicle").getString("type")
                val lineName = transitDetails.getJSONObject("line").getString("short_name")

                mMap.addPolyline(PolylineOptions().addAll(path).color(Color.RED).width(10f))
                updateRouteDetails("Otobüs: $lineName, $vehicleType, Duraklar: $departureStop - $arrivalStop", path.size)
            }
        }
    }

    private fun updateRouteDetails(detail: String, pathLength: Int) {
        routeDetails.append("\n$detail, Mesafe: $pathLength Adım")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSION)
            return
        }
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
