# 1. Usamos una imagen base que ya tiene Java 21 instalado
FROM openjdk:21-jdk-slim

# 2. Creamos una carpeta de trabajo dentro del contenedor
WORKDIR /app

# 3. Copiamos nuestros archivos al contenedor
COPY servidor.jar .
COPY blooket.csv .
COPY myserver.jks .

# 4. Abrimos el puerto 8080
EXPOSE 8080

# 5. Comando para arrancar el servidor
CMD ["java", "-jar", "servidor.jar"]
