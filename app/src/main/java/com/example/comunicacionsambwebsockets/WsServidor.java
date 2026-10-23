package com.example.comunicacionsambwebsockets; // <-- Pon aquí tu paquete real (ej: com.example.chat)

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class WsServidor extends WebSocketServer {

    public WsServidor(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    public WsServidor(InetSocketAddress address) {
        super(address);
    }

    static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) throws InterruptedException, IOException {
        int port = 8888;
        boolean running = true;

        java.lang.System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        WsServidor socket = new WsServidor(port);
        socket.start();
        System.out.println("WsServidor funciona al port: " + socket.getPort());

        while (running) {
            String line = in.readLine();
            if (line != null) {
                socket.broadcast(line);
                if (line.equals("exit")) {
                    running = false;
                }
            }
        }

        System.out.println("Aturant WsServidor");
        socket.stop(1000);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("Benvingut a WsServer");
        broadcast("Nova connexió: " + handshake.getResourceDescriptor());
        String host = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        System.out.println(host + " s'ha connectat");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        broadcast(conn + " s'ha desconnectat");
        System.out.println(conn + " s'ha desconnectat");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.equalsIgnoreCase("list")) {
            System.out.println("Llista de connexions:");
            String strList = "";
            for (WebSocket ws : this.getConnections()) {
                strList += " " + getConnectionId(ws);
            }
            conn.send(strList);
        } else if (message.contains("to(")) {
            String strDesti = message.substring(message.indexOf("(") + 1, message.indexOf(")"));
            WebSocket desti = null;
            for (WebSocket ws : this.getConnections()) {
                if (strDesti.compareTo(getConnectionId(ws)) == 0) {
                    desti = ws;
                    break;
                }
            }

            if (desti != null) {
                String idOrigen = getConnectionId(conn);
                String idDesti = getConnectionId(desti);
                desti.send("Missatge privat de " + idOrigen + ": " + message);
                conn.send("S'ha entregat el missatge privat a: " + idDesti);
                System.out.println("Missatge privat entre " + idOrigen + " i " + idDesti + ": " + message);
            } else {
                conn.send("No s'ha trobat el destí " + strDesti);
            }
        } else {
            broadcast(getConnectionId(conn) + " ha dit: " + message);
            System.out.println("Broadcast de " + getConnectionId(conn) + ": " + message);
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        broadcast(message.array());
        System.out.println(conn + ": " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex != null) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        System.out.println("Escriu 'exit' per aturar el servidor");
        setConnectionLostTimeout(100);
    }

    public String getConnectionId (WebSocket connection) {
        String name = connection.toString();
        return name.replaceAll("org.java_websocket.WebSocketImpl@", "").substring(0, 3);
    }
}
