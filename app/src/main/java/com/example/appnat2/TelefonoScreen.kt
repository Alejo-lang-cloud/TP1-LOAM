package com.example.appnat2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.example.appnat2.databinding.ScreenTelefonoBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun TelefonoScreen() {
    AndroidView(
        factory = { context -> //bindeamos con screen_telefono.xml para poder acceder a los componentes
            val binding = ScreenTelefonoBinding.inflate(android.view.LayoutInflater.from(context))

            var numBomberos = "100"
            var numPolicia = "101"
            var numDefensaCivil = "103"
            var numSem = "107"

            // Escuchar cambios en TIEMPO REAL desde la Firebase para poder ir cambiando los numeros
            sincronizarNumerosFirebase(context) { bomberos, policia, defensaCivil, sem ->
                numBomberos = bomberos
                numPolicia = policia
                numDefensaCivil = defensaCivil
                numSem = sem

                // Actualizar la interfaz con los nuevos numeros
                binding.tvNumBomberos.text = "N° $numBomberos"
                binding.tvNumPolicia.text = "N° $numPolicia"
                binding.tvNumDefensaCivil.text = "N° $numDefensaCivil"
                binding.tvNumSem.text = "N° $numSem"
            }

            fun realizarLlamadaPrueba(ctx: Context, servicio: String, numeroOficial: String) {
                try { //accede a registro_llamadas en la firebase
                    val database = FirebaseDatabase.getInstance()
                    val llamadaRef = database.getReference("registro_llamadas").push()
                    val datosLlamada = hashMapOf(
                        "servicio" to servicio,
                        "numero_oficial" to numeroOficial,
                        "numero_marcado" to "113",
                        "timestamp" to System.currentTimeMillis()
                    )
                    llamadaRef.setValue(datosLlamada)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try { //usamos ACTION_DIAL para no llamar directamente al numero, primero abre el telefono con el numero preparado (a diferencia de ACTION_CALL)
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:113"))
                    ctx.startActivity(intent)
                    Toast.makeText(
                        ctx,
                        "Llamando a $servicio (Oficial $numeroOficial -> Marcando 113)",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(ctx, "No se pudo abrir el marcador de llamadas", Toast.LENGTH_SHORT).show()
                }
            }
             //usamos ViewBinding para bindear los botones
            binding.btnBomberos.setOnClickListener {
                realizarLlamadaPrueba(context, "Bomberos", numBomberos)
            }
            binding.cardBomberos.setOnClickListener {
                realizarLlamadaPrueba(context, "Bomberos", numBomberos)
            }

            binding.btnPolicia.setOnClickListener {
                realizarLlamadaPrueba(context, "Policía", numPolicia)
            }
            binding.cardPolicia.setOnClickListener {
                realizarLlamadaPrueba(context, "Policía", numPolicia)
            }

            binding.btnDefensaCivil.setOnClickListener {
                realizarLlamadaPrueba(context, "Defensa Civil", numDefensaCivil)
            }
            binding.cardDefensaCivil.setOnClickListener {
                realizarLlamadaPrueba(context, "Defensa Civil", numDefensaCivil)
            }

            binding.btnSem.setOnClickListener {
                realizarLlamadaPrueba(context, "SEM Salud", numSem)
            }
            binding.cardSem.setOnClickListener {
                realizarLlamadaPrueba(context, "SEM Salud", numSem)
            }

            binding.root
        }
    )
}

private fun sincronizarNumerosFirebase(context: Context, onNumerosActualizados: (String, String, String, String) -> Unit) {
    try {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("telefonos_emergencia")

        // addValueEventListener para escuchar cambios en TIEMPO REAL
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val numerosDefault = hashMapOf(
                        "bomberos" to "100",
                        "policia" to "101",
                        "defensa_civil" to "103",
                        "sem" to "107"
                    )
                    ref.setValue(numerosDefault)
                    Handler(Looper.getMainLooper()).post {
                        onNumerosActualizados("100", "101", "103", "107")
                    }
                } else {
                    // Conversion sin importar el tipo guardado en Firebase
                    val bomberos = snapshot.child("bomberos").value?.toString() ?: "100"
                    val policia = snapshot.child("policia").value?.toString() ?: "101"
                    val defensaCivil = snapshot.child("defensa_civil").value?.toString() ?: "103"
                    val sem = snapshot.child("sem").value?.toString() ?: "107"

                    //Vamos actualizacion el hilo principal de la app para evitar crasheos
                    Handler(Looper.getMainLooper()).post {
                        onNumerosActualizados(bomberos, policia, defensaCivil, sem)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Se ejecuta si ocurre un error de permisos o conexión en Firebase
            }
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
