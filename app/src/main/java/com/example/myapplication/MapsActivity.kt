package com.example.myapplication

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.myapplication.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Initialize the FusedLocationProviderClient.
        mMap.uiSettings.isZoomControlsEnabled = true

        // Kullanıcı konumunu etkinleştir
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Burada izin isteme kodunu ekleyebilirsin
            // Örneğin:
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // İzinler verilmişse yapılacak işlemler
            mMap.isMyLocationEnabled = true
            //Trafik Yoğunluğunu Gösterme
            mMap.setTrafficEnabled(true)
            // Açılır açılmaz dünya haritasında başlamak yerine
            // Kullanıcının mevcut konumunu fusedLocationClient fonksiyonu ile alıp
            // Oraya 15f değerinde zoom yapıyor
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                }
            }

            // Harita üzerinde bir konum belirleyin
            val ArabicaCoffee = LatLng(40.8229996156, 29.9302521437)
            mMap.addMarker(MarkerOptions().position(ArabicaCoffee).title("ArabicaCoffee Umuttepe"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(ArabicaCoffee))

            val AKapisi = LatLng(40.82399091160, 29.9218869209)
            mMap.addMarker(MarkerOptions().position(AKapisi).title("Kocaeli Üniversitesi A Kapısı"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(AKapisi))

            val BKapisi = LatLng(40.8232926963, 29.9253952503)
            mMap.addMarker(MarkerOptions().position(BKapisi).title("Kocaeli Üniversitesi B Kapısı"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(BKapisi))

            val CKapisi = LatLng(40.8220180285, 29.9335169792)
            mMap.addMarker(MarkerOptions().position(CKapisi).title("Kocaeli Üniversitesi C Kapısı"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(CKapisi))

            val KouHastane = LatLng(40.8251112882, 29.9186038971)
            mMap.addMarker(MarkerOptions().position(KouHastane).title("Kocaeli Üniversitesi Araştırma Hastanesi"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(KouHastane))
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

}
