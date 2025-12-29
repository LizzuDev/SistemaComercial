package modelo;

public class Sesion {
    private static Usuario usuarioActual;

    public static void setUsuarioActual(Usuario u) {
        usuarioActual = u;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }
}