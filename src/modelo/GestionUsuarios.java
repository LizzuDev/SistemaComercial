package modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GestionUsuarios {

    public Usuario login(String user, String pass) {
        Usuario usuarioEncontrado = null;
        
        // Consulta que une USUARIOS, EMPLEADOS y ROLES
        String sql = "SELECT u.id_Usuario, u.usu_NombreUsuario, " +
                     "e.emp_Nombre1 + ' ' + e.emp_Apellido1 AS NombreCompleto, " +
                     "r.id_Rol " +
                     "FROM USUARIOS u " +
                     "INNER JOIN Empleados e ON u.id_Empleado = e.id_Empleado " +
                     "INNER JOIN Roles r ON e.id_Rol = r.id_Rol " +
                     "WHERE u.usu_NombreUsuario = ? AND u.usu_Clave = ? AND u.ESTADO_USU = 'ACT'";

        try (Connection con = PruebaJDBC.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Enviamos los datos a la consulta
            ps.setString(1, user);
            ps.setString(2, pass);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Si entra aquí, ¡Credenciales Correctas!
                    usuarioEncontrado = new Usuario(
                        rs.getInt("id_Usuario"),
                        rs.getString("usu_NombreUsuario"),
                        rs.getString("NombreCompleto"),
                        rs.getString("id_Rol") // Aquí capturamos el rol (ej: ROL-ANA)
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error Login: " + e.getMessage());
        }
        return usuarioEncontrado;
    }
}