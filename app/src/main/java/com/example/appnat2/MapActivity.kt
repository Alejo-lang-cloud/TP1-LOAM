package com.example.appnat2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.appnat2.databinding.ScreenMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ScreenMapBinding
    private var mGoogleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitudActual: Double = -36.6167
    private var longitudActual: Double = -64.2833
    private var direccionActualText: String = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar renderizador de Google Maps
        try {
            MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) {
            }
        } catch (_: Exception) {
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar MapView de Google Maps
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        // Cargar mapa interactivo de respaldo en WebView
        configurarMapaWebView(latitudActual, longitudActual)

        // Listeners de los botones
        binding.btnRegistrarFirebase.setOnClickListener {
            registrarUbicacionEnFirebase()
        }

        binding.btnActualizarUbicacion.setOnClickListener {
            verificarGpsyObtenerUbicacion()
        }

        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        try {
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            googleMap.uiSettings.isCompassEnabled = true
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        } catch (_: Exception) {
        }

        val posicionInicial = LatLng(latitudActual, longitudActual)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionInicial, 14f))
        googleMap.addMarker(
            MarkerOptions()
                .position(posicionInicial)
                .title("Santa Rosa, La Pampa")
        )

        obtenerDireccionFisica(latitudActual, longitudActual)
        verificarGpsyObtenerUbicacion()
    }

    private fun verificarGpsyObtenerUbicacion() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsActivado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsActivado) {
            binding.tvDireccionGrande.text = "GPS Desactivado (Ubicación aproximada)"
            Toast.makeText(this, "Active el GPS para obtener su posición exacta", Toast.LENGTH_SHORT).show()
            actualizarMapas(latitudActual, longitudActual, "Santa Rosa, La Pampa")
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        try {
            mGoogleMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) {
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                latitudActual = location.latitude
                longitudActual = location.longitude
                actualizarMapas(latitudActual, longitudActual, "Tu Ubicación Actual")
                obtenerDireccionFisica(latitudActual, longitudActual)
            } else {
                actualizarMapas(latitudActual, longitudActual, "Santa Rosa, La Pampa")
                Toast.makeText(this, "Obteniendo posición GPS...", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            actualizarMapas(latitudActual, longitudActual, "Santa Rosa, La Pampa")
        }
    }

    private fun actualizarMapas(lat: Double, lon: Double, titulo: String) {
        val latLng = LatLng(lat, lon)
        mGoogleMap?.clear()
        mGoogleMap?.addMarker(MarkerOptions().position(latLng).title(titulo))
        mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

        // Actualizar también el mapa interactivo de respaldo en WebView
        cargarUbicacionEnWebView(lat, lon, titulo)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configurarMapaWebView(lat: Double, lon: Double) {
        val settings = binding.webViewMapa.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        cargarUbicacionEnWebView(lat, lon, "Ubicación de Emergencia")
    }

    private fun cargarUbicacionEnWebView(lat: Double, lon: Double, titulo: String) {
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body, html, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #e0e0e0; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map', { zoomControl: true }).setView([$lat, $lon], 15);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap'
                    }).addTo(map);
                    var marker = L.marker([$lat, $lon]).addTo(map);
                    marker.bindPopup("<b>$titulo</b><br>Lat: $lat, Lon: $lon").openPopup();
                </script>
            </body>
            </html>
        """.trimIndent()

        binding.webViewMapa.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)

        // Si Google Maps no muestra la vista previa, activar automáticamente la vista WebView de mapa
        binding.mapView.postDelayed({
            if (binding.mapView.visibility == View.VISIBLE) {
                // Habilitar la vista de respaldo si el contenedor de Google Maps no puede descargar tiles
                binding.webViewMapa.visibility = View.VISIBLE
            }
        }, 1000)
    }

    private fun obtenerDireccionFisica(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { direcciones ->
                    runOnUiThread {
                        if (direcciones.isNotEmpty()) {
                            direccionActualText = direcciones[0].getAddressLine(0) ?: "Dirección no disponible"
                            binding.tvDireccionGrande.text = direccionActualText
                        } else {
                            binding.tvDireccionGrande.text = "Santa Rosa, La Pampa, Argentina"
                            direccionActualText = "Santa Rosa, La Pampa, Argentina"
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val direcciones = geocoder.getFromLocation(lat, lon, 1)
                if (!direcciones.isNullOrEmpty()) {
                    direccionActualText = direcciones[0].getAddressLine(0) ?: "Dirección no disponible"
                    binding.tvDireccionGrande.text = direccionActualText
                } else {
                    binding.tvDireccionGrande.text = "Santa Rosa, La Pampa, Argentina"
                    direccionActualText = "Santa Rosa, La Pampa, Argentina"
                }
            }
        } catch (e: Exception) {
            binding.tvDireccionGrande.text = "Lat: $lat, Lon: $lon"
            direccionActualText = "Lat: $lat, Lon: $lon"
        }
    }

    private fun registrarUbicacionEnFirebase() {
        if (latitudActual == 0.0 && longitudActual == 0.0) {
            Toast.makeText(this, "Aún no hay una ubicación válida para registrar", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val database = FirebaseDatabase.getInstance()
            val ubicacionRef = database.getReference("ubicaciones_siniestros").push()

            val datosUbicacion = hashMapOf(
                "latitud" to latitudActual,
                "longitud" to longitudActual,
                "direccion" to direccionActualText,
                "timestamp" to System.currentTimeMillis()
            )

            ubicacionRef.setValue(datosUbicacion).addOnSuccessListener {
                Toast.makeText(this, "¡Ubicación de emergencia registrada en Firebase!", Toast.LENGTH_LONG).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar en Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Ubicación obtenida: $direccionActualText (Firebase no configurado)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verificarGpsyObtenerUbicacion()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}