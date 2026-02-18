package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienteHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String nombreCliente;

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // --- FASE 0: Procesar cabeceras HTTP y JSON (Requisito del profesor) ---
            String lineaHttp;
            // Leemos todo el bloque HTTP (cabeceras + cuerpo JSON) hasta encontrar la llave
            // de cierre "}"
            while ((lineaHttp = in.readLine()) != null) {
                System.out.println("[PETICIÓN HTTP] " + lineaHttp); // Imprime para que se vea en el servidor
                if (lineaHttp.trim().equals("}")) {
                    break; // Terminamos de leer el JSON y pasamos al juego
                }
            }
            // ---------------------------------------------------------------

            // --- FASE 1: Conexión Juego ---
            out.println("NOMBRE:Introduce tu nombre de jugador:");

            String respuestaNombre = in.readLine();
            this.nombreCliente = respuestaNombre;

            out.println("BIENVENIDO:Bienvenido " + nombreCliente + "! Espera a que comience la pregunta...");
            System.out.println("Jugador aceptado: " + nombreCliente);

            Servidor.broadcast("INFO:" + nombreCliente + " se ha unido al juego", this);

            // --- FASE 2: Bucle de Juego ---
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equalsIgnoreCase("/salir")) {
                    break;
                }

                // Si es una respuesta válida (A, B, C, D)
                if (inputLine.matches("[a-dA-D]")) {
                    Servidor.procesarRespuesta(this, inputLine.toUpperCase());
                } else {
                    out.println("INFO:Comando no reconocido. Para responder usa A, B, C o D.");
                }
            }

        } catch (IOException e) {
            System.err.println("Error en conexión con cliente: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nombreCliente != null) {
                Servidor.clientes.remove(this);
                Servidor.broadcast("INFO:" + nombreCliente + " ha abandonado el juego", this);
            }
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String getNombreCliente() {
        return nombreCliente;
    }
}