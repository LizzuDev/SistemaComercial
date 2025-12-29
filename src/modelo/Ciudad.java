package modelo;

public class Ciudad {
    private String id;
    private String nombre;

    public Ciudad(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() { return id; }

    @Override
    public String toString() {
        return nombre; // Esto es lo que se verá en el ComboBox
    }
}