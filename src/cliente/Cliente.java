package cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

// PARA ACCEDER AL SERVIDOR -> ssh -i /Users/samuelolivas/FP/PSP/KeysFTP/ssh-key-2026-02-05.key ubuntu@80.225.187.167
// PARA HACER EL ATTACH -> sudo docker attach mi-juego
// PARA SALIR DEL ATTACH -> Pulsa Ctrl + P y justo después Ctrl + Q

public class Cliente {
    private static final String SERVER_IP = "80.225.187.167";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // 1. PEDIMOS EL NOMBRE ANTES DE CONECTAR PARA USARLO EN EL JSON
        System.out.println("--- KAHOOT CLIENTE ---");
        System.out.print("Introduce tu nombre de jugador: ");
        String miNombre = scanner.nextLine();

        // --- CONFIGURACIÓN SSL (SEGURIDAD) ---
        System.setProperty("javax.net.ssl.trustStore", "myserver.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        // -------------------------------------

        try (SSLSocket socket = (SSLSocket) sslFactory.createSocket(SERVER_IP, SERVER_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // --- 2. PETICIÓN HTTP DINÁMICA ---
            // Generamos un ID aleatorio para que cada petición sea única
            long idSesion = System.currentTimeMillis();

            out.println("POST /api/kahoot-like/join HTTP/1.1");
            out.println("Content-Type: application/json");
            out.println(""); // LÍNEA BLANCA OBLIGATORIA

            // JSON personalizado con tu nombre
            out.println("{");
            out.println("  \"action\": \"join_game\",");
            out.println("  \"playerId\": \"" + idSesion + "\",");
            out.println("  \"nickname\": \"" + miNombre + "\","); // <--- Aquí va tu nombre real
            out.println("  \"client\": \"JavaConsole\"");
            out.println("}");
            // ---------------------------------------------------

            // Hilo para escuchar al servidor
            new Thread(() -> {
                try {
                    String fromServer;
                    while ((fromServer = in.readLine()) != null) {

                        // TRUCO: Si el servidor nos pide el nombre, lo enviamos automáticamente
                        // porque ya se lo pedimos al usuario al principio.
                        if (fromServer.startsWith("NOMBRE:")) {
                            out.println(miNombre);
                        } else if (fromServer.startsWith("BIENVENIDO:")) {
                            System.out.println(">>> " + fromServer.substring(11));
                        } else if (fromServer.startsWith("INFO:")) {
                            System.out.println("[INFO] " + fromServer.substring(5));
                        } else if (fromServer.startsWith("PREGUNTA:")) {
                            System.out.println("\n¿? PREGUNTA: " + fromServer.substring(9));
                        } else if (fromServer.startsWith("OPCION_")) {
                            System.out.println("   " + fromServer.substring(9));
                        } else if (fromServer.startsWith("RANKING:")) {
                            System.out.println(fromServer.substring(8));
                        } else {
                            System.out.println(fromServer);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Desconectado del servidor.");
                    System.exit(0);
                }
            }).start();

            // Hilo principal para enviar respuestas (A, B, C, D)
            while (true) {
                if (scanner.hasNextLine()) {
                    String userInput = scanner.nextLine();
                    out.println(userInput);
                    if (userInput.equalsIgnoreCase("/salir")) {
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor.");
            e.printStackTrace();
        }
    }
}