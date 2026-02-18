package servidor;

import comunes.Pregunta;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Servidor {
    private static final int PORT = 8080;

    // --- CREDENCIALES FTP ---
    private static final String FTP_SERVER = "80.225.187.167"; // Tu IP de la nube
    private static final String FTP_FILE = "files/blooket.csv"; // La ruta
    private static final String FTP_USER = "alumno";
    private static final String FTP_PASS = "alumno";
    // ------------------------

    public static List<ClienteHandler> clientes = new ArrayList<>();

    // --- LÓGICA DEL JUEGO ---
    private static List<Pregunta> listaPreguntas = new ArrayList<>();
    private static int indicePreguntaActual = -1;
    private static boolean esperandoRespuestas = false;
    private static long tiempoInicioPregunta;

    private static Map<ClienteHandler, String> respuestasRonda = new HashMap<>();
    private static Map<ClienteHandler, Long> tiemposRonda = new HashMap<>();

    public static void main(String[] args) {
        // LÓGICA HÍBRIDA: Intenta FTP, si falla usa Local
        cargarPreguntas();

        System.out.println("Servidor iniciado en puerto " + PORT);
        System.out.println("Escribe 'NEXT' en la consola para lanzar una pregunta.");

        // Hilo del Administrador (Consola)
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                if (scanner.hasNextLine()) {
                    String comando = scanner.nextLine();
                    if (comando.equalsIgnoreCase("NEXT")) {
                        lanzarSiguientePregunta();
                    }
                }
            }
        }).start();

        // --- CONFIGURACIÓN SSL (SEGURIDAD) ---
        System.setProperty("javax.net.ssl.keyStore", "myserver.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        // Fíjate que ahora usamos SSLServerSocket en el try
        try (SSLServerSocket serverSocket = (SSLServerSocket) sslFactory.createServerSocket(PORT)) {
            while (true) {
                // Esta lógica de dentro es IGUAL que antes, no cambia
                Socket socket = serverSocket.accept();
                ClienteHandler cliente = new ClienteHandler(socket);
                clientes.add(cliente);
                new Thread(cliente).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- MÉTODOS DE CARGA DE PREGUNTAS ---

    private static void cargarPreguntas() {
        System.out.println("INTENTO 1: Conectando al FTP " + FTP_SERVER + "...");
        String ftpUrl = "ftp://" + FTP_USER + ":" + FTP_PASS + "@" + FTP_SERVER + "/" + FTP_FILE;

        try {
            URL url = new URL(ftpUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(10000); // 2 segundos timeout rápido

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String linea;
                in.readLine(); // Saltar cabecera
                while ((linea = in.readLine()) != null) {
                    procesarLineaCSV(linea);
                }
                System.out.println(">>> ÉXITO: Preguntas cargadas vía FTP.");
            }
        } catch (Exception e) {
            System.err.println("!!! FALLO FTP: " + e.getMessage());
            System.out.println("INTENTO 2: Buscando archivo local 'blooket.csv'...");
            cargarPreguntasLocal();
        }
    }

    private static void cargarPreguntasLocal() {
        // Busca el archivo en la raíz del proyecto (fuera de src)
        File archivo = new File("blooket.csv");

        if (!archivo.exists()) {
            System.err.println("ERROR FATAL: No hay FTP y no encuentro 'blooket.csv' en la raíz.");
            // Carga de emergencia para que no crashee si no hay nada
            listaPreguntas.add(new Pregunta("ERROR: Sin preguntas", new String[] { "A", "B", "C", "D" }, 0));
            return;
        }

        try (BufferedReader in = new BufferedReader(new FileReader(archivo))) {
            String linea;
            in.readLine(); // Saltar cabecera (Asumiendo que borraste la línea Template)

            while ((linea = in.readLine()) != null) {
                procesarLineaCSV(linea);
            }
            System.out.println(">>> ÉXITO: Preguntas cargadas desde LOCAL.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void procesarLineaCSV(String linea) {
        // Método auxiliar para no repetir código de parseo
        String[] parts = linea.split(",");
        // Verificamos longitud mínima (el CSV tiene unas 8 columnas)
        if (parts.length >= 8) {
            try {
                // Columna 1: Pregunta, Cols 2-5: Respuestas
                String enunciado = parts[1];
                String[] opciones = {
                        parts[2],
                        parts[3],
                        parts[4],
                        parts[5]
                };

                // Columna 7: Respuesta correcta (1-4). Restamos 1 para índice 0-3
                int correctaCSV = Integer.parseInt(parts[7].trim());
                int indiceCorrecta = correctaCSV - 1;

                listaPreguntas.add(new Pregunta(enunciado, opciones, indiceCorrecta));
            } catch (Exception ex) {
                // Si falla una línea (ej: cabecera extra), la ignoramos y seguimos
                // System.err.println("Línea ignorada: " + ex.getMessage());
            }
        }
    }

    // --- MÉTODOS DEL JUEGO ---

    public static void lanzarSiguientePregunta() {
        if (clientes.isEmpty()) {
            System.out.println("No hay jugadores conectados.");
            return;
        }

        indicePreguntaActual++;
        if (indicePreguntaActual >= listaPreguntas.size()) {
            broadcast("FIN_JUEGO", null);
            broadcast("RANKING:=== JUEGO TERMINADO ===", null);
            indicePreguntaActual = -1;
            return;
        }

        respuestasRonda.clear();
        tiemposRonda.clear();
        esperandoRespuestas = true;

        Pregunta p = listaPreguntas.get(indicePreguntaActual);

        broadcast("PREGUNTA:" + p.getEnunciado(), null);
        broadcast("OPCION_A:A) " + p.getOpciones()[0], null);
        broadcast("OPCION_B:B) " + p.getOpciones()[1], null);
        broadcast("OPCION_C:C) " + p.getOpciones()[2], null);
        broadcast("OPCION_D:D) " + p.getOpciones()[3], null);
        broadcast("RESPONDE:", null);

        tiempoInicioPregunta = System.currentTimeMillis();
        System.out.println("Pregunta " + (indicePreguntaActual + 1) + " lanzada: " + p.getEnunciado());
    }

    public static synchronized void procesarRespuesta(ClienteHandler cliente, String respuesta) {
        if (!esperandoRespuestas) {
            cliente.sendMessage("ESPERA:Espera a la siguiente pregunta");
            return;
        }
        if (respuestasRonda.containsKey(cliente)) {
            cliente.sendMessage("ERROR:Ya has respondido a esta pregunta");
            return;
        }

        long tiempo = System.currentTimeMillis() - tiempoInicioPregunta;
        respuestasRonda.put(cliente, respuesta);
        tiemposRonda.put(cliente, tiempo);

        cliente.sendMessage("RECIBIDO:Respuesta " + respuesta + " recibida en " + tiempo + "ms");
        System.out.println(cliente.getNombreCliente() + " respondio " + respuesta);

        if (respuestasRonda.size() == clientes.size()) {
            esperandoRespuestas = false;
            enviarRanking();
        }
    }

    private static void enviarRanking() {
        broadcast("RANKING:=== RANKING ===", null);
        Pregunta p = listaPreguntas.get(indicePreguntaActual);
        String letraCorrecta = p.getLetraCorrecta();

        for (ClienteHandler c : clientes) {
            String resp = respuestasRonda.get(c);
            long tiempo = tiemposRonda.get(c);
            boolean acerto = resp.equalsIgnoreCase(letraCorrecta);
            String estado = acerto ? "CORRECTO" : "INCORRECTO";

            broadcast("RANKING:" + c.getNombreCliente() + " - " + resp + " (" + tiempo + "ms) " + estado, null);
        }
        broadcast("RANKING:==================", null);
        System.out.println("Ronda finalizada. Esperando NEXT...");
    }

    public static void broadcast(String mensaje, ClienteHandler remitente) {
        for (ClienteHandler cliente : clientes) {
            if (cliente != remitente) {
                cliente.sendMessage(mensaje);
            }
        }
    }
}