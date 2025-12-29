package modelo;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

public class DetalleCompra extends JDialog {

    private int xMouse, yMouse;
    private String idCompra;
    private String estadoActual = "";
    private boolean tienePermisosEscritura;

    // Componentes UI
    private JLabel lblTitulo;
    private JLabel lblSubtotal, lblIVA, lblTotal; 
    private JComboBox<String> cmbIVA; 
    private JTextField txtOrden, txtEstado;
    
    // Componentes Editables
    private JComboBox<ProveedorItem> cmbProveedor; 
    private DatePicker datePicker;                 

    private JTable tablaDetalles;
    private DefaultTableModel modeloTabla;

    // Botones
    private JButton btnGuardar, btnAprobar, btnAnular, btnRestaurar, btnCerrar;
    private JButton btnAgregarProd;

    // Fuentes y Colores
    private Font fontBold = new Font("SansSerif", Font.BOLD, 14);
    private Font fontPlain = new Font("SansSerif", Font.PLAIN, 13);
    private Font fontBig = new Font("SansSerif", Font.BOLD, 20);
    private Color colorRojo = new Color(220, 53, 69);
    private Color colorBlanco = Color.WHITE;

    public DetalleCompra(java.awt.Frame parent, boolean modal, String idCompra, boolean tienePermiso) {
        super(parent, modal);
        this.idCompra = idCompra;
        this.tienePermisosEscritura = tienePermiso;

        setUndecorated(true);
        setSize(850, 680); 
        setLocationRelativeTo(parent);
        setBackground(new Color(0, 0, 0, 0));

        analizarEstadoInicial();
        construirInterfaz();
        cargarDetallesTabla(); 
        controlarEstadoBotones();
    }

    private void analizarEstadoInicial() {
        try (Connection con = PruebaJDBC.getConexion(); 
             PreparedStatement ps = con.prepareStatement("SELECT ESTADO_OC FROM COMPRAS WHERE id_Compra = ?")) {
            ps.setString(1, this.idCompra);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) estadoActual = rs.getString("ESTADO_OC");
        } catch (Exception e) { e.printStackTrace(); }
        if (estadoActual == null) estadoActual = "";
    }

    private void construirInterfaz() {
        JPanel panelFondo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(5, 12, 35), 0, getHeight(), new Color(2, 5, 20));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
            }
        };
        panelFondo.setLayout(null);
        setContentPane(panelFondo);
        agregarMovimientoVentana(panelFondo);

        // --- TÍTULO ---
        lblTitulo = new JLabel("Detalle de Orden de Compra");
        lblTitulo.setFont(fontBig);
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setBounds(30, 20, 400, 30);
        panelFondo.add(lblTitulo);

        // --- CABECERA ---
        JPanel panelCabecera = crearPanelGlass(30, 60, 790, 80);
        panelFondo.add(panelCabecera);

        // 1. ORDEN (Solo lectura)
        crearEtiqueta("N° Orden:", 20, 10, panelCabecera);
        txtOrden = crearInputLectura(20, 30, 120, panelCabecera);
        txtOrden.setText(this.idCompra);

        // 2. PROVEEDOR (COMBOBOX)
        crearEtiqueta("Proveedor:", 160, 10, panelCabecera);
        cmbProveedor = new JComboBox<>();
        cmbProveedor.setBounds(160, 30, 300, 30);
        cargarProveedores(); 
        panelCabecera.add(cmbProveedor);

        // 3. FECHA (DATEPICKER)
        crearEtiqueta("Fecha:", 480, 10, panelCabecera);
        
        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setFormatForDatesCommonEra("dd/MM/yyyy");
        dateSettings.setAllowKeyboardEditing(false);
        
        // 1. PRIMERO construimos el componente
        datePicker = new DatePicker(dateSettings); 
        
        // 2. DESPUÉS aplicamos el límite (Ahora sí funciona porque datePicker ya existe)
        dateSettings.setDateRangeLimits(null, LocalDate.now()); 
        
        datePicker.setBounds(480, 30, 150, 30);
        panelCabecera.add(datePicker);
        
        // 4. ESTADO (Solo lectura)
        crearEtiqueta("Estado:", 650, 10, panelCabecera);
        txtEstado = crearInputLectura(650, 30, 100, panelCabecera);
        txtEstado.setText(estadoActual);

        // --- BOTÓN AGREGAR PRODUCTO ---
        btnAgregarProd = new JButton("+ Agregar Producto");
        btnAgregarProd.setBounds(30, 150, 150, 30);
        btnAgregarProd.setBackground(new Color(0, 100, 150));
        btnAgregarProd.setForeground(Color.WHITE);
        btnAgregarProd.setFont(fontBold.deriveFont(12f));
        btnAgregarProd.addActionListener(e -> mostrarDialogoProducto());
        btnAgregarProd.setVisible(tienePermisosEscritura && (estadoActual.startsWith("ABI"))); 
        panelFondo.add(btnAgregarProd);

        // --- TABLA ---
        JScrollPane scrollTabla = new JScrollPane();
        scrollTabla.setBounds(30, 190, 790, 270); 
        scrollTabla.getViewport().setBackground(new Color(30, 30, 40));
        scrollTabla.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 30)));

        modeloTabla = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 && tienePermisosEscritura && (estadoActual.startsWith("ABI"));
            }
        };

        modeloTabla.addColumn("Acción");      
        modeloTabla.addColumn("ID Producto"); 
        modeloTabla.addColumn("Descripción"); 
        modeloTabla.addColumn("Cantidad");    
        modeloTabla.addColumn("Valor");       
        modeloTabla.addColumn("Subtotal");    
        modeloTabla.addColumn("Estado Prod"); 

        tablaDetalles = new JTable(modeloTabla);
        estilizarTabla(tablaDetalles);
        scrollTabla.setViewportView(tablaDetalles);
        panelFondo.add(scrollTabla);

        if (!tienePermisosEscritura || !estadoActual.startsWith("ABI")) {
            TableColumn colEliminar = tablaDetalles.getColumnModel().getColumn(0);
            tablaDetalles.removeColumn(colEliminar);
        }

        // --- FOOTER ---
        int yTotales = 480;
        int xLabels = 550;
        int xValues = 650;
        int gap = 30;

        JLabel lblTxtSub = new JLabel("Subtotal:");
        lblTxtSub.setFont(fontBold);
        lblTxtSub.setForeground(new Color(200, 200, 200));
        lblTxtSub.setHorizontalAlignment(JLabel.RIGHT);
        lblTxtSub.setBounds(xLabels, yTotales, 90, 20);
        panelFondo.add(lblTxtSub);

        lblSubtotal = new JLabel("0.00");
        lblSubtotal.setFont(fontBold);
        lblSubtotal.setForeground(Color.WHITE);
        lblSubtotal.setHorizontalAlignment(JLabel.RIGHT);
        lblSubtotal.setBounds(xValues, yTotales, 140, 20);
        panelFondo.add(lblSubtotal);

        JLabel lblTxtIVA = new JLabel("IVA %:");
        lblTxtIVA.setFont(fontBold);
        lblTxtIVA.setForeground(new Color(200, 200, 200));
        lblTxtIVA.setHorizontalAlignment(JLabel.RIGHT);
        lblTxtIVA.setBounds(xLabels, yTotales + gap, 90, 25);
        panelFondo.add(lblTxtIVA);

        cmbIVA = new JComboBox<>(new String[]{"0", "12", "15"});
        cmbIVA.setSelectedItem("15"); 
        cmbIVA.setBounds(xValues + 80, yTotales + gap, 60, 25);
        cmbIVA.addActionListener(e -> calcularTotalesUI()); 
        panelFondo.add(cmbIVA);
        
        lblIVA = new JLabel("0.00");
        lblIVA.setFont(fontBold);
        lblIVA.setForeground(Color.WHITE);
        lblIVA.setHorizontalAlignment(JLabel.RIGHT);
        lblIVA.setBounds(xValues, yTotales + gap, 70, 25); 
        panelFondo.add(lblIVA);

        JLabel lblTxtTotal = new JLabel("TOTAL:");
        lblTxtTotal.setFont(fontBig);
        lblTxtTotal.setForeground(new Color(100, 255, 100));
        lblTxtTotal.setHorizontalAlignment(JLabel.RIGHT);
        lblTxtTotal.setBounds(xLabels, yTotales + (gap * 2), 90, 30);
        panelFondo.add(lblTxtTotal);

        lblTotal = new JLabel("0.00");
        lblTotal.setFont(fontBig);
        lblTotal.setForeground(new Color(100, 255, 100));
        lblTotal.setHorizontalAlignment(JLabel.RIGHT);
        lblTotal.setBounds(xValues, yTotales + (gap * 2), 140, 30);
        panelFondo.add(lblTotal);

        // --- BOTONES ---
        int yBotones = 600;

        btnCerrar = crearBoton("Cerrar", new Color(100, 100, 110), new Color(200, 200, 210));
        btnCerrar.setBounds(30, yBotones, 100, 40);
        btnCerrar.addActionListener(e -> dispose());
        panelFondo.add(btnCerrar);

        btnGuardar = crearBoton("💾 Guardar", new Color(0, 100, 200), Color.WHITE);
        btnGuardar.setBounds(390, yBotones, 120, 40);
        btnGuardar.addActionListener(e -> accionGuardarCambios());
        panelFondo.add(btnGuardar);

        btnAnular = crearBoton("✖ Anular", new Color(180, 50, 50), Color.WHITE);
        btnAnular.setBounds(520, yBotones, 110, 40);
        btnAnular.addActionListener(e -> accionAnular());
        panelFondo.add(btnAnular);

        btnRestaurar = crearBoton("↺ Restaurar", new Color(200, 140, 0), Color.WHITE);
        btnRestaurar.setBounds(520, yBotones, 120, 40);
        btnRestaurar.addActionListener(e -> accionRestaurar());
        panelFondo.add(btnRestaurar);

        btnAprobar = crearBoton("✔ Aprobar", new Color(0, 150, 80), Color.WHITE);
        btnAprobar.setBounds(640, yBotones, 130, 40);
        btnAprobar.addActionListener(e -> accionAprobar());
        panelFondo.add(btnAprobar);
    }

    // -------------------------------------------------------------------------
    // LÓGICA DE DATOS
    // -------------------------------------------------------------------------
    
    private void cargarDetallesTabla() {
        Connection con = null;
        try {
            con = PruebaJDBC.getConexion();

            // 1. Cargar datos de Cabecera
            String sqlCab = "SELECT c.oc_Fecha_Hora, c.ESTADO_OC, c.oc_IVA, c.id_Proveedor " +
                            "FROM COMPRAS c WHERE c.id_Compra = ?";
            PreparedStatement ps = con.prepareStatement(sqlCab);
            ps.setString(1, this.idCompra);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("oc_Fecha_Hora");
                if (ts != null) {
                    datePicker.setDate(ts.toLocalDateTime().toLocalDate());
                }
                
                String idProvGuardado = rs.getString("id_Proveedor");
                for (int i = 0; i < cmbProveedor.getItemCount(); i++) {
                    if (cmbProveedor.getItemAt(i).id.equals(idProvGuardado)) {
                        cmbProveedor.setSelectedIndex(i);
                        break;
                    }
                }
                
                String ivaGuardado = String.valueOf(rs.getInt("oc_IVA"));
                cmbIVA.setSelectedItem(ivaGuardado);
            }
            rs.close(); ps.close();

            // 2. Cargar Filas
            String sqlDet = "SELECT px.id_Producto, pr.pro_Descripcion, px.pxo_Cantidad, px.pxo_Valor, px.ESTADO_PxOC " +
                            "FROM PROXOC px " +
                            "INNER JOIN PRODUCTOS pr ON px.id_Producto = pr.id_Producto " +
                            "WHERE px.id_Compra = ?";
            ps = con.prepareStatement(sqlDet);
            ps.setString(1, this.idCompra);
            rs = ps.executeQuery();

            modeloTabla.setRowCount(0);
            while (rs.next()) {
                int cant = rs.getInt("pxo_Cantidad");
                double valor = rs.getDouble("pxo_Valor");
                double sub = cant * valor;

                modeloTabla.addRow(new Object[]{
                    "Eliminar",
                    rs.getString("id_Producto"),
                    rs.getString("pro_Descripcion"),
                    cant,
                    valor,
                    sub,
                    rs.getString("ESTADO_PxOC")
                });
            }
            calcularTotalesUI(); 

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    // --- ACCIONES DE GUARDADO ---
    private void accionGuardarCambios() {
        if (modeloTabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "La orden no puede quedar vacía.");
            return;
        }
        
        // --- VALIDACIÓN DE FECHA ---
        LocalDate fecha = datePicker.getDate();
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "La fecha no puede estar vacía.");
            return;
        }
        if (fecha.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Error: No se permite guardar una orden con fecha futura.");
            return;
        }
        // ---------------------------
        
        int confirm = JOptionPane.showConfirmDialog(this, "¿Guardar los cambios?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        Connection con = null;
        try {
            con = PruebaJDBC.getConexion();
            con.setAutoCommit(false); 

            // 1. Borrar detalles anteriores
            String sqlDel = "DELETE FROM PROXOC WHERE id_Compra = ?";
            PreparedStatement psDel = con.prepareStatement(sqlDel);
            psDel.setString(1, this.idCompra);
            psDel.executeUpdate();

            // 2. Insertar los nuevos
            String sqlIns = "INSERT INTO PROXOC (id_Compra, id_Producto, pxo_Cantidad, pxo_Valor, ESTADO_PxOC) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement psIns = con.prepareStatement(sqlIns);

            for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                psIns.setString(1, this.idCompra);
                psIns.setString(2, modeloTabla.getValueAt(i, 1).toString());
                psIns.setInt(3, Integer.parseInt(modeloTabla.getValueAt(i, 3).toString()));
                psIns.setDouble(4, Double.parseDouble(modeloTabla.getValueAt(i, 4).toString()));
                psIns.setString(5, "ABI");
                psIns.addBatch();
            }
            psIns.executeBatch();

            // 3. Actualizar Cabecera
            String sqlUpd = "UPDATE COMPRAS SET oc_Subtotal = ?, oc_IVA = ?, id_Proveedor = ?, oc_Fecha_Hora = ?, ESTADO_OC = 'ABI' WHERE id_Compra = ?";
            PreparedStatement psUpd = con.prepareStatement(sqlUpd);
            
            double sub = Double.parseDouble(lblSubtotal.getText().replace(",", "."));
            int ivaPerc = Integer.parseInt(cmbIVA.getSelectedItem().toString());
            String nuevoProv = ((ProveedorItem) cmbProveedor.getSelectedItem()).id;
            
            psUpd.setDouble(1, sub);
            psUpd.setInt(2, ivaPerc);
            psUpd.setString(3, nuevoProv);
            psUpd.setTimestamp(4, Timestamp.valueOf(fecha.atStartOfDay()));
            psUpd.setString(5, this.idCompra);
            psUpd.executeUpdate();

            con.commit();
            Logger.registrar("COMPRAS", this.idCompra, "UPDATE", "Modificación de productos/fecha/proveedor en OC");
            JOptionPane.showMessageDialog(this, "Orden actualizada correctamente.");
            cargarDetallesTabla(); 

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    private void controlarEstadoBotones() {
        if (estadoActual == null) return;

        boolean esAbierta = estadoActual.startsWith("ABI");
        boolean esEscritura = tienePermisosEscritura;

        btnRestaurar.setVisible(false);
        btnAnular.setVisible(true);
        btnAgregarProd.setVisible(esEscritura && esAbierta);

        cmbProveedor.setEnabled(esEscritura && esAbierta);
        datePicker.setEnabled(esEscritura && esAbierta);
        cmbIVA.setEnabled(esEscritura && esAbierta);

        if (estadoActual.startsWith("ANU")) { 
            txtEstado.setForeground(colorRojo);
            btnAprobar.setEnabled(false); btnGuardar.setEnabled(false);
            btnAnular.setVisible(false); btnRestaurar.setVisible(true);
        } else if (estadoActual.startsWith("APR")) { 
            txtEstado.setForeground(new Color(100, 255, 100));
            btnAprobar.setEnabled(false); btnGuardar.setEnabled(false);
            btnAnular.setEnabled(true);
        } else { // ABIERTO
            txtEstado.setForeground(new Color(255, 200, 0));
            btnAprobar.setEnabled(true); btnGuardar.setEnabled(true);
            btnAnular.setEnabled(true);
        }

        if (!esEscritura) {
            btnAprobar.setVisible(false); btnAnular.setVisible(false);
            btnRestaurar.setVisible(false); btnGuardar.setVisible(false);
        }
    }

    // --- DIÁLOGO PRODUCTO ---
    private void mostrarDialogoProducto() {
        JDialog dialogProd = new JDialog(this, "Agregar Item", true);
        dialogProd.setUndecorated(true);
        dialogProd.setSize(400, 350);
        dialogProd.setLocationRelativeTo(this);
        
        JPanel panelP = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(5, 12, 35), 0, getHeight(), new Color(2, 5, 20));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0);
            }
        };
        panelP.setLayout(null);
        dialogProd.setContentPane(panelP);

        JLabel lblP = new JLabel("Seleccionar Producto");
        lblP.setFont(fontBold.deriveFont(18f));
        lblP.setForeground(colorBlanco);
        lblP.setBounds(20, 20, 300, 30);
        panelP.add(lblP);

        crearEtiqueta("Producto:", 20, 70, panelP);
        JComboBox<ProductoItem> cmbProdPopup = new JComboBox<>();
        cmbProdPopup.setBounds(20, 95, 360, 30);
        cargarProductos(cmbProdPopup); 
        panelP.add(cmbProdPopup);

        crearEtiqueta("Cantidad:", 20, 140, panelP);
        JTextField txtCantPopup = new JTextField("1");
        txtCantPopup.setBounds(20, 165, 100, 30);
        txtCantPopup.setHorizontalAlignment(JTextField.CENTER);
        panelP.add(txtCantPopup);

        crearEtiqueta("Precio Unitario:", 140, 140, panelP);
        JTextField txtPrecioPopup = new JTextField("0.00");
        txtPrecioPopup.setBounds(140, 165, 100, 30);
        panelP.add(txtPrecioPopup);

        cmbProdPopup.addActionListener(e -> {
            ProductoItem item = (ProductoItem) cmbProdPopup.getSelectedItem();
            if (item != null) txtPrecioPopup.setText(String.valueOf(item.precio));
        });

        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setBounds(80, 250, 100, 35);
        btnAceptar.setBackground(new Color(40, 167, 69)); // Verde
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.addActionListener(e -> {
            agregarFilaATabla(cmbProdPopup, txtCantPopup, txtPrecioPopup);
            dialogProd.dispose();
        });
        panelP.add(btnAceptar);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setBounds(200, 250, 100, 35);
        btnCancelar.setBackground(new Color(100, 100, 110));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.addActionListener(e -> dialogProd.dispose());
        panelP.add(btnCancelar);

        dialogProd.setVisible(true);
    }

    private void agregarFilaATabla(JComboBox<ProductoItem> combo, JTextField txtC, JTextField txtP) {
        // 1. Validar selección
        ProductoItem prod = (ProductoItem) combo.getSelectedItem();
        if (prod == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto.");
            return;
        }

        // --- VALIDACIÓN DE DUPLICADOS ---
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            String idEnTabla = modeloTabla.getValueAt(i, 1).toString(); // Col 1 es ID
            if (idEnTabla.equals(prod.id)) {
                JOptionPane.showMessageDialog(this, 
                    "El producto '" + prod.nombre + "' ya está en la orden.\n" +
                    "Si desea cambiar la cantidad, elimine la línea y agréguela de nuevo.", 
                    "Producto Duplicado", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        // --------------------------------

        // 2. Validar Cantidad
        int cant = 0;
        try {
            cant = Integer.parseInt(txtC.getText().trim());
            if (cant <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida (debe ser entero > 0).");
            return;
        }

        // 3. Validar Precio
        double precioIngresado = 0.0;
        try {
            precioIngresado = Double.parseDouble(txtP.getText().trim().replace(",", "."));
            if (precioIngresado < 0) {
                JOptionPane.showMessageDialog(this, "El precio no puede ser negativo.");
                return;
            }
            if (precioIngresado < prod.precio) {
                JOptionPane.showMessageDialog(this, "El precio no puede ser menor al costo base ($" + prod.precio + ").");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Precio inválido.");
            return;
        }

        // 4. Agregar Fila (Visualmente)
        double totalLinea = cant * precioIngresado;
        modeloTabla.addRow(new Object[]{ "Eliminar", prod.id, prod.toString(), cant, precioIngresado, totalLinea, "ABI" });
        
        calcularTotalesUI();
        JOptionPane.showMessageDialog(this, "Producto agregado. Recuerde dar clic en 'Guardar' para confirmar cambios.");
    }

    private void calcularTotalesUI() {
        double subtotal = 0;
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            subtotal += Double.parseDouble(modeloTabla.getValueAt(i, 5).toString());
        }
        int ivaPorcentaje = Integer.parseInt(cmbIVA.getSelectedItem().toString());
        double ivaValor = subtotal * (ivaPorcentaje / 100.0);
        double total = subtotal + ivaValor;

        lblSubtotal.setText(String.format("%.2f", subtotal));
        lblIVA.setText(String.format("%.2f", ivaValor));
        lblTotal.setText(String.format("%.2f", total));
    }

    private void eliminarFila(int row) {
        if (row >= 0) {
            modeloTabla.removeRow(row);
            calcularTotalesUI(); 
            JOptionPane.showMessageDialog(this, "Producto eliminado de la lista. Clic en 'Guardar' para confirmar cambios.");
        }
    }

    private void accionAprobar() {
        if(JOptionPane.showConfirmDialog(this, "¿Aprobar orden y generar asiento?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            double sub = Double.parseDouble(lblSubtotal.getText().replace(",", "."));
            double imp = Double.parseDouble(lblIVA.getText().replace(",", "."));
            double tot = Double.parseDouble(lblTotal.getText().replace(",", "."));
            String nomProv = cmbProveedor.getSelectedItem().toString();

            GestionOrdenCompra gestor = new GestionOrdenCompra();
            if (gestor.aprobarCompraConAsiento(this.idCompra, nomProv, sub, imp, tot)) {
                JOptionPane.showMessageDialog(this, "Orden APROBADA y Asiento Generado.");
                estadoActual = "APR"; txtEstado.setText("APR"); controlarEstadoBotones();
                cargarDetallesTabla(); 
                if (tablaDetalles.getColumnCount() > 0 && tablaDetalles.getColumnName(0).equals("Acción")) {
                    tablaDetalles.removeColumn(tablaDetalles.getColumnModel().getColumn(0));
                }
            }
        }
    }

    private void accionAnular() {
        if(JOptionPane.showConfirmDialog(this, "¿Anular orden?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            actualizarEstadoBD("ANU");
        }
    }
    private void accionRestaurar() {
        if(JOptionPane.showConfirmDialog(this, "¿Restaurar orden?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            actualizarEstadoBD("ABI");
        }
    }

    private void actualizarEstadoBD(String nuevoEstado) {
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement("UPDATE COMPRAS SET ESTADO_OC = ? WHERE id_Compra = ?")) {
            ps.setString(1, nuevoEstado); ps.setString(2, this.idCompra);
            ps.executeUpdate();
            String accionLog = nuevoEstado.equals("ANU") ? "ANULAR" : "RESTAURAR";
            Logger.registrar("COMPRAS", this.idCompra, accionLog, "Cambio de estado manual a " + nuevoEstado);
            try (PreparedStatement ps2 = con.prepareStatement("UPDATE PROXOC SET ESTADO_PxOC = ? WHERE id_Compra = ?")) {
                ps2.setString(1, nuevoEstado); ps2.setString(2, this.idCompra);
                ps2.executeUpdate();
            }
            this.estadoActual = nuevoEstado;
            txtEstado.setText(nuevoEstado);
            controlarEstadoBotones();
            cargarDetallesTabla();
            JOptionPane.showMessageDialog(this, "Estado actualizado.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void estilizarTabla(JTable table) {
        table.setOpaque(false);
        table.setBackground(new Color(0, 0, 0, 0));
        table.setForeground(Color.WHITE);
        table.setFont(fontPlain);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(255, 255, 255, 20));
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(20, 20, 30));
        header.setForeground(new Color(200, 200, 250));
        header.setFont(fontBold);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
        table.setDefaultRenderer(Object.class, new RenderizadorGeneral());
        try {
            TableColumn colAccion = table.getColumnModel().getColumn(0);
            if (colAccion.getHeaderValue().toString().equals("Acción")) {
                colAccion.setCellRenderer(new BotonEliminarRenderer());
                colAccion.setCellEditor(new BotonEliminarEditor(table));
                colAccion.setPreferredWidth(80);
            }
        } catch (Exception e) {}
    }

    class RenderizadorGeneral extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JLabel) c).setOpaque(true); 
            if (isSelected) { c.setBackground(new Color(40, 60, 100)); c.setForeground(Color.WHITE); } 
            else { c.setBackground(new Color(30, 30, 40)); c.setForeground(Color.WHITE); }
            return c;
        }
    }

    class BotonEliminarRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setOpaque(true);
            if (isSelected) l.setBackground(new Color(40, 60, 100)); else l.setBackground(new Color(30, 30, 40));
            l.setForeground(colorRojo); l.setFont(fontBold); l.setHorizontalAlignment(JLabel.CENTER); l.setText("Eliminar");
            return l;
        }
    }

    class BotonEliminarEditor extends AbstractCellEditor implements TableCellEditor {
        JButton btn = new JButton("Eliminar");
        int filaActual;
        public BotonEliminarEditor(JTable table) {
            btn.setForeground(colorRojo); btn.setFont(fontBold);
            btn.setContentAreaFilled(false); btn.setBorderPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> { fireEditingStopped(); eliminarFila(filaActual); });
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            filaActual = row; btn.setOpaque(true); btn.setBackground(new Color(40, 60, 100)); return btn;
        }
        @Override public Object getCellEditorValue() { return "Eliminar"; }
    }

    private JButton crearBoton(String texto, Color bg, Color fg) {
        JButton b = new JButton(texto);
        b.setBackground(bg); b.setForeground(fg); b.setFont(fontBold);
        b.setFocusPainted(false); b.setBorder(BorderFactory.createEmptyBorder());
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); return b;
    }
    private JPanel crearPanelGlass(int x, int y, int w, int h) {
        JPanel p = new JPanel() { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(new Color(255, 255, 255, 10)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); g2.setColor(new Color(255, 255, 255, 30)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); } };
        p.setLayout(null); p.setBounds(x, y, w, h); p.setOpaque(false); return p;
    }
    private void crearEtiqueta(String texto, int x, int y, JPanel panel) { JLabel lbl = new JLabel(texto); lbl.setForeground(new Color(180, 180, 200)); lbl.setFont(new Font("SansSerif", Font.PLAIN, 11)); lbl.setBounds(x, y, 150, 20); panel.add(lbl); }
    private JTextField crearInputLectura(int x, int y, int w, JPanel panel) { JTextField t = new JTextField(); t.setBounds(x, y, w, 30); t.setBackground(new Color(0,0,0,0)); t.setForeground(Color.WHITE); t.setFont(fontBold); t.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 100))); t.setEditable(false); t.setFocusable(false); panel.add(t); return t; }
    private void agregarMovimientoVentana(JPanel panel) { MouseAdapter ma = new MouseAdapter() { public void mousePressed(MouseEvent evt) { xMouse = evt.getX(); yMouse = evt.getY(); } public void mouseDragged(MouseEvent evt) { setLocation(evt.getXOnScreen() - xMouse, evt.getYOnScreen() - yMouse); } }; panel.addMouseListener(ma); panel.addMouseMotionListener(ma); }
    private void cargarProveedores() { try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement("SELECT id_Proveedor, prv_Nombre FROM PROVEEDORES WHERE ESTADO_PRV = 'ACT'"); ResultSet rs = ps.executeQuery()) { while (rs.next()) cmbProveedor.addItem(new ProveedorItem(rs.getString("id_Proveedor"), rs.getString("prv_Nombre"))); } catch (Exception e) {} }
    private void cargarProductos(JComboBox<ProductoItem> combo) { try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement("SELECT id_Producto, pro_Descripcion, pro_Valor_Compra FROM PRODUCTOS WHERE ESTADO_PROD = 'ACT'"); ResultSet rs = ps.executeQuery()) { while (rs.next()) combo.addItem(new ProductoItem(rs.getString("id_Producto"), rs.getString("pro_Descripcion"), rs.getDouble("pro_Valor_Compra"))); } catch (Exception e) {} }

    public class ProveedorItem { public String id; public String nombre; public ProveedorItem(String id, String nombre) { this.id = id; this.nombre = nombre; } @Override public String toString() { return nombre; } }
    public class ProductoItem { public String id; public String nombre; public double precio; public ProductoItem(String id, String nombre, double precio) { this.id = id; this.nombre = nombre; this.precio = precio; } @Override public String toString() { return nombre; } }
}