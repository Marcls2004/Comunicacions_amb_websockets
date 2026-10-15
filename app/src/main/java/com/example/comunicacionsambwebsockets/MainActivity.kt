package com.example.comunicacionsambwebsockets

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class MainActivity : AppCompatActivity() {
    var wsclient : MyWebSocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        connectaWS()
    }

    // "inner" és important per accedir als mètodes de MainActivity
    inner class MyWebSocketClient(serverUri: URI) : WebSocketClient(serverUri) {

        override fun onOpen(handshakedata: ServerHandshake?) {
            println("✅ Connectat al servidor")
            println("🔗 Codi d'estat: ${handshakedata?.httpStatus}")
            println("📝 Descripció: ${handshakedata?.httpStatusMessage}")
        }

        override fun onMessage(message: String?) {
            println("📥 Rebut: $message")

            // crides segures a la GUI (main thread)
            runOnUiThread {
                val tv = findViewById<TextView>(R.id.textView)
                tv.text = "Nou missatge: $message"
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            println("🔌 Connexió tancada")
            println("📋 Codi: $code, Raó: $reason, Remote: $remote")
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
        val uri = URI("wss://echo.websocket.org") // utilitzeu wss:// si va sobre HTTPS
        wsclient = MyWebSocketClient(uri)
        wsclient.connect()
    }
}