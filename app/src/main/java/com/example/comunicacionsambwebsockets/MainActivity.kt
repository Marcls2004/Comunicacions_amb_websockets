package com.example.comunicacionsambwebsockets

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var wsClient: MyWebSocketClient
    private var servidor: WsServidor? = null

    private lateinit var textViewChat: TextView
    private lateinit var scrollViewChat: ScrollView
    private lateinit var txtMissatge: EditText
    private lateinit var btnEnviar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewChat = findViewById(R.id.textView)
        scrollViewChat = findViewById(R.id.scrollView)
        txtMissatge = findViewById(R.id.txtMissatge)
        btnEnviar = findViewById(R.id.btnEnviar)

        // 1. Arrancar el servidor en segundo plano
        thread {
            try {
                // Escucha en el puerto 8888 de la red interna
                servidor = WsServidor(8888)
                servidor?.start()
                runOnUiThread { actualitzarHistorial("🏠 Servidor WebSocket encés al port 8888") }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Esperar un segundo a que el servidor esté listo y conectar el cliente de la app
        textViewChat.postDelayed({
            connectaWS()
        }, 1000)

        // 3. Acción del botón enviar de la app
        btnEnviar.setOnClickListener {
            val text = txtMissatge.text.toString().trim()
            if (text.isNotEmpty()) {
                enviaMissatge(text)
                txtMissatge.text.clear()
            }
        }
    }

    inner class MyWebSocketClient(serverUri: URI) : WebSocketClient(serverUri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            runOnUiThread { actualitzarHistorial("✅ App connectada al xat") }
        }

        override fun onMessage(message: String?) {
            runOnUiThread { actualitzarHistorial("📥 $message") }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            runOnUiThread { actualitzarHistorial("🔌 Connexió tancada") }
        }

        override fun onError(ex: Exception?) {
            runOnUiThread { actualitzarHistorial("❌ Error: ${ex?.message}") }
        }
    }

    private fun connectaWS() {
        try {
            // El emulador mapea su propio localhost interno como 127.0.0.1
            val uri = URI("ws://127.0.0.1:8888")
            wsClient = MyWebSocketClient(uri)
            wsClient.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun enviaMissatge(text: String) {
        if (::wsClient.isInitialized && wsClient.isOpen) {
            wsClient.send(text)
        } else {
            actualitzarHistorial("⚠️ Sense connexió amb el servidor")
        }
    }

    private fun actualitzarHistorial(missatge: String) {
        textViewChat.append("$missatge\n")
        scrollViewChat.post {
            scrollViewChat.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wsClient.isInitialized) wsClient.close()
        thread {
            try { servidor?.stop() } catch (e: Exception) {}
        }
    }
}
