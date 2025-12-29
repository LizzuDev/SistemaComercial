package modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GestionProveedores {

    // --- CARGAR LISTA USANDO VISTA SQL ---
    public List<Object[]> obtenerTodosLosProveedores() {
        List<Object[]> datos = new ArrayList<>();
        String sql = "SELECT * FROM V_INFO_PROVEEDORES ORDER BY id_Proveedor ASC";

        try (Connection con = PruebaJDBC.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Object[] fila = new Object[7];
                fila[0] = rs.getString("id_Proveedor");
                
                String nombre = rs.getString("prv_Nombre");
                fila[1] = (nombre != null ? nombre : "Desconocido") + "#1";
                
                String ruc = rs.getString("prv_RUC_CED");
                fila[2] = (ruc != null) ? ruc : "---";
                
                String cel = rs.getString("prv_Celular");
                String tel = rs.getString("prv_Telefono");
                fila[3] = (cel != null && !cel.isEmpty()) ? cel : (tel != null ? tel : "---");
                
                String mail = rs.getString("prv_Mail");
                fila[4] = (mail != null) ? mail : "";
                
                String ciudad = rs.getString("ciu_descripcion");
                fila[5] = (ciudad != null) ? ciudad : "Sin Asignar"; 
                
                fila[6] = ""; 
                datos.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("Error cargando vista: " + e.getMessage());
            e.printStackTrace();
        }
        return datos;
    }

    public Proveedor obtenerProveedorPorId(String id) {
        Proveedor p = null;
        String sql = "SELECT * FROM PROVEEDORES WHERE id_Proveedor = ?";
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id); 
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                p = new Proveedor(
                    rs.getString("id_Proveedor"), 
                    rs.getString("prv_Nombre"), 
                    rs.getString("prv_RUC_CED"), 
                    rs.getString("prv_Telefono"), 
                    rs.getString("prv_Celular"), 
                    rs.getString("prv_Mail"), 
                    rs.getString("prv_Direccion"), 
                    rs.getString("id_Ciudad")
                );
            }
        } catch (SQLException e) { e.printStackTrace(); } 
        return p;
    }

    public String generarSiguienteId() {
        int maxNumero = 0;
        String sql = "SELECT id_Proveedor FROM PROVEEDORES";
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id_Proveedor");
                if (id != null && id.startsWith("PRV-")) {
                    try { int n = Integer.parseInt(id.substring(4)); if (n > maxNumero) maxNumero = n; } catch (Exception e) {}
                }
            }
        } catch (Exception e) {} return String.format("PRV-%03d", maxNumero + 1);
    }

    public boolean existeProveedorConNombre(String nombre, String idExcluir) {
        String sql = "SELECT COUNT(*) FROM PROVEEDORES WHERE prv_Nombre = ? AND ESTADO_PRV = 'ACT'";
        if (idExcluir != null) sql += " AND id_Proveedor != ?";
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre); if (idExcluir != null) ps.setString(2, idExcluir);
            ResultSet rs = ps.executeQuery(); if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {} return false;
    }

    public boolean existeProveedorConRUC(String ruc, String idExcluir) {
        String sql = "SELECT COUNT(*) FROM PROVEEDORES WHERE prv_RUC_CED = ? AND ESTADO_PRV = 'ACT'";
        if (idExcluir != null) sql += " AND id_Proveedor != ?";
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ruc); if (idExcluir != null) ps.setString(2, idExcluir);
            ResultSet rs = ps.executeQuery(); if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {} return false;
    }

    // --- INSERTAR PROVEEDOR (CON LOG) ---
    public boolean registrarProveedor(String id, String nombre, String ruc, String tel, String cel, String mail, String dir, String idCiudad) {
        String sql = "INSERT INTO PROVEEDORES (id_Proveedor, prv_Nombre, prv_RUC_CED, prv_Telefono, prv_Celular, prv_Mail, prv_Direccion, id_Ciudad, ESTADO_PRV) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACT')";
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id); 
            ps.setString(2, nombre); 
            ps.setString(3, ruc); 
            ps.setString(4, tel); 
            ps.setString(5, cel); 
            ps.setString(6, mail); 
            ps.setString(7, dir); 
            ps.setString(8, idCiudad);
            
            boolean exito = ps.executeUpdate() > 0;
            if (exito) {
                Logger.registrar("PROVEEDORES", id, "INSERT", "Nuevo proveedor: " + nombre);
            }
            return exito;
            
        } catch (Exception e) { 
            System.err.println("ERROR SQL AL INSERTAR: " + e.getMessage());
            e.printStackTrace();
            return false; 
        }
    }

    // --- ACTUALIZAR PROVEEDOR (CON LOG) ---
    public boolean actualizarProveedor(Proveedor p) {
        String sql = "UPDATE PROVEEDORES SET prv_Nombre = ?, prv_RUC_CED = ?, prv_Telefono = ?, prv_Celular = ?, prv_Mail = ?, prv_Direccion = ?, id_Ciudad = ? WHERE id_Proveedor = ?";
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre()); 
            ps.setString(2, p.getRuc()); 
            ps.setString(3, p.getTelefono()); 
            ps.setString(4, p.getCelular()); 
            ps.setString(5, p.getEmail()); 
            ps.setString(6, p.getDireccion()); 
            ps.setString(7, p.getIdCiudad()); 
            ps.setString(8, p.getId()); 
            
            boolean exito = ps.executeUpdate() > 0;
            if (exito) {
                Logger.registrar("PROVEEDORES", p.getId(), "UPDATE", "Datos actualizados");
            }
            return exito;
            
        } catch (Exception e) { return false; }
    }

    // --- ELIMINAR PROVEEDOR (CON LOG) ---
    public boolean eliminarProveedorLogico(String id) {
        String sql = "UPDATE PROVEEDORES SET ESTADO_PRV = 'INA' WHERE id_Proveedor = ?";
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement(sql)) { 
            ps.setString(1, id); 
            
            boolean exito = ps.executeUpdate() > 0;
            if (exito) {
                Logger.registrar("PROVEEDORES", id, "DELETE", "Cambio a estado Inactivo");
            }
            return exito;
            
        } catch (Exception e) { return false; }
    }
}