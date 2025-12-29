package modelo;

public class Usuario {
    private int idUsuario;
    private String username;
    private String nombreCompleto; // Para decir "Hola, Ana Perez"
    private String idRol;          // Para los permisos (ROL-ANA, ROL-ADM...)

    public Usuario(int idUsuario, String username, String nombreCompleto, String idRol) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.nombreCompleto = nombreCompleto;
        this.idRol = idRol;
    }

    public String getNombreCompleto() { return nombreCompleto; }
    public String getId() {
        return this.username; // O return this.nick; (Depende de cómo identifiques al usuario)
    }
    
    public String getIdRol() {
        return this.idRol;
    }
}