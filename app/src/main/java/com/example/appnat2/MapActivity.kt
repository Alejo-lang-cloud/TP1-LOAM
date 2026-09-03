package com.example.appnat2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.appnat2.databinding.ScreenMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar fragmento de Google Maps
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

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

        // Configurar opciones de interfaz del mapa
        try {
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            googleMap.uiSettings.isCompassEnabled = true
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        } catch (_: Exception) {
        }

        // Posición inicial por defecto (Santa Rosa, La Pampa) visible inmediatamente
        val posicionInicial = LatLng(latitudActual, longitudActual)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionInicial, 14f))
        googleMap.addMarker(
            MarkerOptions()
                .position(posicionInicial)
                .title("Santa Rosa, La Pampa")
                .snippet("Ubicación predeterminada")
        )
        obtenerDireccionFisica(latitudActual, longitudActual)

        // Intentar actualizar con la ubicación GPS exacta
        verificarGpsyObtenerUbicacion()
    }

    private fun verificarGpsyObtenerUbicacion() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsActivado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsActivado) {
            binding.tvDireccionGrande.text = "GPS Desactivado (Usando locación aproximada)"
            Toast.makeText(this, "Por favor, active el GPS para mayor precisión", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación de Permisos de Ubicación
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

        // Obtener la ubicación GPS del dispositivo
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                latitudActual = location.latitude
                longitudActual = location.longitude
                val ubicacionLatLng = LatLng(latitudActual, longitudActual)

                mGoogleMap?.clear()
                mGoogleMap?.addMarker(
                    MarkerOptions()
                        .position(ubicacionLatLng)
                        .title("Tu Ubicación Actual")
                )
                mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacionLatLng, 16f))

                obtenerDireccionFisica(latitudActual, longitudActual)
            } else {
                Toast.makeText(this, "Obteniendo fix GPS...", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "No se pudo actualizar por GPS", Toast.LENGTH_SHORT).show()
        }
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