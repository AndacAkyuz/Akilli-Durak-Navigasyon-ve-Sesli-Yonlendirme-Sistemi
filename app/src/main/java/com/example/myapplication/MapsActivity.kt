package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
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
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var placesClient: PlacesClient
    private var currentLocation: LatLng? = null
    private var searchMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private lateinit var destinationInput: EditText
    private lateinit var routeDetails: TextView
    private val averageStepLength = 0.75 // ortalama metre cinsinden adım uzunluğu
    private lateinit var tts: TextToSpeech // text to speech google entegrasyon değişkeni
    private val RQ_SPEECH_REC = 102

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        maximizeVolume() // Uygulamaya girdiğinde ses seviyesini fulleyen kod

        val talkButton = findViewById<Button>(R.id.talkToPush)
        val microphoneButton = findViewById<Button>(R.id.microphoneButton)
        val showAllButtons = findViewById<Button>(R.id.showAllButtons)

        talkButton.setOnClickListener {
            askspeechinput()
        }

        showAllButtons.setOnClickListener {
            showAllButtons()
        }

        microphoneButton.setOnClickListener {
            askspeechinput()
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("tr", "TR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "This Language is not supported")
                } else {
                    tts.speak(
                        "Merhaba navigasyon uygulamamıza hoşgeldiniz. Uygulamayı kullanmak için cihazınızın ses açma tuşuna bastıktan sonra mikrofona gitmek istediğiniz yeri söylemeniz yeterlidir. Olası bir iptal veya durdurma için de ses kısma tuşuna basabilirsiniz. Keyifli ulaşımlar dileriz",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                }
            } else {
                Log.e("TTS", "Initialization Failed!")
            }
        }

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

        val startNavigationButton: Button = findViewById(R.id.startNavigationButton)
        startNavigationButton.setOnClickListener {
            startNavigation()
        }

        // Initialize location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    if (isNavigating) {
                        updateNavigation(location)
                    }
                }
            }
        }
    }

    private fun showAllButtons() {
        val microphoneButton = findViewById<Button>(R.id.microphoneButton)
        microphoneButton.visibility = View.GONE

        val showAllButtons = findViewById<Button>(R.id.showAllButtons)
        showAllButtons.visibility = View.GONE

        val destinationInput: EditText = findViewById(R.id.destinationInput)
        val destinationButton: Button = findViewById(R.id.destinationButton)
        val createRouteButton: Button = findViewById(R.id.createRouteButton)
        val startNavigationButton: Button = findViewById(R.id.startNavigationButton)
        val routeDetails: TextView = findViewById(R.id.routeDetails)
        val talkToPush: Button = findViewById(R.id.talkToPush)
        val stopNavigationButton: Button = findViewById(R.id.stopNavigationButton)

        destinationInput.visibility = View.VISIBLE
        destinationButton.visibility = View.VISIBLE
        createRouteButton.visibility = View.VISIBLE
        startNavigationButton.visibility = View.VISIBLE
        routeDetails.visibility = View.VISIBLE
        talkToPush.visibility = View.VISIBLE
        stopNavigationButton.visibility = View.VISIBLE
    }

    private fun maximizeVolume() { //Ses fulleyen kod
        // AudioManager'ı al
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Medya ses seviyesini maksimuma çıkar
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean { // Ses açma kısma tuşlarına işlev atayan fonksiyon
        // Ses açma tuşuna basıldığında
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // talkToPush butonunu çalıştır
            val talkButton = findViewById<Button>(R.id.talkToPush)
            talkButton.performClick()
            return true
        }
        // Ses kısma tuşuna basıldığında
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            stopNavigation(null)
            return true
        }
        // Diğer tuşlar için varsayılan işlevi gerçekleştir
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RQ_SPEECH_REC && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0).toString()

            if (spokenText.isNotEmpty()) {
                destinationInput.setText(spokenText)
                if (!isNavigating && !isRouteDrawn) {
                    searchAndNavigate(spokenText)
                }
            } else {
                Toast.makeText(this, "Please speak clearly.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var isNavigating = false
    private var isRouteDrawn = false

    private fun searchAndNavigate(address: String) {
        if (isNavigating || isRouteDrawn) return

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
                        searchMarker?.remove()
                        searchMarker = mMap.addMarker(MarkerOptions().position(it).title(address))
                        currentLocation?.let { origin ->
                            drawRoute(origin, it, "transit")
                        }
                    }
                }
            }
        }
    }

    private var isDrawingRoute = false // Flag to check if route is being drawn

    private fun drawRoute(origin: LatLng, destination: LatLng, mode: String) {
        if (isDrawingRoute || isRouteDrawn) return

        isDrawingRoute = true
        val url = getDirectionsUrl(origin, destination, mode)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = URL(url).readText()
                withContext(Dispatchers.Main) {
                    // Çift rotayı önlemek için önceki rotayı temizleyin
                    routePolyline?.remove()
                    parseDirections(result)
                    isRouteDrawn = true
                    if (!isNavigating) {
                        startNavigation()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapsActivity, "Error in drawing route: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isDrawingRoute = false
            }
        }
    }

    private fun askspeechinput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Ses tanımlanamadı", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR") // Türkçe dil ayarı
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Konuşun")
            startActivityForResult(intent, RQ_SPEECH_REC)
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
        routePolyline?.remove()  // Clear the previous route
        val path = mutableListOf<LatLng>()

        for (i in 0 until steps.length()) {
            val step = steps.getJSONObject(i)
            val travelMode = step.getString("travel_mode")
            val polyline = step.getJSONObject("polyline").getString("points")
            val segmentPath = PolyUtil.decode(polyline)

            if (travelMode == "WALKING") {
                mMap.addPolyline(PolylineOptions().addAll(segmentPath).color(Color.BLUE).width(10f))
                updateRouteDetails("Yürüyerek ${segmentPath.size} adım sonra dön.")
            } else if (travelMode == "TRANSIT") {
                val transitDetails = step.getJSONObject("transit_details")
                val departureStop = transitDetails.getJSONObject("departure_stop").getString("name")
                val arrivalStop = transitDetails.getJSONObject("arrival_stop").getString("name")
                val vehicleType = transitDetails.getJSONObject("line").getJSONObject("vehicle").getString("type")
                val lineName = transitDetails.getJSONObject("line").getString("short_name")

                // Otobüs ve durak bilgilerini güncelle
                updateNavigationStatus(lineName, arrivalStop)

                mMap.addPolyline(PolylineOptions().addAll(segmentPath).color(Color.RED).width(10f))
                updateRouteDetails("$departureStop'dan $arrivalStop'a $lineName $vehicleType ile gidin. $arrivalStop'ta inin.")
            }
            path.addAll(segmentPath)
        }
        routePolyline = mMap.addPolyline(PolylineOptions().addAll(path).width(12f).color(Color.TRANSPARENT))
    }

    private fun updateRouteDetails(detail: String) {  // parseDirections sınıfı için gerekli update route details fonksiyonu
        routeDetails.append("\n$detail")
    }

    private var currentBusNumber: String = "" // updateNavigationStatus fonksiyonunun değişkeni 1
    private var currentStop: String = "" // updateNavigationStatus fonksiyonunun değişkeni 2

    private fun updateNavigationStatus(busNumber: String, stopName: String) { // navigatePath sınıfı için gerekli update navigasyon durumu fonksiyonu
        currentBusNumber = busNumber
        currentStop = stopName
    }

    private fun startNavigation() {
        if (routePolyline == null || routePolyline!!.points.isEmpty() || isNavigating) {
            Toast.makeText(this, "Lütfen ilk önce rota oluşturun.", Toast.LENGTH_SHORT).show()
            return
        }
        isNavigating = true
        startLocationUpdates() // Start real-time location updates
    }

    private var navigationHandler: Handler? = null // Global Handler reference for navigation  , Stop işlemi için gerekli

    fun stopNavigation(view: View?) {
        isNavigating = false

        // Haritadaki tüm görsel unsurları temizle
        mMap.clear()

        // Global Handler ve TTS işlemlerini durdur
        navigationHandler?.removeCallbacksAndMessages(null)
        navigationHandler = null
        tts.stop()

        // UI detaylarını sıfırla
        routeDetails.text = ""
        destinationInput.setText("")

        stopLocationUpdates() // Stop real-time location updates

        Toast.makeText(this, "Navigasyon durduruldu ve sıfırlandı.", Toast.LENGTH_SHORT).show()
        tts.speak("Navigasyon durduruldu ve sıfırlandı.", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun calculateDirection(from: LatLng, to: LatLng): String {
        val bearing = SphericalUtil.computeHeading(from, to)
        return when {
            bearing > -45 && bearing <= 45 -> "ileri" // Kuzey
            bearing > 45 && bearing <= 135 -> "sola" // Batı
            bearing > -135 && bearing <= -45 -> "sağa" // Doğu
            else -> "geri" // Güney
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        enableMyLocation()
        //mMap.isTrafficEnabled = true  //Trafik gösterimi
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID // Harita modunun hibrit olması yani binaların ve caddelerin gösterilmesi
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
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 16f))
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

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private var lastSpokenTime: Long = 0 // Son konuşma zamanını tutar
    private fun updateNavigation(location: Location) {
        currentLocation = LatLng(location.latitude, location.longitude)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 17f))

        routePolyline?.let { polyline ->
            if (polyline.points.isNotEmpty()) {
                val closestPointIndex = getClosestPointIndex(currentLocation!!, polyline.points)
                if (closestPointIndex < polyline.points.size - 1) {
                    val nextPoint = polyline.points[closestPointIndex + 1]
                    val direction = calculateDirection(currentLocation!!, nextPoint)
                    val distance = SphericalUtil.computeDistanceBetween(currentLocation!!, nextPoint)
                    val stepsToNext = (distance / averageStepLength).toInt()
                    val detailedDirection = "$stepsToNext adım sonra $direction yönüne gidin."

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSpokenTime > 10000) { // 10 saniye gecikme
                        Toast.makeText(this, detailedDirection, Toast.LENGTH_LONG).show()
                        tts.speak(detailedDirection, TextToSpeech.QUEUE_FLUSH, null, null)
                        lastSpokenTime = currentTime
                    }
                }
            }
        }
    }

    private fun getClosestPointIndex(currentLocation: LatLng, path: List<LatLng>): Int {
        var closestPointIndex = 0
        var closestDistance = Double.MAX_VALUE

        for (i in path.indices) {
            val distance = SphericalUtil.computeDistanceBetween(currentLocation, path[i])
            if (distance < closestDistance) {
                closestDistance = distance
                closestPointIndex = i
            }
        }

        return closestPointIndex
    }

    override fun onDestroy() {  //Text to speech boş ise konuşmasın
        if (tts != null) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
