package modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.swing.table.DefaultTableModel;

public class GestionOrdenCompra {

    public String generarIdOrden() {
        int siguienteNumero = 1;
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(id_Compra, 4, 20) AS INT)), 0) FROM COMPRAS WHERE id_Compra LIKE 'OC-%'";

        try (Connection con = PruebaJDBC.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                siguienteNumero = rs.getInt(1) + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
        return String.format("OC-%04d", siguienteNumero);
    }

    public boolean registrarOrdenCompra(String idCompra, String idProveedor, LocalDate fecha, 
                                        double subtotal, int porcentajeIva, 
                                        DefaultTableModel modeloTabla, String estadoOrden) {
        Connection con = null;
        try {
            con = PruebaJDBC.getConexion();
            con.setAutoCommit(false); 

            if (existeId(con, idCompra)) {
                throw new SQLException("El ID " + idCompra + " ya existe. Intente nuevamente.");
            }

            // CABECERA
            String sqlCab = "INSERT INTO COMPRAS (id_Compra, id_Proveedor, oc_Fecha_Hora, oc_Subtotal, oc_IVA, ESTADO_OC) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psCab = con.prepareStatement(sqlCab)) {
                psCab.setString(1, idCompra);
                psCab.setString(2, idProveedor);
                psCab.setDate(3, java.sql.Date.valueOf(fecha));
                psCab.setDouble(4, subtotal);
                psCab.setInt(5, porcentajeIva); 
                psCab.setString(6, estadoOrden);
                psCab.executeUpdate();
            }

            // DETALLES
            String sqlDet = "INSERT INTO PROXOC (id_Compra, id_Producto, pxo_Cantidad, pxo_Valor, ESTADO_PxOC) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement psDet = con.prepareStatement(sqlDet)) {
                for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                    // Índices de la tabla (0:Botón, 1:ID, 2:Prod, 3:Cant, 4:Precio)
                    String idProd = modeloTabla.getValueAt(i, 1).toString(); 
                    
                    // --- CORRECCIÓN CRÍTICA AQUÍ ---
                    // Usamos toString() antes de parsear para evitar ClassCastException
                    int cantidad = Integer.parseInt(modeloTabla.getValueAt(i, 3).toString());
                    double precio = Double.parseDouble(modeloTabla.getValueAt(i, 4).toString());

                    psDet.setString(1, idCompra);
                    psDet.setString(2, idProd);
                    psDet.setInt(3, cantidad);
                    psDet.setDouble(4, precio);
                    psDet.setString(5, estadoOrden); 
                    psDet.addBatch();
                }
                psDet.executeBatch();
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try { if (con != null) con.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }
    
    private boolean existeId(Connection con, String id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM COMPRAS WHERE id_Compra = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    // Método privado auxiliar para insertar el asiento y sus detalles
    // Método privado auxiliar para insertar el asiento y sus detalles
    private void generarAsientoContable(Connection con, String idCompra, String nombreProveedor, double subtotal, double iva, double total) throws SQLException {
        
        // 1. Generar ID con el algoritmo A-XXXXX
        String idAsiento = generarIdAsientoDesdeCompra(idCompra); 
        
        // 2. Obtener Usuario
        String usuarioResponsable = "SISTEMA";
        if (Sesion.getUsuarioActual() != null) {
             // Asegúrate de usar el método correcto de tu clase Usuario (.getId(), .getNombre(), etc)
             usuarioResponsable = Sesion.getUsuarioActual().getId(); 
        }

        // --- PASO DE SEGURIDAD: LIMPIEZA PREVIA ---
        // Si ya existía un asiento para esta compra (por re-aprobación), lo borramos primero
        // para evitar el error "Violation of PRIMARY KEY".
        try (PreparedStatement psDelDet = con.prepareStatement("DELETE FROM CTAxASI WHERE id_Asiento = ?")) {
            psDelDet.setString(1, idAsiento);
            psDelDet.executeUpdate();
        }
        try (PreparedStatement psDelCab = con.prepareStatement("DELETE FROM ASIENTOS WHERE id_Asiento = ?")) {
            psDelCab.setString(1, idAsiento);
            psDelCab.executeUpdate();
        }
        // ------------------------------------------
        
        // 3. INSERTAR CABECERA (ASIENTOS)
        String sqlCabecera = "INSERT INTO ASIENTOS (id_Asiento, asi_Descripcion, asi_Total_Debe, asi_Total_Haber, asi_FechaHora, USER_ID, ESTADO_ASI) " +
                             "VALUES (?, ?, ?, ?, GETDATE(), ?, 'ACT')";
        
        try (PreparedStatement psCab = con.prepareStatement(sqlCabecera)) {
            psCab.setString(1, idAsiento);
            psCab.setString(2, "Ref Compra: " + idCompra); // Descripción corta
            psCab.setDouble(3, total); 
            psCab.setDouble(4, total); 
            psCab.setString(5, usuarioResponsable);
            psCab.executeUpdate();
        }

        // 4. INSERTAR DETALLES (CTAxASI)
        String sqlDetalle = "INSERT INTO CTAxASI (id_Asiento, id_Cuenta, cxa_Debe, cxa_Haber, ESTADO_CXA) VALUES (?, ?, ?, ?, 'ACT')";
        
        try (PreparedStatement psDet = con.prepareStatement(sqlDetalle)) {
            
            // --- MOVIMIENTO 1: INVENTARIO (DEBE) ---
            psDet.setString(1, idAsiento);
            psDet.setString(2, "1.1.03.01.01"); // Cuenta: Inventarios Prod Terminado
            psDet.setDouble(3, subtotal);
            psDet.setDouble(4, 0.00);
            psDet.addBatch();

            // --- MOVIMIENTO 2: IVA (DEBE) ---
            if (iva > 0.01) {
                psDet.setString(1, idAsiento);
                psDet.setString(2, "1.1.05.01.01"); // Cuenta: IVA en Compras
                psDet.setDouble(3, iva);
                psDet.setDouble(4, 0.00);
                psDet.addBatch();
            }

            // --- MOVIMIENTO 3: PROVEEDOR (HABER) ---
            psDet.setString(1, idAsiento);
            psDet.setString(2, "2.1.03.01.01"); // Cuenta: Proveedores Locales
            psDet.setDouble(3, 0.00);
            psDet.setDouble(4, total);
            psDet.addBatch();

            psDet.executeBatch();
        }
    }
    
    // Algoritmo para convertir "OC-00001" en "A-00001" (Máximo 7 caracteres)
    private String generarIdAsientoDesdeCompra(String idCompra) {
        // 1. Quitar todo lo que no sea número (OC-001 -> 001)
        String soloNumeros = idCompra.replaceAll("[^0-9]", "");
        
        // 2. Si no hay números (ej. error de data), usar un default
        if (soloNumeros.isEmpty()) return "A-ERR";

        // 3. Tomar solo los últimos 5 dígitos para asegurar que quepa en CHAR(7)
        // Ejemplo: Si es 1000025, tomamos 00025.
        if (soloNumeros.length() > 5) {
            soloNumeros = soloNumeros.substring(soloNumeros.length() - 5);
        }
        
        // 4. Retornar formato A-XXXXX
        return "A-" + soloNumeros;
    }
    
    public boolean aprobarCompraConAsiento(String idCompra, String nombreProv, double sub, double iva, double total) {
        Connection con = null;
        try {
            con = PruebaJDBC.getConexion();
            con.setAutoCommit(false); // INICIO TRANSACCIÓN

            // 1. Actualizar Estado Cabecera Compra
            try (PreparedStatement ps1 = con.prepareStatement("UPDATE COMPRAS SET ESTADO_OC = 'APR' WHERE id_Compra = ?")) {
                ps1.setString(1, idCompra);
                ps1.executeUpdate();
            }

            // 2. Actualizar Estado Líneas Detalle
            try (PreparedStatement ps2 = con.prepareStatement("UPDATE PROXOC SET ESTADO_PxOC = 'APR' WHERE id_Compra = ?")) {
                ps2.setString(1, idCompra);
                ps2.executeUpdate();
            }

            // 3. GENERAR ASIENTO CONTABLE (Llamada al método privado del paso 2)
            generarAsientoContable(con, idCompra, nombreProv, sub, iva, total);

            con.commit(); // CONFIRMAR TODO
            return true;

        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) {}
            }
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (SQLException e) {}
        }
    }
}