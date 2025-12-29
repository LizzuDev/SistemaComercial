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
import java.time.LocalDate;
import javax.swing.AbstractCellEditor;
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

public class FormularioNuevaCompra extends JDialog {

    private DefaultTableModel modeloTabla;
    private JComboBox<ProveedorItem> cmbProveedores;
    private DatePicker datePicker;
    
    // Componentes de Totales e IVA
    private JLabel lblSubtotal, lblIVA, lblTotal;
    private JComboBox<String> cmbIVA; 
    
    private JLabel lblIdCompra;
    // --- NUEVO COMBOBOX PARA ESTADO ---
    private JComboBox<String> cmbEstadoInicial; 
    // ----------------------------------
    
    private JTable tablaDetalles; 

    // Variables de cálculo
    private double subtotalCalculado = 0.0;
    private double montoIvaCalculado = 0.0;
    private double totalCalculado = 0.0;

    // Estilos
    private Font fontBold = new Font("SansSerif", Font.BOLD, 14);
    private Font fontTitle = new Font("SansSerif", Font.BOLD, 24);
    private Font fontPlain = new Font("SansSerif", Font.PLAIN, 12);
    private Color colorBlanco = Color.WHITE;
    private Color colorRojo = new Color(220, 53, 69); 

    private int xMouse, yMouse;
    private GestionOrdenCompra gestionOC;

    public FormularioNuevaCompra(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.gestionOC = new GestionOrdenCompra();
        
        setUndecorated(true);
        setSize(900, 700); 
        setLocationRelativeTo(parent);

        construirInterfazPrincipal();
        cargarProveedores();
        lblIdCompra.setText(gestionOC.generarIdOrden());
    }

    private void construirInterfazPrincipal() {
        JPanel panelFondo = crearPanelDegradado();
        panelFondo.setLayout(null);
        setContentPane(panelFondo);
        agregarMovimientoVentana(panelFondo);

        // Header
        JLabel lblTitulo = new JLabel("Nueva Orden de Compra");
        lblTitulo.setFont(fontTitle);
        lblTitulo.setForeground(colorBlanco);
        lblTitulo.setBounds(30, 20, 350, 30);
        panelFondo.add(lblTitulo);

        lblIdCompra = new JLabel("Cargando...");
        lblIdCompra.setFont(fontTitle);
        lblIdCompra.setForeground(colorBlanco);
        lblIdCompra.setHorizontalAlignment(JLabel.RIGHT);
        lblIdCompra.setBounds(650, 20, 200, 30);
        panelFondo.add(lblIdCompra);

        // --- SELECCIÓN DE ESTADO INICIAL ---
        JLabel lblEst = new JLabel("Estado:");
        lblEst.setFont(fontBold);
        lblEst.setForeground(colorBlanco);
        lblEst.setHorizontalAlignment(JLabel.RIGHT);
        lblEst.setBounds(600, 60, 80, 25);
        panelFondo.add(lblEst);

        // Aquí permitimos escoger ABI o CER
        cmbEstadoInicial = new JComboBox<>(new String[]{"ABI", "CER"});
        cmbEstadoInicial.setBounds(690, 60, 160, 25);
        panelFondo.add(cmbEstadoInicial);
        // -----------------------------------

        // Formulario (Proveedor, Fecha, etc...)
        crearEtiqueta("Proveedor:", 30, 70, panelFondo);
        cmbProveedores = new JComboBox<>();
        cmbProveedores.setBounds(30, 95, 350, 30);
        panelFondo.add(cmbProveedores);

        crearEtiqueta("Fecha Emisión:", 400, 70, panelFondo);
        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setFormatForDatesCommonEra("dd/MM/yyyy");
        dateSettings.setAllowKeyboardEditing(false);
        datePicker = new DatePicker(dateSettings);
        datePicker.setDateToToday();
        datePicker.setBounds(400, 95, 180, 30);
        dateSettings.setDateRangeLimits(null, LocalDate.now()); 

        panelFondo.add(datePicker);

        JButton btnAgregarProd = new JButton("AGREGAR PRODUCTO (+)");
        btnAgregarProd.setBounds(650, 95, 200, 30);
        btnAgregarProd.setBackground(new Color(0, 100, 150));
        btnAgregarProd.setForeground(Color.WHITE);
        btnAgregarProd.addActionListener(e -> mostrarDialogoProducto());
        panelFondo.add(btnAgregarProd);

        // --- TABLA ---
        modeloTabla = new DefaultTableModel(new String[]{"Acción", "ID", "Producto", "Cant", "Precio Unit.", "Total Línea", "Estado"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 0; }
        };
        
        tablaDetalles = new JTable(modeloTabla);
        estilizarTabla(tablaDetalles); 
        
        JScrollPane scroll = new JScrollPane(tablaDetalles);
        scroll.setBounds(30, 150, 840, 300);
        panelFondo.add(scroll);

        // --- TOTALES E IVA ---
        int xTotales = 600;
        int yInicio = 470;
        int gap = 30;

        lblSubtotal = new JLabel("Subtotal: $ 0.00");
        lblSubtotal.setFont(fontBold);
        lblSubtotal.setForeground(colorBlanco);
        lblSubtotal.setHorizontalAlignment(JLabel.RIGHT);
        lblSubtotal.setBounds(xTotales, yInicio, 250, 20);
        panelFondo.add(lblSubtotal);

        // Combo IVA
        JLabel lblTxtIVA = new JLabel("IVA %:");
        lblTxtIVA.setFont(fontBold);
        lblTxtIVA.setForeground(colorBlanco);
        lblTxtIVA.setBounds(xTotales + 60, yInicio + gap, 50, 25);
        panelFondo.add(lblTxtIVA);

        cmbIVA = new JComboBox<>(new String[]{"0", "12", "15"});
        cmbIVA.setSelectedItem("15");
        cmbIVA.setBounds(xTotales + 110, yInicio + gap, 50, 25);
        cmbIVA.addActionListener(e -> calcularTotal()); 
        panelFondo.add(cmbIVA);

        lblIVA = new JLabel("$ 0.00");
        lblIVA.setFont(fontBold);
        lblIVA.setForeground(colorBlanco);
        lblIVA.setHorizontalAlignment(JLabel.RIGHT);
        lblIVA.setBounds(xTotales + 170, yInicio + gap, 80, 25);
        panelFondo.add(lblIVA);

        lblTotal = new JLabel("TOTAL: $ 0.00");
        lblTotal.setFont(fontTitle.deriveFont(22f));
        lblTotal.setForeground(colorBlanco);
        lblTotal.setHorizontalAlignment(JLabel.RIGHT);
        lblTotal.setBounds(xTotales, yInicio + (gap * 2), 250, 30);
        panelFondo.add(lblTotal);

        // --- BOTONES ---
        int yBtn = 600;
        
        // BOTÓN GUARDAR (MODIFICADO)
        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.setBounds(30, yBtn, 100, 35);
        btnGuardar.setBackground(new Color(40, 167, 69));
        btnGuardar.setForeground(Color.WHITE);
        
        // AQUÍ ESTÁ LA LÓGICA: Leemos el estado del combo box
        btnGuardar.addActionListener(e -> {
            String estadoSeleccionado = cmbEstadoInicial.getSelectedItem().toString();
            procesarOrden(estadoSeleccionado);
        }); 
        panelFondo.add(btnGuardar);

        JButton btnAprobar = new JButton("Aprobar");
        btnAprobar.setBounds(140, yBtn, 100, 35);
        btnAprobar.addActionListener(e -> procesarOrden("APR")); 
        panelFondo.add(btnAprobar);

        JButton btnAnular = new JButton("Anular");
        btnAnular.setBounds(250, yBtn, 100, 35);
        btnAnular.setBackground(new Color(220, 53, 69));
        btnAnular.setForeground(Color.WHITE);
        btnAnular.addActionListener(e -> procesarOrden("ANU")); 
        panelFondo.add(btnAnular);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setBounds(770, yBtn, 100, 35);
        btnCerrar.addActionListener(e -> dispose());
        panelFondo.add(btnCerrar);
    }

    // --- LÓGICA CON LOG INTEGRADO ---

    private void procesarOrden(String estadoDestino) {
        if (modeloTabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Debe agregar productos.");
            return;
        }
        
        // Validación segura
        ProveedorItem itemProv = (ProveedorItem) cmbProveedores.getSelectedItem();
        if (itemProv == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un proveedor.");
            return;
        }

        LocalDate fecha = datePicker.getDate();
        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Fecha inválida.");
            return;
        }

        // Si es APR o ANU, pedimos confirmación extra
        if (!estadoDestino.equals("ABI") && !estadoDestino.equals("CER")) {
            String accion = estadoDestino.equals("APR") ? "APROBAR" : "ANULAR";
            if (JOptionPane.showConfirmDialog(this, "¿Desea GUARDAR y " + accion + " la orden?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String idFinal = gestionOC.generarIdOrden();
        String idProveedor = itemProv.id; 
        int ivaSeleccionado = Integer.parseInt(cmbIVA.getSelectedItem().toString());

        boolean exito = gestionOC.registrarOrdenCompra(
            idFinal,
            idProveedor,
            fecha,
            subtotalCalculado,
            ivaSeleccionado,
            modeloTabla,
            estadoDestino 
        );

        if (exito) {
            // =========================================================================
            // AQUÍ REGISTRAMOS EL LOG DEL INSERT
            // =========================================================================
            Logger.registrar("COMPRAS", idFinal, "INSERT", "Nueva Orden creada con estado: " + estadoDestino);
            // =========================================================================
            
            JOptionPane.showMessageDialog(this, "Orden " + idFinal + " procesada (" + estadoDestino + ").");
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar en BD.");
        }
    }

    private void calcularTotal() {
        subtotalCalculado = 0;
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            subtotalCalculado += Double.parseDouble(modeloTabla.getValueAt(i, 5).toString());
        }
        
        int porcentaje = Integer.parseInt(cmbIVA.getSelectedItem().toString());
        montoIvaCalculado = subtotalCalculado * (porcentaje / 100.0);
        totalCalculado = subtotalCalculado + montoIvaCalculado;

        lblSubtotal.setText(String.format("Subtotal: $ %.2f", subtotalCalculado));
        lblIVA.setText(String.format("$ %.2f", montoIvaCalculado));
        lblTotal.setText(String.format("TOTAL: $ %.2f", totalCalculado));
    }

    private void eliminarFila(int row) {
        if (row >= 0) {
            modeloTabla.removeRow(row);
            calcularTotal();
        }
    }

    // --- TABLA Y RENDERERS ---
    private void estilizarTabla(JTable table) {
        table.setRowHeight(30);
        JTableHeader header = table.getTableHeader();
        header.setFont(fontBold);
        
        TableColumn colAccion = table.getColumnModel().getColumn(0);
        colAccion.setCellRenderer(new BotonEliminarRenderer());
        colAccion.setCellEditor(new BotonEliminarEditor(table));
        colAccion.setPreferredWidth(80);
    }

    class BotonEliminarRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setForeground(colorRojo); l.setFont(fontBold); l.setHorizontalAlignment(JLabel.CENTER); l.setText("Eliminar");
            return l;
        }
    }

    class BotonEliminarEditor extends AbstractCellEditor implements TableCellEditor {
        JButton btn = new JButton("Eliminar");
        int filaActual;
        public BotonEliminarEditor(JTable table) {
            btn.setForeground(colorRojo); btn.setFont(fontBold);
            btn.addActionListener(e -> { fireEditingStopped(); eliminarFila(filaActual); });
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            filaActual = row; return btn;
        }
        @Override public Object getCellEditorValue() { return "Eliminar"; }
    }

    // --- DIÁLOGO PRODUCTO ---
    private void mostrarDialogoProducto() {
        JDialog dialogProd = new JDialog(this, "Agregar Item", true);
        dialogProd.setUndecorated(true); dialogProd.setSize(400, 350); dialogProd.setLocationRelativeTo(this);
        JPanel panelP = crearPanelDegradado(); panelP.setLayout(null); dialogProd.setContentPane(panelP);

        JLabel lblP = new JLabel("Seleccionar Producto"); lblP.setFont(fontBold.deriveFont(18f)); lblP.setForeground(colorBlanco); lblP.setBounds(20, 20, 300, 30); panelP.add(lblP);
        crearEtiqueta("Producto:", 20, 70, panelP);
        JComboBox<ProductoItem> cmbProdPopup = new JComboBox<>(); cmbProdPopup.setBounds(20, 95, 360, 30); cargarProductos(cmbProdPopup); panelP.add(cmbProdPopup);
        crearEtiqueta("Cantidad:", 20, 140, panelP);
        JTextField txtCantPopup = new JTextField("1"); txtCantPopup.setBounds(20, 165, 100, 30); txtCantPopup.setHorizontalAlignment(JTextField.CENTER); panelP.add(txtCantPopup);
        crearEtiqueta("Precio Unitario:", 140, 140, panelP);
        JTextField txtPrecioPopup = new JTextField("0.00"); txtPrecioPopup.setBounds(140, 165, 100, 30); panelP.add(txtPrecioPopup);

        cmbProdPopup.addActionListener(e -> {
            ProductoItem item = (ProductoItem) cmbProdPopup.getSelectedItem();
            if(item != null) txtPrecioPopup.setText(String.valueOf(item.precio));
        });

        JButton btnAceptar = new JButton("Aceptar"); btnAceptar.setBounds(80, 250, 100, 35); 
        btnAceptar.setBackground(new Color(40, 167, 69)); // Verde
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.addActionListener(e -> { agregarFilaATabla(cmbProdPopup, txtCantPopup, txtPrecioPopup); dialogProd.dispose(); }); panelP.add(btnAceptar);
        
        JButton btnCancelar = new JButton("Cancelar"); btnCancelar.setBounds(200, 250, 100, 35); btnCancelar.addActionListener(e -> dialogProd.dispose()); panelP.add(btnCancelar);
        dialogProd.setVisible(true);
    }

    private void agregarFilaATabla(JComboBox<ProductoItem> combo, JTextField txtC, JTextField txtP) {
        // 1. Validar selección
        ProductoItem prod = (ProductoItem) combo.getSelectedItem();
        if (prod == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto.");
            return;
        }

        // --- NUEVA VALIDACIÓN: DUPLICADOS ---
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            String idEnTabla = modeloTabla.getValueAt(i, 1).toString(); // Columna 1 es el ID
            if (idEnTabla.equals(prod.id)) {
                JOptionPane.showMessageDialog(this, 
                    "El producto '" + prod.nombre + "' ya está en la lista.\n" +
                    "Si desea cambiar la cantidad, elimínelo y agréguelo de nuevo.", 
                    "Producto Duplicado", JOptionPane.WARNING_MESSAGE);
                return; // Detenemos el proceso aquí
            }
        }
        // ------------------------------------

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

        // 4. Agregar Fila
        double totalLinea = cant * precioIngresado;
        modeloTabla.addRow(new Object[]{ "Eliminar", prod.id, prod.toString(), cant, precioIngresado, totalLinea, "ABI" });
        
        calcularTotal();
    }

    // --- HELPERS Y CLASES ---
    private void cargarProveedores() {
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement("SELECT id_Proveedor, prv_Nombre FROM PROVEEDORES WHERE ESTADO_PRV = 'ACT'"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cmbProveedores.addItem(new ProveedorItem(rs.getString("id_Proveedor"), rs.getString("prv_Nombre")));
        } catch (Exception e) {}
    }
    private void cargarProductos(JComboBox<ProductoItem> combo) {
        try (Connection con = PruebaJDBC.getConexion(); PreparedStatement ps = con.prepareStatement("SELECT id_Producto, pro_Descripcion, pro_Valor_Compra FROM PRODUCTOS WHERE ESTADO_PROD = 'ACT'"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) combo.addItem(new ProductoItem(rs.getString("id_Producto"), rs.getString("pro_Descripcion"), rs.getDouble("pro_Valor_Compra")));
        } catch (Exception e) {}
    }
    private JPanel crearPanelDegradado() { return new JPanel() { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); GradientPaint gp = new GradientPaint(0, 0, new Color(5, 12, 35), 0, getHeight(), new Color(2, 5, 20)); g2.setPaint(gp); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0); } }; }
    private void crearEtiqueta(String texto, int x, int y, JPanel panel) { JLabel lbl = new JLabel(texto); lbl.setFont(fontPlain); lbl.setForeground(colorBlanco); lbl.setBounds(x, y, 200, 20); panel.add(lbl); }
    private void agregarMovimientoVentana(JPanel panel) { MouseAdapter ma = new MouseAdapter() { public void mousePressed(MouseEvent evt) { xMouse = evt.getX(); yMouse = evt.getY(); } public void mouseDragged(MouseEvent evt) { setLocation(evt.getXOnScreen() - xMouse, evt.getYOnScreen() - yMouse); } }; panel.addMouseListener(ma); panel.addMouseMotionListener(ma); }

    public class ProveedorItem {
        public String id; public String nombre;
        public ProveedorItem(String id, String nombre) { this.id = id; this.nombre = nombre; }
        @Override public String toString() { return nombre; }
    }

    public class ProductoItem {
        public String id; public String nombre; public double precio;
        public ProductoItem(String id, String nombre, double precio) { this.id = id; this.nombre = nombre; this.precio = precio; }
        @Override public String toString() { return nombre; }
    }
}