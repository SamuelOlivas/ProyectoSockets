package comunes;

public class Pregunta {
    private String enunciado;
    private String[] opciones; // Array de 4 opciones
    private int indiceCorrecta; // 0=A, 1=B, 2=C, 3=D

    public Pregunta(String enunciado, String[] opciones, int indiceCorrecta) {
        this.enunciado = enunciado;
        this.opciones = opciones;
        this.indiceCorrecta = indiceCorrecta;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public String[] getOpciones() {
        return opciones;
    }

    public int getIndiceCorrecta() {
        return indiceCorrecta;
    }

    // Helper para convertir 0->A, 1->B...
    public String getLetraCorrecta() {
        return String.valueOf((char) ('A' + indiceCorrecta));
    }
}