package modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class GestionCiudades {
    public List<Ciudad> obtenerCiudades() {
        List<Ciudad> lista = new ArrayList<>();
        String sql = "SELECT id_Ciudad, ciu_descripcion FROM CIUDADES ORDER BY ciu_descripcion";
        
        try (Connection con = PruebaJDBC.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             
            while(rs.next()) {
                lista.add(new Ciudad(rs.getString("id_Ciudad"), rs.getString("ciu_descripcion")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }
}