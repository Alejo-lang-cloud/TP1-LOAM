package com.example.appnat2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.example.appnat2.databinding.ScreenTelefonoBinding
import com.google.firebase.database.FirebaseDatabase

@Composable
fun TelefonoScreen() {
    AndroidView(
        factory = { context ->
            val binding = ScreenTelefonoBinding.inflate(android.view.LayoutInflater.from(context))

            sincronizarNumerosFirebase()

            fun realizarLlamadaPrueba(ctx: Context, servicio: String, numeroOficial: String) {
                // Registro en la firebase, registramos las llamadas en la bd.
                try {
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
                //con Intent le indicamos a la app que abra el teléfono y marque '113'
                //usamos ACTION_DIAL (a diferencia de ACTION_CALL) para que el usuario vea el numero antes de llamar
                try {
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

            binding.btnBomberos.setOnClickListener {
                realizarLlamadaPrueba(context, "Bomberos", "100")
            }
            binding.cardBomberos.setOnClickListener {
                realizarLlamadaPrueba(context, "Bomberos", "100")
            }

            binding.btnPolicia.setOnClickListener {
                realizarLlamadaPrueba(context, "Policía", "101")
            }
            binding.cardPolicia.setOnClickListener {
                realizarLlamadaPrueba(context, "Policía", "101")
            }

            binding.btnDefensaCivil.setOnClickListener {
                realizarLlamadaPrueba(context, "Defensa Civil", "103")
            }
            binding.cardDefensaCivil.setOnClickListener {
                realizarLlamadaPrueba(context, "Defensa Civil", "103")
            }

            binding.btnSem.setOnClickListener {
                realizarLlamadaPrueba(context, "SEM Salud", "107")
            }
            binding.cardSem.setOnClickListener {
                realizarLlamadaPrueba(context, "SEM Salud", "107")
            }

            binding.root
        }
    )
}

private fun sincronizarNumerosFirebase() { //numeros de telefono de emergencia linkeados con la firebase
    try {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("telefonos_emergencia")
        ref.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val numerosDefault = hashMapOf(
                    "bomberos" to "100",
                    "policia" to "101",
                    "defensa_civil" to "103",
                    "sem" to "107"
                )
                ref.setValue(numerosDefault)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
