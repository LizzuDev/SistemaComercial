package modelo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class VentanaPrincipal extends JFrame {

    // Variables de estado
    private int xMouse, yMouse;
    private double angle = 0; // Para la animación del fondo
    private String usuarioNombre;
    
    // VARIABLES GLOBALES DE LOS BOTONES DEL MENÚ (Necesarias para los permisos)
    private JButton btnMenuDash;
    private JButton btnMenuCompras;
    private JButton btnMenuVentas;
    private JButton btnMenuInventario;
    private JButton btnMenuRRHH;
    private JButton btnMenuConta;
    
    private JPanel cardCompras, cardInventarios, cardVentas, cardRRHH, cardConta;

    // Constructor que recibe el nombre del usuario (Login Exitoso)
    public VentanaPrincipal(String usuario) {
        this.usuarioNombre = usuario;
        setUndecorated(true);
        initComponents();
        aplicarDisenoDashboard();
    }
    
    // Constructor por defecto
    public VentanaPrincipal() {
        this.usuarioNombre = "Invitado";
        setUndecorated(true);
        initComponents();
        aplicarDisenoDashboard();
    }

    private void aplicarDisenoDashboard() {
        // 1. Configuración de la Ventana
        int ancho = 1100;
        int alto = 700;
        setSize(ancho, alto);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));

        // 2. PANEL DE FONDO ANIMADO
        JPanel panelFondo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo Base Oscuro
                Color colorFondo1 = new Color(5, 12, 25);
                Color colorFondo2 = new Color(2, 5, 15);
                GradientPaint gp = new GradientPaint(0, 0, colorFondo1, 0, getHeight(), colorFondo2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);

                // Orbes de Luz Animados
                double x1 = getWidth() * 0.8 + Math.cos(angle) * 40;
                double y1 = getHeight() * 0.2 + Math.sin(angle) * 40;
                dibujarOrbe(g2, x1, y1, 400, new Color(20, 60, 150, 40));

                double x2 = getWidth() * 0.2 - Math.sin(angle) * 30;
                dibujarOrbe(g2, x2, getHeight() * 0.8, 350, new Color(100, 150, 255, 30));

                // Borde sutil
                g2.setColor(new Color(255, 255, 255, 20));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 40, 40);
            }
        };
        panelFondo.setLayout(null);
        setContentPane(panelFondo);

        // Timer de Animación
        Timer timer = new Timer(50, e -> {
            angle += 0.02;
            panelFondo.repaint();
        });
        timer.start();

        // 3. SIDEBAR (Menú Lateral de Cristal)
        int anchoSidebar = 260;
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Cristal vertical izquierdo
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(10, 10, getWidth()-20, getHeight()-20, 30, 30);
                // Borde
                g2.setColor(new Color(255, 255, 255, 20));
                g2.drawRoundRect(10, 10, getWidth()-20, getHeight()-20, 30, 30);
            }
        };
        sidebar.setBounds(0, 0, anchoSidebar, alto);
        sidebar.setLayout(null);
        sidebar.setOpaque(false);
        panelFondo.add(sidebar);

        // Logo en Sidebar
        JLabel lblLogo = new JLabel("ACME");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setBounds(40, 40, 200, 40);
        sidebar.add(lblLogo);
        
        JLabel lblSys = new JLabel("ERP SYSTEM");
        lblSys.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSys.setForeground(new Color(150, 180, 200));
        lblSys.setBounds(42, 75, 200, 20);
        sidebar.add(lblSys);

        // =======================================================
        // BOTONES DEL MENÚ (Instanciación y Posicionamiento)
        // =======================================================
        int yMenu = 140;
        int gap = 60; // Espacio entre botones

        // 1. Dashboard (Siempre visible)
        btnMenuDash = crearBotonMenu("Dashboard", yMenu, true);
        sidebar.add(btnMenuDash);

        // 2. Compras
        btnMenuCompras = crearBotonMenu("Compras", yMenu + gap, false);
        btnMenuCompras.addActionListener(e -> abrirVentanaCompras());
        sidebar.add(btnMenuCompras);

        // 3. Ventas
        btnMenuVentas = crearBotonMenu("Ventas", yMenu + gap * 2, false);
        sidebar.add(btnMenuVentas);
        
        // 4. Inventarios
        btnMenuInventario = crearBotonMenu("Inventarios", yMenu + gap * 3, false);
        sidebar.add(btnMenuInventario);

        // 5. Recursos Humanos
        btnMenuRRHH = crearBotonMenu("R. Humanos", yMenu + gap * 4, false);
        sidebar.add(btnMenuRRHH);
        
        // 6. Contabilidad
        btnMenuConta = crearBotonMenu("Contabilidad", yMenu + gap * 5, false);
        sidebar.add(btnMenuConta);
        
        // Botón Salir (Abajo)
        JButton btnSalir = crearBotonMenu("Cerrar Sesión", alto - 80, false);
        btnSalir.setForeground(new Color(255, 100, 100)); // Rojo suave
        btnSalir.addActionListener(e -> {
            System.exit(0);
        });
        sidebar.add(btnSalir);

        // 4. HEADER (Encabezado)
        JLabel lblTitle = new JLabel("Panel Principal");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setBounds(anchoSidebar + 30, 40, 300, 30);
        panelFondo.add(lblTitle);

        JLabel lblUser = new JLabel("Hola, " + usuarioNombre);
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblUser.setForeground(new Color(200, 200, 200));
        lblUser.setHorizontalAlignment(SwingConstants.RIGHT);
        lblUser.setBounds(ancho - 250, 45, 150, 20);
        panelFondo.add(lblUser);
        
        // Avatar
        JPanel avatar = new JPanel() {
             @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(100, 150, 255));
                g2.fillOval(0,0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String inicial = (usuarioNombre.length() > 0) ? usuarioNombre.substring(0, 1).toUpperCase() : "U";
                g2.drawString(inicial, 12, 22);
            }
        };
        avatar.setBounds(ancho - 80, 35, 35, 35);
        avatar.setOpaque(false);
        panelFondo.add(avatar);

        // 5. GRID DE TARJETAS (Módulos)
        int startX = anchoSidebar + 30;
        int startY = 120;
        int gapCard = 20;
        int cardW = 240;
        int cardH = 160;

        // --- FILA 1 ---
        
        // Tarjeta COMPRAS (Asignamos a la variable global cardCompras)
        cardCompras = crearTarjetaModulo("COMPRAS", "Gestión de Proveedores", "cart", startX, startY, cardW, cardH);
        cardCompras.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new VentanaCompras().setVisible(true); // Abre el módulo
            }
        });
        panelFondo.add(cardCompras);

        // Tarjeta INVENTARIOS
        cardInventarios = crearTarjetaModulo("INVENTARIOS", "Stock y Almacenes", "box", startX + cardW + gapCard, startY, cardW, cardH);
        panelFondo.add(cardInventarios);
        
        // Tarjeta VENTAS
        cardVentas = crearTarjetaModulo("VENTAS", "Facturación y Clientes", "graph", startX + (cardW + gapCard) * 2, startY, cardW, cardH);
        panelFondo.add(cardVentas);

        // --- FILA 2 ---
        
        // Tarjeta RRHH
        cardRRHH = crearTarjetaModulo("R. HUMANOS", "Personal y Nómina", "users", startX, startY + cardH + gapCard, cardW, cardH);
        panelFondo.add(cardRRHH);
        
        // Tarjeta CONTABILIDAD
        cardConta = crearTarjetaModulo("CONTABILIDAD", "Finanzas y Reportes", "money", startX + cardW + gapCard, startY + cardH + gapCard, cardW * 2 + gapCard, cardH);
        panelFondo.add(cardConta);
        
        // 6. CONTROLES DE VENTANA
        agregarBotonesControl(panelFondo, ancho);

        // 7. ARRASTRE
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) { xMouse = evt.getX(); yMouse = evt.getY(); }
            @Override
            public void mouseDragged(MouseEvent evt) {
                int x = evt.getXOnScreen();
                int y = evt.getYOnScreen();
                setLocation(x - xMouse, y - yMouse);
            }
        };
        panelFondo.addMouseListener(ma);
        panelFondo.addMouseMotionListener(ma);
        
        // =======================================================
        // APLICAR LÓGICA DE SEGURIDAD AL FINAL DE LA CREACIÓN
        // =======================================================
        aplicarPermisosRol();
    }

    // =======================================================
    // LÓGICA DE SEGURIDAD (ROLES)
    // =======================================================
    private void aplicarPermisosRol() {
        modelo.Usuario u = modelo.Sesion.getUsuarioActual();
        if (u == null) return; 

        String rol = u.getIdRol(); 

        // 1. OCULTAR TODO POR DEFECTO (Botones y Tarjetas)
        // --- Botones Sidebar ---
        btnMenuCompras.setVisible(false);
        btnMenuVentas.setVisible(false);
        btnMenuInventario.setVisible(false);
        btnMenuRRHH.setVisible(false);
        btnMenuConta.setVisible(false);
        
        // --- Tarjetas Dashboard (NUEVO) ---
        cardCompras.setVisible(false);
        cardVentas.setVisible(false);
        cardInventarios.setVisible(false);
        cardRRHH.setVisible(false);
        cardConta.setVisible(false);
        
        // 2. Definir "Equipo de Compras"
        boolean esEquipoCompras = rol.equals("ROL-ANA") || 
                                  rol.equals("ROL-COO") || 
                                  rol.equals("ROL-PRO") || 
                                  rol.equals("ROL-PLA") || 
                                  rol.equals("ROL-GER") || 
                                  rol.equals("ROL-JEF") || 
                                  rol.equals("ROL-AUX");

        // 3. APLICAR REGLAS (Mostrar lo que corresponda)
        
        // --- ADMIN (Ve todo) ---
        if (rol.equals("ROL-ADM")) {
            // Mostrar Botones
            btnMenuCompras.setVisible(true);
            btnMenuVentas.setVisible(true);
            btnMenuInventario.setVisible(true);
            btnMenuRRHH.setVisible(true);
            btnMenuConta.setVisible(true);
            
            // Mostrar Tarjetas
            cardCompras.setVisible(true);
            cardVentas.setVisible(true);
            cardInventarios.setVisible(true);
            cardRRHH.setVisible(true);
            cardConta.setVisible(true);
        }
        
        // --- COMPRAS ---
        else if (esEquipoCompras) {
            btnMenuCompras.setVisible(true); // Botón
            cardCompras.setVisible(true);    // Tarjeta
        }
        
        // --- VENTAS ---
        else if (rol.equals("ROL-VEND")) {
            btnMenuVentas.setVisible(true);
            cardVentas.setVisible(true);
        }
        
        // --- CONTABILIDAD ---
        else if (rol.equals("ROL-CONT")) {
            btnMenuConta.setVisible(true);
            btnMenuCompras.setVisible(true);
            
            cardConta.setVisible(true);
            cardCompras.setVisible(true);
        }
    }

    private void abrirVentanaCompras() {
        // Abrir la ventana de compras
        new VentanaCompras().setVisible(true);
    }

    // --- MÉTODOS AUXILIARES DE DISEÑO ---

    private JButton crearBotonMenu(String texto, int y, boolean activo) {
        JButton btn = new JButton(texto) {
             @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (activo) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                    g2.setColor(new Color(100, 200, 255)); 
                    g2.fillRect(0, 10, 4, getHeight()-20);
                }
                super.paintComponent(g);
            }
        };
        btn.setBounds(20, y, 220, 45);
        btn.setForeground(activo ? Color.WHITE : new Color(180, 180, 180));
        btn.setFont(new Font("Segoe UI", activo ? Font.BOLD : Font.PLAIN, 14));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if(!activo) btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { if(!activo) btn.setForeground(new Color(180, 180, 180)); }
        });
        return btn;
    }

    private JPanel crearTarjetaModulo(String titulo, String desc, String iconoTipo, int x, int y, int w, int h) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 40, 60, 150)); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                GradientPaint gp = new GradientPaint(0, 0, new Color(255,255,255,15), 0, getHeight(), new Color(0,0,0,0));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 25, 25);
                dibujarIconoModulo(g2, iconoTipo, getWidth()-60, 25);
            }
        };
        card.setBounds(x, y, w, h);
        card.setLayout(null);
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblT = new JLabel(titulo);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblT.setForeground(Color.WHITE);
        lblT.setBounds(20, 25, w-60, 25);
        card.add(lblT);
        
        JLabel lblD = new JLabel("<html>" + desc + "</html>");
        lblD.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblD.setForeground(new Color(170, 180, 200));
        lblD.setVerticalAlignment(SwingConstants.TOP);
        lblD.setBounds(20, 55, w-40, 40);
        card.add(lblD);

        JLabel lblGo = new JLabel("→");
        lblGo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblGo.setForeground(new Color(100, 200, 255));
        lblGo.setBounds(w - 40, h - 40, 30, 30);
        card.add(lblGo);
        
        return card;
    }
    
    private void dibujarIconoModulo(Graphics2D g2, String tipo, int x, int y) {
        g2.setColor(new Color(100, 200, 255, 200)); 
        g2.setStroke(new BasicStroke(2f));
        
        if (tipo.equals("cart")) {
            g2.drawRoundRect(x, y, 25, 20, 5, 5);
            g2.drawLine(x+5, y+5, x+20, y+5);
            g2.fillOval(x+5, y+22, 5, 5);
            g2.fillOval(x+18, y+22, 5, 5);
        } else if (tipo.equals("box")) {
            g2.drawRect(x, y+5, 25, 20);
            g2.drawLine(x, y+10, x+25, y+10);
            g2.fillRect(x+10, y+2, 5, 3);
        } else if (tipo.equals("graph")) {
            g2.drawLine(x, y+25, x+25, y+25); 
            g2.drawLine(x, y, x, y+25);       
            int[] px = {x, x+8, x+16, x+25};
            int[] py = {y+20, y+15, y+8, y+2};
            g2.drawPolyline(px, py, 4);
        } else if (tipo.equals("users")) {
            g2.drawOval(x+5, y, 10, 10); 
            g2.drawArc(x, y+12, 20, 15, 0, 180); 
        } else if (tipo.equals("money")) {
            g2.drawRoundRect(x, y+2, 28, 18, 5, 5);
            g2.drawString("$", x+10, y+16);
        }
    }

    private void dibujarOrbe(Graphics2D g2, double x, double y, float radio, Color c) {
        Point2D center = new Point2D.Double(x, y);
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {c, new Color(0, 0, 0, 0)};
        RadialGradientPaint p = new RadialGradientPaint(center, radio, dist, colors);
        g2.setPaint(p);
        g2.fillOval((int) (x - radio), (int) (y - radio), (int) (radio * 2), (int) (radio * 2));
    }

    private void agregarBotonesControl(JPanel panel, int ancho) {
        JLabel lblClose = new JLabel("X");
        lblClose.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblClose.setForeground(new Color(150, 150, 180));
        lblClose.setBounds(ancho - 35, 10, 30, 30);
        lblClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblClose.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { lblClose.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { lblClose.setForeground(new Color(150, 150, 180)); }
        });
        panel.add(lblClose);

        JLabel lblMin = new JLabel("—");
        lblMin.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblMin.setForeground(new Color(150, 150, 180));
        lblMin.setBounds(ancho - 65, 10, 30, 30);
        lblMin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblMin.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { setExtendedState(ICONIFIED); }
            public void mouseEntered(MouseEvent e) { lblMin.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { lblMin.setForeground(new Color(150, 150, 180)); }
        });
        panel.add(lblMin);
    }

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
            java.util.logging.Logger.getLogger(VentanaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
