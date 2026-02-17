<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/SSL-Sockets-2CA5E0?style=for-the-badge&logo=letsencrypt&logoColor=white"/>
  <img src="https://img.shields.io/badge/FTP-Preguntas-00C853?style=for-the-badge&logo=files&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
</p>

<h1 align="center">🎮 ProyectoSockets — Kahoot por Consola</h1>

<p align="center">
  <b>Un juego multijugador de preguntas y respuestas tipo Kahoot, construido desde cero con Java Sockets sobre SSL.</b><br>
  Proyecto de la asignatura <i>Programación de Servicios y Procesos</i> — 2º DAM.
</p>

---

## 📖 ¿Qué es?

Un clon de Kahoot que funciona completamente por **terminal**. Un servidor lanza preguntas a todos los jugadores conectados, estos responden (A, B, C o D) y se genera un **ranking en tiempo real** con tiempos de respuesta.

Todo esto usando **comunicación cifrada SSL**, protocolo **HTTP simulado con JSON** y carga de preguntas desde un **servidor FTP** remoto (con fallback a archivo local).

---

## ⚙️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                      SERVIDOR (SSL)                         │
│                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌────────────────┐  │
│  │  FTP / Local  │──▶│  Preguntas   │──▶│   Broadcast    │  │
│  │  (blooket.csv)│   │  (Pregunta)  │   │  a jugadores   │  │
│  └──────────────┘   └──────────────┘   └────────────────┘  │
│                            │                                │
│                    Comando "NEXT"                            │
│                   (consola admin)                            │
└──────────────────────────┬──────────────────────────────────┘
                           │ SSL Socket (puerto 8080)
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
   │  Cliente 1  │  │  Cliente 2  │  │  Cliente N  │
   │  (Jugador)  │  │  (Jugador)  │  │  (Jugador)  │
   └─────────────┘  └─────────────┘  └─────────────┘
```

---

## 🧩 Estructura del proyecto

```
ProyectoSockets/
├── src/
│   ├── cliente/
│   │   └── Cliente.java          # Cliente SSL que se conecta al servidor
│   ├── comunes/
│   │   └── Pregunta.java         # Modelo de datos para las preguntas
│   └── servidor/
│       ├── Servidor.java         # Servidor SSL + lógica del juego
│       └── ClienteHandler.java   # Manejo individual de cada jugador
├── blooket.csv                   # Banco de preguntas (formato Blooket)
├── myserver.jks                  # Keystore SSL para cifrado
├── Dockerfile                    # Despliegue en contenedor Docker
├── manifest.txt                  # Manifest del JAR
└── servidor.jar                  # JAR compilado del servidor
```

---

## 🚀 Cómo ejecutar

### Servidor

```bash
java -jar servidor.jar
```

> El servidor escucha en el **puerto 8080** con SSL.
> Usa el comando `NEXT` en la consola del servidor para lanzar preguntas.

### Cliente

```bash
# Compilar
javac -d bin src/comunes/Pregunta.java src/cliente/Cliente.java

# Ejecutar
java -cp bin cliente.Cliente
```

> El cliente pedirá un **nombre de jugador** y se conectará automáticamente.

### Con Docker

```bash
# Construir la imagen
docker build -t kahoot-server .

# Levantar el contenedor
docker run -it -p 8080:8080 kahoot-server
```

---

## 🎯 Flujo de juego

| Paso | Acción                                           | Quién    |
| ---- | ------------------------------------------------ | -------- |
| 1    | El jugador introduce su **nombre** y se conecta  | Cliente  |
| 2    | Se envía una petición **HTTP + JSON** simulada   | Cliente  |
| 3    | El servidor registra al jugador y espera         | Servidor |
| 4    | El admin escribe `NEXT` para lanzar una pregunta | Admin    |
| 5    | Los jugadores responden con **A, B, C o D**      | Cliente  |
| 6    | Se genera un **ranking** con aciertos y tiempos  | Servidor |
| 7    | Repetir hasta acabar las preguntas               | Todos    |

---

## 🔐 Tecnologías y conceptos aplicados

| Tecnología         | Uso en el proyecto                                    |
| ------------------ | ----------------------------------------------------- |
| **Java Sockets**   | Comunicación cliente-servidor en tiempo real          |
| **SSL / TLS**      | Cifrado de toda la comunicación                       |
| **Multithreading** | Cada cliente se gestiona en su propio hilo            |
| **FTP**            | Descarga remota del banco de preguntas                |
| **HTTP + JSON**    | Petición simulada al conectarse (requisito académico) |
| **Docker**         | Despliegue del servidor en contenedor                 |
| **CSV Parsing**    | Lectura y parseo del archivo de preguntas             |
| **Broadcast**      | Envío simultáneo de mensajes a todos los jugadores    |

---

## 📝 Formato del CSV de preguntas

El archivo `blooket.csv` sigue el formato de exportación de [Blooket](https://www.blooket.com/):

```
..., Pregunta, OpciónA, OpciónB, OpciónC, OpciónD, ..., RespuestaCorrecta, ...
```

> La columna 7 indica el número de la respuesta correcta (1-4).

---

## 📌 Notas

- Para salir del juego como cliente: escribe `/salir`
- El servidor intenta primero cargar preguntas por **FTP**; si falla, usa el **archivo local**
- Si se usa Docker con `docker attach`, para salir sin parar el contenedor: `Ctrl+P` → `Ctrl+Q`
