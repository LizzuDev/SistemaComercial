package modelo;

public class Proveedor {
    private String id, nombre, ruc, telefono, celular, email, direccion, idCiudad;

    public Proveedor(String id, String nombre, String ruc, String telefono, String celular, String email, String direccion, String idCiudad) {
        this.id = id;
        this.nombre = nombre;
        this.ruc = ruc;
        this.telefono = telefono;
        this.celular = celular;
        this.email = email;
        this.direccion = direccion;
        this.idCiudad = idCiudad;
    }

    // Getters
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getRuc() { return ruc; }
    public String getTelefono() { return telefono; }
    public String getCelular() { return celular; }
    public String getEmail() { return email; }
    public String getDireccion() { return direccion; }
    public String getIdCiudad() { return idCiudad; }
}