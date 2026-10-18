package com.example.comunicacionsambwebsockets

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    var wsclient : MyWebSocketClient? = null

    private lateinit var tvHistorial: TextView
    private lateinit var etMissatge: EditText
    private lateinit var btnEnviar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Vincular elementos de la interfaz gráfica
        tvHistorial = findViewById(R.id.textView)
        etMissatge = findViewById(R.id.txtMissatge)
        btnEnviar = findViewById(R.id.btnEnviar)

        // Acción del botón Enviar
        btnEnviar.setOnClickListener {
            val text = etMissatge.text.toString().trim()
            if (text.isNotEmpty()) {
                // Comprobamos si el socket está abierto antes de intentar transmitir
                if (wsclient != null && wsclient!!.isOpen) {
                    wsclient?.send(text) // Enviamos el texto al servidor local
                    tvHistorial.append("📤 Tu: $text\n") // Lo pintamos en tu pantalla inmediatamente
                    etMissatge.text.clear() // Limpiamos el cajón de escritura
                } else {
                    // Si el socket se había caído, avisamos al usuario e intentamos forzar reconexión
                    tvHistorial.append("⚠️ Error: No s'ha pogut enviar, el canal està tancat. Reconnectant...\n")
                    connectaWS()
                }
            }
        }

        // Lanzamos la primera conexión de red
        connectaWS()
    }

    inner class MyWebSocketClient(serverUri: URI) : WebSocketClient(serverUri) {

        override fun onOpen(handshakedata: ServerHandshake?) {
            println("✅ Connectat al servidor")
            runOnUiThread {
                tvHistorial.append("🟢 Connectat al servidor de xat de Proxmox\n")
            }
        }

        override fun onMessage(message: String?) {
            println("📥 Rebut: $message")

            // Cuando el servidor procesa el mensaje y nos lo devuelve, lo pintamos en el chat
            runOnUiThread {
                // Filtramos para no duplicar tu propio mensaje si el servidor te lo devuelve como eco
                if (message != null && !message.startsWith("¡Conectado")) {
                    tvHistorial.append("📩 Servidor diu: $message\n")
                } else if (message != null) {
                    tvHistorial.append("ℹ️ $message\n")
                }
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            println("🔌 Connexió tancada: $reason. Reconnectant...")
            runOnUiThread {
                tvHistorial.append("🔴 Connexió perduda. Intentant connectar de nou...\n")
            }

            // HILO DE RECONEXIÓN AUTOMÁTICA EN BUCLE
            // Si el servidor o Proxmox tiran la conexión, este hilo espera 2 segundos y la levanta sola
            Thread {
                try {
                    Thread.sleep(2000)
                    connectaWS()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        override fun onError(ex: Exception?) {
            println("❌ Error: ${ex?.message}")
            ex?.printStackTrace()
        }
    }

    override fun onDestroy(){
        super.onDestroy()
        wsclient?.close()
    }

    fun connectaWS() {
        try {
            // Dirección IP de tu servidor Proxmox con el puerto asignado en Node.js
            val uri = URI("ws://192.168.152.38:8080")
            wsclient = MyWebSocketClient(uri)
            wsclient?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
