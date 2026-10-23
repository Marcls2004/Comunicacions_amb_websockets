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

class MainActivity : AppCompatActivity() {

    private lateinit var wsClient: MyWebSocketClient
    private lateinit var textViewChat: TextView
    private lateinit var scrollViewChat: ScrollView
    private lateinit var txtMissatge: EditText
    private lateinit var btnEnviar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Vincular los componentes de tu XML por su ID
        textViewChat = findViewById(R.id.textView)
        scrollViewChat = findViewById(R.id.scrollView)
        txtMissatge = findViewById(R.id.txtMissatge)
        btnEnviar = findViewById(R.id.btnEnviar)

        // 2. Conectar al servidor local (10.0.2.2 es el localhost del PC desde el emulador)
        connectaWS()

        // 3. Configurar la acción del botón Enviar
        btnEnviar.setOnClickListener {
            val text = txtMissatge.text.toString().trim()
            if (text.isNotEmpty()) {
                enviaMissatge(text)
                txtMissatge.text.clear() // Limpiar la caja de texto al enviar
            }
        }
    }

    // Clase interna para gestionar las llamadas del WebSocket
    inner class MyWebSocketClient(serverUri: URI) : WebSocketClient(serverUri) {

        override fun onOpen(handshakedata: ServerHandshake?) {
            actualitzarHistorial("✅ Connectat al servidor")
        }

        override fun onMessage(message: String?) {
            // Recibimos mensaje del servidor. Usamos runOnUiThread porque viene de un hilo de red
            runOnUiThread {
                actualitzarHistorial("📥 $message")
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            actualitzarHistorial("🔌 Connexió tancada: $reason")
        }

        override fun onError(ex: Exception?) {
            actualitzarHistorial("❌ Error: ${ex?.message}")
            ex?.printStackTrace()
        }
    }

    private fun connectaWS() {
        try {
            // Puerto 8888 tal como está definido en tu archivo WsServidor.java
            val uri = URI("ws://10.0.2.2:8888")
            wsClient = MyWebSocketClient(uri)
            wsClient.connect()
        } catch (e: Exception) {
            actualitzarHistorial("❌ Error al crear la URI del servidor")
            e.printStackTrace()
        }
    }

    private fun enviaMissatge(text: String) {
        // Comprobar que el socket esté abierto antes de enviar
        if (::wsClient.isInitialized && wsClient.isOpen) {
            wsClient.send(text)
        } else {
            actualitzarHistorial("⚠️ No s'ha pogut enviar el missatge (Sense connexió)")
        }
    }

    // Función auxiliar para pintar texto en pantalla y bajar el scroll automáticamente
    private fun actualitzarHistorial(missatge: String) {
        runOnUiThread {
            textViewChat.append("$missatge\n")
            // Forzar al ScrollView a deslizarse hacia abajo para ver lo último recibido
            scrollViewChat.post {
                scrollViewChat.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cerrar la conexión limpiamente si se destruye la actividad
        if (::wsClient.isInitialized) {
            wsClient.close()
        }
    }
}
