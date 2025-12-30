package modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GestionUsuarios {

    public Usuario login(String user, String pass) {
        Usuario usuarioEncontrado = null;
        
        // --- CONSULTA ACTUALIZADA AL NUEVO SCRIPT ---
        // Ya no necesitamos JOINS porque el nombre real y el rol están en la tabla USUARIOS
        String sql = "SELECT id_Usuario_PK, nombre_usuario, usu_NombreReal, id_RolSistema " +
                     "FROM USUARIOS " +
                     "WHERE nombre_usuario = ? AND clave = ? AND usu_Estado = 'ACT'";

        try (Connection con = PruebaJDBC.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Enviamos los datos a la consulta
            ps.setString(1, user);
            ps.setString(2, pass);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Si entra aquí, ¡Credenciales Correctas!
                    // Creamos el objeto con los nombres de columnas exactos de tu nueva BD
                    usuarioEncontrado = new Usuario(
                        rs.getInt("id_Usuario_PK"),      // Antes id_Usuario
                        rs.getString("nombre_usuario"),  // Antes usu_NombreUsuario
                        rs.getString("usu_NombreReal"),  // Antes venía de Empleados (NombreCompleto)
                        rs.getString("id_RolSistema")    // Antes id_Rol
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error Login: " + e.getMessage());
        }
        return usuarioEncontrado;
    }
}