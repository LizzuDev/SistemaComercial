package modelo;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

public class VentanaCompras extends JFrame {

    private int xMouse, yMouse;
    private CardLayout cardLayout;
    private JPanel panelContenido;
    private JPanel sidebar;
    private JButton btnMenuDash, btnMenuProv, btnMenuOrd;
    private Font fontBold, fontPlain;
    
    // VARIABLES GLOBALES DE DATOS
    private DefaultTableModel modeloTablaProveedores; 
    private JTextField txtBuscarProv;
    private DefaultTableModel modeloTablaOrdenes;
    private JTextField txtBuscarOrden;
    
    // --- VARIABLES DE PERMISOS (SEPARADAS) ---
    private boolean puedeEditarProveedores = false;
    private boolean puedeCrearOrdenes = false;    // Permiso para el botón "Nueva Orden"
    private boolean puedeEditarOrdenes = false;   // Permiso para modificar dentro del Detalle (Aprobar/Anular)
    private boolean puedeVerAnuladas = false;     // Permiso para ver historial completo

    // Listas en MEMORIA
    private List<Object[]> listaMaestra = new ArrayList<>(); 
    private List<Object[]> listaFiltrada = new ArrayList<>(); 
    
    // PAGINACIÓN PROVEEDORES
    private int paginaActual = 1;
    private final int FILAS_X_PAGINA = 10;
    private JLabel lblInfoPaginacion;
    private JButton btnPagPrev, btnPagNext;
    
    // PAGINACIÓN ÓRDENES
    private List<Object[]> listaMaestraOrdenes = new ArrayList<>();
    private List<Object[]> listaFiltradaOrdenes = new ArrayList<>();
    private int paginaActualOrdenes = 1;
    private JLabel lblInfoPaginacionOrdenes;
    private JButton btnPagPrevOrdenes, btnPagNextOrdenes;

    public VentanaCompras() {
        cargarFuentes();
        verificarPermisos(); // Configura los flags de seguridad
        
        setUndecorated(true);
        initComponents();
        aplicarDisenoFinal();
    }
    
    // --- LÓGICA DE SEGURIDAD (CONFIGURACIÓN DE ROLES) ---
    private void verificarPermisos() {
        Usuario u = Sesion.getUsuarioActual();
        if (u != null) {
            String rol = u.getIdRol();
            
            // 1. Definir Roles de "Alto Mando" (Gerencia/Admin)
            boolean esAltoMando = rol.equals("ROL-ADM") || 
                                  rol.equals("ROL-GER") || 
                                  rol.equals("ROL-001"); // Ejemplo Dueño

            // 2. Definir Roles Operativos (Jefes/Coordinadores)
            boolean esOperativo = rol.equals("ROL-COO") || 
                                  rol.equals("ROL-JEF");

            // --- ASIGNACIÓN DE PERMISOS ESPECÍFICOS ---
            
            // A. Permiso para CREAR nuevas órdenes
            puedeCrearOrdenes = esAltoMando || esOperativo;

            // B. Permiso para EDITAR/APROBAR/ANULAR (Aquí está la separación)
            // Quizás los operativos pueden crear pero solo los Gerentes pueden Aprobar/Anular
            puedeEditarOrdenes = esAltoMando; 

            // C. Permiso para ver Proveedores y editar su data maestra
            puedeEditarProveedores = esAltoMando;
            
            // D. Permiso para ver lo anulado (Auditoría)
            puedeVerAnuladas = esAltoMando; 
        }
    }
    
    // --- LÓGICA DE CARGA DE ÓRDENES (FILTRADO POR SQL) ---
    private void cargarOrdenesDesdeBD() {
        listaMaestraOrdenes.clear();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.id_Compra, p.prv_Nombre, c.oc_Fecha_Hora, c.oc_Subtotal, c.ESTADO_OC ");
        sql.append("FROM COMPRAS c ");
        sql.append("INNER JOIN PROVEEDORES p ON c.id_Proveedor = p.id_Proveedor ");
        
        // Si NO tiene permiso de ver anuladas, las ocultamos
        if (!puedeVerAnuladas) {
            sql.append("WHERE c.ESTADO_OC != 'ANU' ");
        }
        
        sql.append("ORDER BY c.oc_Fecha_Hora DESC"); 

        try {
            con = PruebaJDBC.getConexion();
            ps = con.prepareStatement(sql.toString());
            rs = ps.executeQuery();

            while (rs.next()) {
                listaMaestraOrdenes.add(new Object[]{
                    rs.getString("id_Compra"),
                    rs.getString("prv_Nombre"),
                    rs.getString("oc_Fecha_Hora"),
                    "$ " + rs.getDouble("oc_Subtotal"),
                    rs.getString("ESTADO_OC"),
                    "" // Columna botón
                });
            }
            // Resetear filtro al cargar
            filtrarOrdenes(""); 
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); if (ps != null) ps.close(); if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    private void filtrarOrdenes(String texto) {
        if (texto.isEmpty() || texto.equals("Buscar orden...")) {
            listaFiltradaOrdenes = new ArrayList<>(listaMaestraOrdenes);
        } else {
            String txt = texto.toLowerCase();
            listaFiltradaOrdenes = listaMaestraOrdenes.stream()
                .filter(fila -> {
                    String id = fila[0].toString().toLowerCase();
                    String prov = fila[1].toString().toLowerCase();
                    return id.contains(txt) || prov.contains(txt);
                })
                .collect(Collectors.toList());
        }
        paginaActualOrdenes = 1;
        actualizarTablaOrdenes();
    }

    private void actualizarTablaOrdenes() {
        modeloTablaOrdenes.setRowCount(0);
        int totalRegistros = listaFiltradaOrdenes.size();
        int totalPaginas = (int) Math.ceil((double) totalRegistros / FILAS_X_PAGINA);
        if (totalPaginas == 0) totalPaginas = 1;

        if (paginaActualOrdenes < 1) paginaActualOrdenes = 1;
        if (paginaActualOrdenes > totalPaginas) paginaActualOrdenes = totalPaginas;

        int inicio = (paginaActualOrdenes - 1) * FILAS_X_PAGINA;
        int fin = Math.min(inicio + FILAS_X_PAGINA, totalRegistros);

        for (int i = inicio; i < fin; i++) {
            modeloTablaOrdenes.addRow(listaFiltradaOrdenes.get(i));
        }

        if (totalRegistros == 0) lblInfoPaginacionOrdenes.setText("Sin resultados");
        else lblInfoPaginacionOrdenes.setText("Mostrando " + (inicio + 1) + "-" + fin + " de " + totalRegistros);
        
        btnPagPrevOrdenes.setEnabled(paginaActualOrdenes > 1);
        btnPagNextOrdenes.setEnabled(paginaActualOrdenes < totalPaginas);
    }
    
    private void cambiarPaginaOrdenes(int delta) {
        paginaActualOrdenes += delta;
        actualizarTablaOrdenes();
    }

    private void cargarFuentes() {
        try {
            fontBold = new Font("SansSerif", Font.BOLD, 13);
            fontPlain = new Font("SansSerif", Font.PLAIN, 13);
        } catch (Exception e) { }
    }

    private void aplicarDisenoFinal() {
        setSize(1150, 720);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));

        JPanel panelFondo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(5, 12, 35), 0, getHeight(), new Color(2, 5, 20));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 35, 35);
                g2.setColor(new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 35, 35);
            }
        };
        panelFondo.setLayout(null);
        setContentPane(panelFondo);

        crearSidebar(panelFondo);

        cardLayout = new CardLayout();
        panelContenido = new JPanel(cardLayout);
        panelContenido.setBounds(250, 60, 880, 640);
        panelContenido.setOpaque(false);
        panelFondo.add(panelContenido);

        panelContenido.add(crearVistaDashboard(), "DASHBOARD");
        panelContenido.add(crearVistaProveedores(), "PROVEEDORES");
        panelContenido.add(crearVistaOrdenes(), "ORDENES");

        JButton btnVolver = new JButton("← Atrás");
        btnVolver.setFont(fontBold.deriveFont(14f));
        btnVolver.setForeground(new Color(200, 200, 220));
        btnVolver.setContentAreaFilled(false);
        btnVolver.setBorderPainted(false);
        btnVolver.setFocusPainted(false);
        btnVolver.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVolver.setBounds(15, 15, 100, 30);
        btnVolver.addActionListener(e -> this.dispose());
        panelFondo.add(btnVolver);

        agregarBotonesControl(panelFondo, getWidth());

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent evt) { xMouse = evt.getX(); yMouse = evt.getY(); }
            @Override public void mouseDragged(MouseEvent evt) { setLocation(evt.getXOnScreen() - xMouse, evt.getYOnScreen() - yMouse); }
        };
        panelFondo.addMouseListener(ma);
        panelFondo.addMouseMotionListener(ma);
        
        navegarA("PROVEEDORES", btnMenuProv);
    }

    // ... (MÉTODOS DE PROVEEDORES OMITIDOS PARA AHORRAR ESPACIO, SON IGUALES A TU CÓDIGO ANTERIOR) ...
    // ... Asegúrate de mantener crearVistaProveedores, cargarDatosDesdeBD(Proveedores), etc. ...
    
    // --- VISTA ÓRDENES ---
    private JPanel crearVistaOrdenes() {
        JPanel p = new JPanel(null);
        p.setOpaque(false);

        JLabel lbl = new JLabel("Órdenes de Compra");
        lbl.setFont(fontBold.deriveFont(28f));
        lbl.setForeground(Color.WHITE);
        lbl.setBounds(0, 0, 400, 40);
        p.add(lbl);

        JPanel glass = crearPanelGlassClaro(0, 60, 880, 580);
        p.add(glass);
        
        txtBuscarOrden = crearInputBlanco("Buscar orden...");
        txtBuscarOrden.setBounds(30, 25, 250, 40);
        txtBuscarOrden.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filtrarOrdenes(txtBuscarOrden.getText().trim()); }
        });
        glass.add(txtBuscarOrden);

        JButton btnNuevaCompra = crearBotonBlanco("+ Nueva Orden", 720, 25, 130);
        btnNuevaCompra.setForeground(new Color(0, 150, 80)); 
        btnNuevaCompra.setVisible(puedeCrearOrdenes); // Usa el permiso de CREAR
        btnNuevaCompra.addActionListener(e -> {
            FormularioNuevaCompra form = new FormularioNuevaCompra(this, true);
            form.setVisible(true);
            cargarOrdenesDesdeBD();
        });
        glass.add(btnNuevaCompra);

        String[] cols = {"N° Orden", "Proveedor", "Fecha", "Total", "Estado", "Ver"};
        JScrollPane scroll = crearTablaOrdenes(cols);
        scroll.setBounds(30, 90, 820, 400); 
        glass.add(scroll);

        JPanel panelPaginacion = crearPanelPaginacionOrdenes(30, 510, 820);
        glass.add(panelPaginacion);

        cargarOrdenesDesdeBD();
        return p; 
    }
    
    private JScrollPane crearTablaOrdenes(String[] cols) {
        modeloTablaOrdenes = new DefaultTableModel(null, cols) {
            @Override public boolean isCellEditable(int row, int col) { return col == cols.length - 1; }
        };

        JTable table = new JTable(modeloTablaOrdenes);
        table.setOpaque(false);
        table.setBackground(Color.WHITE);
        table.setForeground(new Color(60, 70, 80));
        table.setRowHeight(50);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(230, 230, 240));
        table.setFont(fontPlain);

        // Anchos
        table.getColumnModel().getColumn(0).setPreferredWidth(100); 
        table.getColumnModel().getColumn(1).setPreferredWidth(250); 
        table.getColumnModel().getColumn(2).setPreferredWidth(120); 
        table.getColumnModel().getColumn(3).setPreferredWidth(100); 
        table.getColumnModel().getColumn(4).setPreferredWidth(100); 
        table.getColumnModel().getColumn(5).setPreferredWidth(100); // Un poco más ancho para 2 botones

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 50));
        
        table.getColumnModel().getColumn(4).setCellRenderer(new EstadoRenderer());
        
        // --- CAMBIO AQUÍ: Usamos los nuevos Renderer y Editor ---
        TableColumn colAcciones = table.getColumnModel().getColumn(5);
        colAcciones.setCellRenderer(new BotonOrdenRenderer(puedeEditarOrdenes));
        colAcciones.setCellEditor(new BotonOrdenEditor(table, puedeEditarOrdenes));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    // --- CLASES INTERNAS ORDENES ---
    class EstadoRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String estado = value != null ? value.toString() : "";
            l.setFont(fontBold.deriveFont(12f));
            l.setHorizontalAlignment(JLabel.CENTER);
            if (estado.startsWith("ABI") || estado.startsWith("PEN")) l.setForeground(new Color(255, 140, 0)); 
            else if (estado.startsWith("APR") || estado.startsWith("REC")) l.setForeground(new Color(0, 180, 60)); 
            else if (estado.startsWith("ANU")) l.setForeground(new Color(200, 50, 50)); 
            else l.setForeground(Color.GRAY);
            return l;
        }
    }

    // =========================================================================
    // CLASES INTERNAS PARA TABLA ÓRDENES (LUPITA Y LÁPIZ)
    // =========================================================================

    // 1. RENDERER: Dibuja los iconos
    class BotonOrdenRenderer extends DefaultTableCellRenderer {
        boolean puedeEditar;
        public BotonOrdenRenderer(boolean puedeEditar) { this.puedeEditar = puedeEditar; }
        
        @Override 
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
            p.setOpaque(true);
            p.setBackground(isSelected ? new Color(235, 245, 255) : Color.WHITE);
            
            // LUPITA (Siempre visible) -> Modo Lectura
            JLabel lVer = new JLabel("🔍");
            lVer.setForeground(new Color(100, 60, 180)); // Morado
            lVer.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
            lVer.setBorder(new EmptyBorder(0, 5, 0, 5));
            p.add(lVer);

            // LÁPIZ (Solo si tiene permiso) -> Modo Edición
            if (puedeEditar) {
                JLabel lEdit = new JLabel("✎");
                lEdit.setForeground(new Color(0, 100, 200)); // Azul
                lEdit.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
                lEdit.setBorder(new EmptyBorder(0, 5, 0, 5));
                p.add(lEdit);
            } else {
                // Si no tiene permiso, mostramos un candadito gris opcional
                JLabel lLock = new JLabel("🔒");
                lLock.setForeground(Color.GRAY);
                p.add(lLock);
            }
            return p;
        }
    }

    // 2. EDITOR: Maneja los clics
    class BotonOrdenEditor extends AbstractCellEditor implements TableCellEditor {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        JButton btnVer = new JButton("🔍");
        JButton btnEdit = new JButton("✎");
        JTable tablaRef;
        boolean puedeEditar;
        
        public BotonOrdenEditor(JTable tabla, boolean puedeEditar) {
            this.tablaRef = tabla;
            this.puedeEditar = puedeEditar;
            
            // Configurar Lupita
            estilizarBotonTabla(btnVer, new Color(100, 60, 180));
            p.add(btnVer);
            
            // Configurar Lápiz
            if (puedeEditar) {
                estilizarBotonTabla(btnEdit, new Color(0, 100, 200));
                p.add(btnEdit);
            } else {
                JLabel lLock = new JLabel("🔒");
                lLock.setForeground(Color.GRAY);
                p.add(lLock);
            }
            
            p.setOpaque(true);
            p.setBackground(new Color(235, 245, 255));

            // --- ACCIÓN 1: VER (SOLO LECTURA) ---
            btnVer.addActionListener(e -> {
                fireEditingStopped();
                abrirDetalle(false); // false = Sin permisos de escritura
            });

            // --- ACCIÓN 2: EDITAR (ESCRITURA) ---
            btnEdit.addActionListener(e -> {
                fireEditingStopped();
                abrirDetalle(true); // true = Con permisos (si los tenía asignados)
            });
        }
        
        private void abrirDetalle(boolean modoEdicion) {
            int fila = tablaRef.getSelectedRow();
            if (fila >= 0) {
                String idOrden = tablaRef.getValueAt(fila, 0).toString();
                
                // Abrimos el detalle pasando el modo específico seleccionado
                DetalleCompra detalle = new DetalleCompra(VentanaCompras.this, true, idOrden, modoEdicion);
                detalle.setVisible(true);
                
                // Al volver, refrescamos la tabla
                cargarOrdenesDesdeBD();
            }
        }

        private void estilizarBotonTabla(JButton b, Color c) {
            b.setForeground(c);
            b.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
            b.setBorder(new EmptyBorder(0, 5, 0, 5));
            b.setContentAreaFilled(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            // Aseguramos que el fondo del panel editor coincida con la selección
            p.setBackground(isSelected ? new Color(235, 245, 255) : Color.WHITE);
            return p;
        }
        @Override public Object getCellEditorValue() { return ""; }
    }

    private JPanel crearPanelPaginacionOrdenes(int x, int y, int w) {
        JPanel p = new JPanel(); p.setBounds(x, y, w, 50); p.setOpaque(false);
        p.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 10));
        lblInfoPaginacionOrdenes = new JLabel("Cargando..."); lblInfoPaginacionOrdenes.setFont(fontPlain); lblInfoPaginacionOrdenes.setForeground(Color.GRAY); p.add(lblInfoPaginacionOrdenes);
        btnPagPrevOrdenes = crearBotonFlecha(false); btnPagPrevOrdenes.addActionListener(e -> cambiarPaginaOrdenes(-1));
        btnPagNextOrdenes = crearBotonFlecha(true); btnPagNextOrdenes.addActionListener(e -> cambiarPaginaOrdenes(1));
        p.add(btnPagPrevOrdenes); p.add(btnPagNextOrdenes);
        return p;
    }

    // ... (MÉTODOS AUXILIARES Y SIDEBAR IGUAL QUE ANTES) ...
    private void crearSidebar(JPanel panelFondo) { /* ... MISMO CÓDIGO ... */ sidebar = new JPanel() { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(new Color(255, 255, 255, 10)); g2.fillRoundRect(15, 60, getWidth()-15, getHeight()-80, 25, 25); } }; sidebar.setBounds(0, 0, 240, 720); sidebar.setLayout(null); sidebar.setOpaque(false); panelFondo.add(sidebar); JLabel lblMenu = new JLabel("COMPRAS"); lblMenu.setFont(fontBold.deriveFont(16f)); lblMenu.setForeground(new Color(180, 200, 240)); lblMenu.setBounds(40, 80, 180, 20); sidebar.add(lblMenu); int btnY = 130; btnMenuDash = crearBotonSidebar("Dashboard", btnY, false); btnMenuProv = crearBotonSidebar("Proveedores", btnY + 60, true); btnMenuOrd = crearBotonSidebar("Órdenes de Compra", btnY + 120, false); btnMenuDash.addActionListener(e -> navegarA("DASHBOARD", btnMenuDash)); btnMenuProv.addActionListener(e -> navegarA("PROVEEDORES", btnMenuProv)); btnMenuOrd.addActionListener(e -> navegarA("ORDENES", btnMenuOrd)); sidebar.add(btnMenuDash); sidebar.add(btnMenuProv); sidebar.add(btnMenuOrd); }
    private JButton crearBotonSidebar(String texto, int y, boolean activo) { JButton btn = new JButton(texto); btn.setBounds(0, y, 240, 50); btn.setForeground(activo ? Color.WHITE : new Color(160, 170, 190)); btn.setFont(activo ? fontBold : fontPlain); btn.setHorizontalAlignment(SwingConstants.LEFT); btn.setBorderPainted(false); btn.setContentAreaFilled(false); btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setBorder(new EmptyBorder(0, 30, 0, 0)); return btn; }
    private void navegarA(String vista, JButton botonActivo) { cardLayout.show(panelContenido, vista); btnMenuDash.setForeground(new Color(160, 170, 190)); btnMenuDash.setFont(fontPlain); btnMenuProv.setForeground(new Color(160, 170, 190)); btnMenuProv.setFont(fontPlain); btnMenuOrd.setForeground(new Color(160, 170, 190)); btnMenuOrd.setFont(fontPlain); botonActivo.setForeground(Color.WHITE); botonActivo.setFont(fontBold); }
    private void agregarBotonesControl(JPanel panel, int ancho) { JLabel lblClose = new JLabel("X"); lblClose.setFont(fontBold.deriveFont(18f)); lblClose.setForeground(new Color(150, 150, 180)); lblClose.setBounds(ancho - 35, 15, 30, 30); lblClose.setCursor(new Cursor(Cursor.HAND_CURSOR)); lblClose.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { dispose(); } }); panel.add(lblClose); }
    private JPanel crearVistaDashboard() { JPanel p = new JPanel(null); p.setOpaque(false); JLabel lbl = new JLabel("Resumen de Compras"); lbl.setFont(fontBold.deriveFont(28f)); lbl.setForeground(Color.WHITE); lbl.setBounds(0,0,400,40); p.add(lbl); JPanel glass = crearPanelGlassClaro(0, 60, 880, 580); p.add(glass); glass.add(crearCardMetrica("Total Gastado", "$ 45K", 30, 30, new Color(100, 150, 255))); return p; }
    private JPanel crearCardMetrica(String t, String v, int x, int y, Color c) { JPanel card = new JPanel(null); card.setBounds(x, y, 260, 100); card.setBackground(Color.WHITE); JLabel l1 = new JLabel(t); l1.setBounds(20,20,200,20); card.add(l1); JLabel l2 = new JLabel(v); l2.setBounds(20,50,200,30); l2.setFont(fontBold.deriveFont(24f)); card.add(l2); return card; }
    private JPanel crearVistaProveedores() { JPanel p = new JPanel(null); p.setOpaque(false); JLabel lbl = new JLabel("Gestión de Proveedores"); lbl.setFont(fontBold.deriveFont(28f)); lbl.setForeground(Color.WHITE); lbl.setBounds(0, 0, 400, 40); p.add(lbl); JPanel glass = crearPanelGlassClaro(0, 60, 880, 580); p.add(glass); int controlsY = 25; txtBuscarProv = crearInputBlanco("Buscar proveedor..."); txtBuscarProv.setBounds(30, controlsY, 250, 40); txtBuscarProv.addKeyListener(new KeyAdapter() { @Override public void keyReleased(KeyEvent e) { filtrarDatos(txtBuscarProv.getText().trim()); } }); glass.add(txtBuscarProv); JButton btnFiltroID = crearBotonBlanco("ID", 290, controlsY, 50); glass.add(btnFiltroID); JButton btnNuevo = crearBotonBlanco("+ Nuevo", 720, controlsY, 130); btnNuevo.setForeground(new Color(0, 100, 200)); btnNuevo.addActionListener(e -> { FormularioProveedor form = new FormularioProveedor(this, true); form.setVisible(true); if (form.isGuardadoExitoso()) { cargarDatosDesdeBD(); } }); btnNuevo.setVisible(puedeEditarProveedores); glass.add(btnNuevo); String[] cols = {"ID", "Proveedor (Estado)", "RUC/Cédula", "Contacto", "Email", "Ciudad", "Acciones"}; Object[][] dataInicial = {}; JScrollPane scroll = crearTablaRedondeada(cols, dataInicial); scroll.setBounds(30, 90, 820, 400); glass.add(scroll); JPanel paginacion = crearPanelPaginacion(30, 510, 820); glass.add(paginacion); cargarDatosDesdeBD(); return p; }
    private void cargarDatosDesdeBD() { GestionProveedores gestion = new GestionProveedores(); listaMaestra = gestion.obtenerTodosLosProveedores(); listaFiltrada = new ArrayList<>(listaMaestra); paginaActual = 1; actualizarTabla(); }
    private void filtrarDatos(String texto) { if (texto.isEmpty()) { listaFiltrada = new ArrayList<>(listaMaestra); } else { String textoMin = texto.toLowerCase(); listaFiltrada = listaMaestra.stream().filter(fila -> { String id = fila[0].toString().toLowerCase(); String nombre = fila[1].toString().toLowerCase(); return id.contains(textoMin) || nombre.contains(textoMin); }).collect(Collectors.toList()); } paginaActual = 1; actualizarTabla(); }
    private void actualizarTabla() { modeloTablaProveedores.setRowCount(0); int totalRegistros = listaFiltrada.size(); int totalPaginas = (int) Math.ceil((double) totalRegistros / FILAS_X_PAGINA); if (totalPaginas == 0) totalPaginas = 1; if (paginaActual < 1) paginaActual = 1; if (paginaActual > totalPaginas) paginaActual = totalPaginas; int inicio = (paginaActual - 1) * FILAS_X_PAGINA; int fin = Math.min(inicio + FILAS_X_PAGINA, totalRegistros); for (int i = inicio; i < fin; i++) { modeloTablaProveedores.addRow(listaFiltrada.get(i)); } if (totalRegistros == 0) { lblInfoPaginacion.setText("Sin resultados"); inicio = 0; fin = 0; } else { lblInfoPaginacion.setText("Mostrando " + (inicio + 1) + "-" + fin + " de " + totalRegistros); } btnPagPrev.setEnabled(paginaActual > 1); btnPagNext.setEnabled(paginaActual < totalPaginas); }
    private void cambiarPagina(int delta) { paginaActual += delta; actualizarTabla(); }
    private JScrollPane crearTablaRedondeada(String[] cols, Object[][] data) { modeloTablaProveedores = new DefaultTableModel(data, cols) { @Override public boolean isCellEditable(int row, int col) { return col == cols.length - 1; } }; JTable table = new JTable(modeloTablaProveedores); table.setOpaque(false); table.setBackground(Color.WHITE); table.setForeground(new Color(60, 70, 80)); table.setRowHeight(50); table.setShowVerticalLines(false); table.setShowHorizontalLines(true); table.setGridColor(new Color(230, 230, 240)); table.setFont(fontPlain); if(cols.length > 0) table.getColumnModel().getColumn(0).setPreferredWidth(90); if(cols.length > 1) table.getColumnModel().getColumn(1).setPreferredWidth(200); if(cols.length > 2) table.getColumnModel().getColumn(2).setPreferredWidth(110); if(cols.length > 3) table.getColumnModel().getColumn(3).setPreferredWidth(90); if(cols.length > 4) table.getColumnModel().getColumn(4).setPreferredWidth(150); if(cols.length > 5) table.getColumnModel().getColumn(5).setPreferredWidth(90); JTableHeader header = table.getTableHeader(); header.setPreferredSize(new Dimension(header.getWidth(), 50)); if (cols.length > 1 && cols[1].contains("Proveedor")) { table.getColumnModel().getColumn(1).setCellRenderer(new NameWithStatusRenderer()); } table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() { @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) { Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); ((JLabel)c).setBorder(new EmptyBorder(0, 20, 0, 0)); c.setBackground(isSelected ? new Color(235, 245, 255) : Color.WHITE); c.setForeground(isSelected ? new Color(0, 100, 200) : new Color(60, 70, 80)); return c; } }); TableColumn colAcciones = table.getColumnModel().getColumn(cols.length - 1); colAcciones.setCellRenderer(new AccionesRenderer(puedeEditarProveedores)); colAcciones.setCellEditor(new AccionesEditor(new JCheckBox(), table, puedeEditarProveedores)); colAcciones.setPreferredWidth(100); JScrollPane scroll = new JScrollPane(table); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getViewport().setBackground(Color.WHITE); return scroll; }
    private JPanel crearPanelPaginacion(int x, int y, int w) { JPanel p = new JPanel(); p.setBounds(x, y, w, 50); p.setOpaque(false); p.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 10)); lblInfoPaginacion = new JLabel("Cargando..."); lblInfoPaginacion.setFont(fontPlain); lblInfoPaginacion.setForeground(Color.GRAY); p.add(lblInfoPaginacion); btnPagPrev = crearBotonFlecha(false); btnPagPrev.addActionListener(e -> cambiarPagina(-1)); btnPagNext = crearBotonFlecha(true); btnPagNext.addActionListener(e -> cambiarPagina(1)); p.add(btnPagPrev); p.add(btnPagNext); return p; }
    private JButton crearBotonFlecha(boolean der) { JButton b = new JButton(der ? ">" : "<"); b.setPreferredSize(new Dimension(35, 35)); b.setForeground(Color.GRAY); b.setBackground(Color.WHITE); b.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 240))); b.setContentAreaFilled(true); b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); return b; }
    private JButton crearBotonBlanco(String txt, int x, int y, int w) { JButton b = new JButton(txt); b.setBounds(x, y, w, 40); b.setBackground(Color.WHITE); b.setForeground(new Color(80, 90, 100)); b.setFont(fontBold); b.setFocusPainted(false); b.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 230))); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); return b; }
    private JTextField crearInputBlanco(String ph) { JTextField t = new JTextField(ph); t.setBackground(Color.WHITE); t.setForeground(Color.GRAY); t.setFont(fontPlain); t.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 220, 230)), new EmptyBorder(5, 10, 5, 10))); return t; }
    private JPanel crearPanelGlassClaro(int x, int y, int w, int h) { JPanel p = new JPanel() { @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(new Color(245, 250, 255, 230)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30); g2.setColor(new Color(255, 255, 255, 100)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 30, 30); } }; p.setBounds(x, y, w, h); p.setLayout(null); p.setOpaque(false); return p; }
    
    // =========================================================================
    // CLASES INTERNAS PARA LA TABLA DE PROVEEDORES (Lupita, Lápiz, Basurero)
    // =========================================================================

    // 1. RENDERER (Dibuja los iconos)
    class AccionesRenderer extends DefaultTableCellRenderer {
        boolean permisoEditar;
        
        public AccionesRenderer(boolean permisoEditar) { 
            this.permisoEditar = permisoEditar; 
        }

        @Override 
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
            p.setOpaque(true);
            p.setBackground(isSelected ? new Color(235, 245, 255) : Color.WHITE);
            
            // 1. Lupa (Siempre visible)
            JLabel lVer = new JLabel("🔍");
            lVer.setForeground(new Color(100, 60, 180));
            lVer.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
            lVer.setBorder(new EmptyBorder(0, 5, 0, 5));
            p.add(lVer);

            // 2. Edición (Solo si tiene permiso)
            if (permisoEditar) {
                JLabel lEdit = new JLabel("✎"); 
                lEdit.setForeground(new Color(0, 100, 200)); 
                lEdit.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
                
                JLabel lDel = new JLabel("🗑"); 
                lDel.setForeground(new Color(200, 60, 60)); 
                lDel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
                
                lEdit.setBorder(new EmptyBorder(0, 5, 0, 5)); 
                lDel.setBorder(new EmptyBorder(0, 5, 0, 5));
                p.add(lEdit); 
                p.add(lDel);
            } else {
                JLabel lLock = new JLabel("🔒"); 
                lLock.setForeground(Color.GRAY); 
                p.add(lLock);
            }
            return p;
        }
    }

    // 2. EDITOR (Maneja los clics en los botones)
    class AccionesEditor extends AbstractCellEditor implements TableCellEditor {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        JButton btnVer = new JButton("🔍");
        JButton btnEdit = new JButton("✎");
        JButton btnDel = new JButton("🗑");
        
        String idSeleccionado;
        boolean permisoEditar;

        public AccionesEditor(JCheckBox c, JTable table, boolean permisoEditar) {
            this.permisoEditar = permisoEditar;
            
            // Estilos
            estilizarBotonTabla(btnVer, new Color(100, 60, 180));
            p.add(btnVer);

            if (permisoEditar) {
                estilizarBotonTabla(btnEdit, new Color(0, 100, 200));
                estilizarBotonTabla(btnDel, new Color(200, 60, 60));
                p.add(btnEdit);
                p.add(btnDel);
            } else {
                JLabel lblLock = new JLabel("🔒");
                lblLock.setForeground(Color.GRAY);
                p.add(lblLock);
            }
            
            p.setOpaque(true);
            p.setBackground(new Color(235, 245, 255));

            // --- ACCIONES ---
            // A. VER (Lupita)
            btnVer.addActionListener(e -> {
                fireEditingStopped();
                GestionProveedores gestion = new GestionProveedores();
                Proveedor prov = gestion.obtenerProveedorPorId(idSeleccionado);
                if (prov != null) {
                    FormularioProveedor form = new FormularioProveedor(VentanaCompras.this, true);
                    form.cargarDatosParaEdicion(prov);
                    form.activarModoLectura(); // Solo lectura
                    form.setVisible(true);
                }
            });

            // B. EDITAR (Lápiz)
            btnEdit.addActionListener(e -> {
                fireEditingStopped();
                GestionProveedores gestion = new GestionProveedores();
                Proveedor prov = gestion.obtenerProveedorPorId(idSeleccionado);
                if (prov != null) {
                    FormularioProveedor form = new FormularioProveedor(VentanaCompras.this, true);
                    form.cargarDatosParaEdicion(prov);
                    form.setVisible(true);
                    if (form.isGuardadoExitoso()) cargarDatosDesdeBD();
                }
            });

            // C. ELIMINAR (Basurero)
            btnDel.addActionListener(e -> {
                fireEditingStopped();
                int confirm = JOptionPane.showConfirmDialog(null, 
                        "¿Eliminar este proveedor?", "Confirmar", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    GestionProveedores gestion = new GestionProveedores();
                    if (gestion.eliminarProveedorLogico(idSeleccionado)) {
                        cargarDatosDesdeBD();
                    } else {
                        JOptionPane.showMessageDialog(null, "Error al eliminar.");
                    }
                }
            });
        }
        
        private void estilizarBotonTabla(JButton b, Color c) {
            b.setForeground(c);
            b.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
            b.setBorder(new EmptyBorder(0, 5, 0, 5));
            b.setContentAreaFilled(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            idSeleccionado = table.getValueAt(row, 0).toString();
            return p;
        }
        @Override public Object getCellEditorValue() { return ""; }
    }
    
    // =========================================================================
    // CLASE FALTANTE: RENDERER PARA NOMBRE CON ESTADO (PUNTITO)
    // =========================================================================
    
    class NameWithStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = (value != null) ? value.toString() : "";
            
            // Lógica para detectar estado: 
            // Si el texto trae marcas como "#1" (activo) o "#0" (inactivo), las usamos.
            // Si no trae nada, asumimos que está activo (verde) por defecto.
            boolean activo = !text.contains("#0"); 
            String nombreReal = text.split("#")[0]; // Limpiamos el texto para que solo se vea el nombre

            JPanel p = new JPanel(null);
            p.setOpaque(true);
            
            // 1. CAMBIO DE FONDO (Debe coincidir con el resto de la tabla)
            p.setBackground(isSelected ? new Color(235, 245, 255) : Color.WHITE);
            
            // 2. DIBUJO DEL PUNTO (DOT)
            JPanel dotDraw = new JPanel() {
                 @Override 
                 protected void paintComponent(Graphics g) {
                     super.paintComponent(g); 
                     Graphics2D g2 = (Graphics2D)g; 
                     g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                     // Color: Verde si es activo, Rojo si es inactivo
                     g2.setColor(activo ? new Color(50, 200, 100) : new Color(200, 60, 60));
                     g2.fillOval(0, 0, 8, 8); // Dibuja el círculo
                 }
            };
            dotDraw.setBounds(10, 21, 10, 10); // Posición centrada verticalmente (para altura 50)
            dotDraw.setOpaque(false);
            p.add(dotDraw);

            // 3. ETIQUETA DEL NOMBRE
            JLabel lbl = new JLabel(nombreReal);
            lbl.setFont(fontPlain);
            // Color del texto: Azul si está seleccionado, Gris oscuro si no
            lbl.setForeground(isSelected ? new Color(0, 100, 200) : new Color(60, 70, 80));
            lbl.setBounds(30, 0, 250, 50); // Dejamos espacio a la izquierda para el punto
            p.add(lbl);
            
            return p;
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VentanaCompras.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new VentanaCompras().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
