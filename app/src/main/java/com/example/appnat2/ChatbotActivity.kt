package com.example.appnat2

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)

class ChatbotActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: MaterialButton
    private lateinit var adapter: ChatAdapter
    private val messagesList = mutableListOf<ChatMessage>()

    // Variable global en la Activity para guardar las respuestas de Firebase en memoria
    private val diccionarioBot = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        toolbar = findViewById(R.id.toolbar_chatbot)
        rvMessages = findViewById(R.id.rv_chat_messages)
        etInput = findViewById(R.id.et_user_input)
        btnSend = findViewById(R.id.btn_send_chat)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = ChatAdapter(messagesList)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        // Cargar respuestas dinámicas desde Firebase Realtime Database
        cargarRespuestasDelBot()

        // Mensaje de bienvenida inicial del Bot
        addBotMessage("¡Hola! 👋 Soy VAlert Bot, tu asistente virtual de emergencias. ¿En qué puedo ayudarte hoy?\n\nPuedes consultarme sobre sismos, incendios o inundaciones.")

        btnSend.setOnClickListener {
            val userText = etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                sendUserQuery(userText)
                etInput.setText("")
            }
        }

        // Configurar chips de sugerencias
        findViewById<Chip>(R.id.chip_sismo).setOnClickListener {
            sendUserQuery("¿Qué hacer en un sismo?")
        }
        findViewById<Chip>(R.id.chip_incendio).setOnClickListener {
            sendUserQuery("¿Cómo actuar en un incendio?")
        }
        findViewById<Chip>(R.id.chip_inundacion).setOnClickListener {
            sendUserQuery("Consejos por inundaciones")
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // 1. Función para cargar las respuestas desde Firebase
    private fun cargarRespuestasDelBot() {
        try {
            val database = FirebaseDatabase.getInstance()
            val botRef = database.getReference("chatbot_respuestas")

            // Usamos get() para leer los datos UNA sola vez, ahorrando ancho de banda
            botRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    for (hijo in snapshot.children) {
                        val palabraClave = hijo.key?.lowercase() ?: ""
                        val respuesta = hijo.value.toString()
                        diccionarioBot[palabraClave] = respuesta
                    }
                    Log.d("Chatbot", "Diccionario cargado con ${diccionarioBot.size} respuestas.")
                }
            }.addOnFailureListener {
                Log.e("Chatbot", "Error al cargar respuestas", it)
            }
        } catch (e: Exception) {
            Log.e("Chatbot", "Firebase no configurado o no disponible", e)
        }
    }

    private fun sendUserQuery(query: String) {
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        messagesList.add(ChatMessage(query, isUser = true, timestamp = timeNow))
        adapter.notifyItemInserted(messagesList.size - 1)
        rvMessages.smoothScrollToPosition(messagesList.size - 1)

        // Procesar mensaje del usuario buscando coincidencias en el diccionario de Firebase
        procesarMensajeUsuario(query)
    }

    // 2. Función para procesar la consulta del usuario y responder con el diccionario de Firebase
    private fun procesarMensajeUsuario(mensajeUsuario: String) {
        val mensajeLimpio = mensajeUsuario.lowercase().trim()

        var respuestaFinal = "Lo siento, soy un asistente básico. Prueba usar palabras clave como 'inundación', 'sismo' o 'incendio'."

        for ((clave, respuesta) in diccionarioBot) {
            if (mensajeLimpio.contains(clave)) {
                respuestaFinal = respuesta
                break
            }
        }

        rvMessages.postDelayed({
            addBotMessage(respuestaFinal)
        }, 400)
    }

    private fun addBotMessage(text: String) {
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        messagesList.add(ChatMessage(text, isUser = false, timestamp = timeNow))
        adapter.notifyItemInserted(messagesList.size - 1)
        rvMessages.smoothScrollToPosition(messagesList.size - 1)
    }
}

class ChatAdapter(private val list: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: LinearLayout = itemView.findViewById(R.id.ll_chat_message_root)
        val cardBubble: MaterialCardView = itemView.findViewById(R.id.card_chat_bubble)
        val tvSender: TextView = itemView.findViewById(R.id.tv_chat_sender)
        val tvMessage: TextView = itemView.findViewById(R.id.tv_chat_message_text)
        val tvTime: TextView = itemView.findViewById(R.id.tv_chat_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = list[position]
        holder.tvMessage.text = msg.text
        holder.tvTime.text = msg.timestamp

        if (msg.isUser) {
            holder.rootLayout.gravity = Gravity.END
            holder.tvSender.text = "Tú"
            holder.tvSender.setTextColor(Color.parseColor("#1565C0"))
            holder.cardBubble.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
        } else {
            holder.rootLayout.gravity = Gravity.START
            holder.tvSender.text = "VAlert Bot"
            holder.tvSender.setTextColor(Color.parseColor("#2E7D32"))
            holder.cardBubble.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        }
    }

    override fun getItemCount(): Int = list.size
}