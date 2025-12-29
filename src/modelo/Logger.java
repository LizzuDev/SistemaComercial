package modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Logger {

    public static void registrar(String tabla, String idRegistro, String accion, String descripcion) {
        // 1. Obtener el usuario actual de forma segura
        String usuario = "SISTEMA";
        if (Sesion.getUsuarioActual() != null) {
            usuario = Sesion.getUsuarioActual().getId(); 
        }

        String sql = "INSERT INTO historial_logs (usuario, tabla_afectada, id_registro, accion, descripcion, fecha_hora) " +
                     "VALUES (?, ?, ?, ?, ?, GETDATE())";

        try (Connection con = PruebaJDBC.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario);
            ps.setString(2, tabla);
            ps.setString(3, idRegistro);
            ps.setString(4, accion);
            ps.setString(5, descripcion);

            ps.executeUpdate();
            // No mostramos mensaje de éxito para no interrumpir el flujo visual del usuario
            System.out.println("[LOG] Acción registrada: " + accion + " en " + tabla);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[LOG ERROR] No se pudo guardar el log.");
        }
    }
}