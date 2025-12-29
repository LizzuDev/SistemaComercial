package modelo;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 *
 * @author jlcad
 */
public class VentanaLogin extends javax.swing.JFrame {
        
    // GestorGaleria gestor = new GestorGaleria(); 
    
    int xMouse, yMouse;

    public VentanaLogin() {
        setUndecorated(true);
        initComponents();
        aplicarDisenoDarkGlassRefinado();
    }

    private void aplicarDisenoDarkGlassRefinado() {
        // 1. Configuración de la ventana 
        // ANCHO REDUCIDO: De 850 a 750
        int anchoVentana = 750;
        int altoVentana = 500;
        setSize(anchoVentana, altoVentana);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0)); // Transparencia obligatoria

        // 2. PANEL DE FONDO: Azul Profundo con Orbes Estáticos
        JPanel panelFondo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // A. Fondo Base: Azul Muy Oscuro
                Color colorFondo1 = new Color(5, 12, 25); 
                Color colorFondo2 = new Color(2, 5, 15);
                GradientPaint gp = new GradientPaint(0, 0, colorFondo1, 0, getHeight(), colorFondo2);
                g2.setPaint(gp);
                // ESQUINAS MÁS REDONDAS: Radio aumentado a 50
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 50, 50);
                
                // B. Orbes de Luz Difusa
                dibujarOrbe(g2, getWidth()*0.2, getHeight()*0.2, 300, new Color(20, 80, 150, 60));
                dibujarOrbe(g2, getWidth()*0.8, getHeight()*0.1, 250, new Color(100, 150, 255, 50));
                dibujarOrbe(g2, getWidth()*0.1, getHeight()*0.9, 280, new Color(10, 50, 100, 40));
                dibujarOrbe(g2, getWidth()*0.9, getHeight()*0.8, 300, new Color(20, 60, 120, 50));
                
                // Borde de ventana sutil
                g2.setColor(new Color(255,255,255,20));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 50, 50);
            }
        };
        panelFondo.setLayout(null);
        setContentPane(panelFondo);

        // 3. TARJETA CENTRAL DE CRISTAL
        int anchoPanel = 525; // Ajustado al nuevo ancho
        int altoPanel = 370;
        int xPanel = (anchoVentana - anchoPanel) / 2;
        int yPanel = (altoVentana - altoPanel) / 2;
        
        JPanel panelCristal = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Fondo del Cristal
                g2.setColor(new Color(255, 255, 255, 10)); 
                // ESQUINAS DEL PANEL CRISTAL TAMBIÉN REDONDEADAS (Radio 40)
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                
                // Borde sutil del cristal
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 40, 40);
            }
        };
        panelCristal.setBounds(xPanel, yPanel, anchoPanel, altoPanel);
        panelCristal.setLayout(null);
        panelCristal.setOpaque(false);
        panelFondo.add(panelCristal);

        // --- ELEMENTOS DENTRO DEL CRISTAL ---
        
        // TÍTULO "ACME"
        JLabel lblTitulo = new JLabel("ACME");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 36)); 
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setBounds(0, 25, anchoPanel, 40);
        panelCristal.add(lblTitulo);
        
        // Subtítulo
        JLabel lblSub = new JLabel("Sistema de Gestión Comercial");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblSub.setForeground(new Color(200, 200, 200));
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        lblSub.setBounds(0, 65, anchoPanel, 20);
        panelCristal.add(lblSub);

        // FORMULARIO CENTRADO
        int wInput = 280;
        int xInput = (anchoPanel - wInput) / 2;
        int yStart = 100;

        // Usuario
        JLabel lblUser = new JLabel("USUARIO");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblUser.setForeground(Color.WHITE);
        lblUser.setBounds(xInput, yStart, wInput, 20);
        panelCristal.add(lblUser);

        txtUsuario.setBounds(xInput, yStart + 20, wInput - 30, 30); 
        estilizarInput(txtUsuario);
        panelCristal.add(txtUsuario);
        
        // Icono Usuario
        JLabel iconUser = crearIcono("user");
        iconUser.setBounds(xInput + wInput - 25, yStart + 25, 20, 20);
        panelCristal.add(iconUser);

        JSeparator sep1 = new JSeparator();
        sep1.setBounds(xInput, yStart + 50, wInput, 10);
        sep1.setForeground(Color.WHITE);
        panelCristal.add(sep1);

        // Contraseña
        JLabel lblPass = new JLabel("CONTRASEÑA");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblPass.setForeground(Color.WHITE);
        lblPass.setBounds(xInput, yStart + 60, wInput, 20);
        panelCristal.add(lblPass);

        txtContra.setBounds(xInput, yStart + 80, wInput - 30, 30);
        estilizarInput(txtContra);
        panelCristal.add(txtContra);
        
        // Icono Ojo
        JLabel iconEye = crearIcono("eye");
        iconEye.setBounds(xInput + wInput - 25, yStart + 85, 20, 20);
        panelCristal.add(iconEye);

        JSeparator sep2 = new JSeparator();
        sep2.setBounds(xInput, yStart + 110, wInput, 10);
        sep2.setForeground(Color.WHITE);
        panelCristal.add(sep2);

        // BOTÓN BLANCO CON BORDES BRILLOSOS
        JButton btnLogin = new JButton("INICIAR SESIÓN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 1. Relleno Blanco (con un degradado muy sutil perla)
                GradientPaint relleno = new GradientPaint(0, 0, Color.WHITE, 0, getHeight(), new Color(230, 230, 240));
                g2.setPaint(relleno);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 35, 35);
                
                // 2. Borde "Brillante" (Glow sutil blanco puro sobre el relleno)
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(Color.WHITE); // Borde blanco puro brillante
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 35, 35);

                super.paintComponent(g);
            }
        };
        btnLogin.setBounds(xInput, yStart + 155, wInput, 40);
        
        // Texto Azul Oscuro para contraste con el botón blanco
        btnLogin.setForeground(new Color(10, 30, 60)); 
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBorderPainted(false);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnLogin.addActionListener(this::btnIngresarActionPerformed);
        panelCristal.add(btnLogin);
        
        btnIngresar.setVisible(false);

        // CONTROLES DE VENTANA
        agregarBotonesControl(panelFondo, anchoVentana);

        // ARRASTRE
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
    }
    
    private void dibujarOrbe(Graphics2D g2, double x, double y, float radio, Color c) {
        Point2D center = new Point2D.Double(x, y);
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {c, new Color(0, 0, 0, 0)};
        RadialGradientPaint p = new RadialGradientPaint(center, radio, dist, colors);
        g2.setPaint(p);
        g2.fillOval((int)(x-radio), (int)(y-radio), (int)(radio*2), (int)(radio*2));
    }
    
    private void estilizarInput(JTextField txt) {
        txt.setBackground(new Color(0,0,0,0));
        txt.setForeground(Color.WHITE);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.setBorder(null);
        txt.setCaretColor(Color.WHITE);
        txt.setOpaque(false);
    }

    private JLabel crearIcono(String tipo) {
        return new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                if (tipo.equals("user")) {
                    g2.drawOval(5, 0, 10, 10);
                    g2.drawArc(0, 12, 20, 10, 0, 180);
                } else {
                    g2.drawOval(0, 5, 20, 10);
                    g2.fillOval(7, 7, 6, 6);
                    g2.drawLine(0, 18, 20, 2); 
                }
            }
        };
    }

    private void agregarBotonesControl(JPanel panel, int anchoVentana) {
        JLabel lblClose = new JLabel("X");
        lblClose.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblClose.setForeground(new Color(150, 150, 180));
        lblClose.setBounds(anchoVentana - 35, 10, 30, 30);
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
        lblMin.setBounds(anchoVentana - 65, 10, 30, 30);
        lblMin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblMin.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { setExtendedState(ICONIFIED); }
        });
        panel.add(lblMin);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        btnIngresar = new javax.swing.JButton();
        txtContra = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Usuario");

        jLabel2.setText("Contraseña");

        txtUsuario.addActionListener(this::txtUsuarioActionPerformed);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel3.setText("Login");

        btnIngresar.setText("Ingresar");
        btnIngresar.addActionListener(this::btnIngresarActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(83, 83, 83)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addGap(54, 54, 54)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtUsuario)
                            .addComponent(txtContra, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(155, 155, 155)
                        .addComponent(btnIngresar))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(175, 175, 175)
                        .addComponent(jLabel3)))
                .addContainerGap(62, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(80, 80, 80)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtContra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnIngresar)
                .addContainerGap(68, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtUsuarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtUsuarioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtUsuarioActionPerformed

    private void btnIngresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIngresarActionPerformed
       String user = txtUsuario.getText();
        String pass = new String(txtContra.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Ingrese usuario y contraseña");
            return;
        }

        // Llamamos a la lógica de base de datos
        modelo.GestionUsuarios gestion = new modelo.GestionUsuarios();
        modelo.Usuario usu = gestion.login(user, pass);

        if (usu != null) {
            // LOGIN ÉXITO: Guardamos la sesión
            modelo.Sesion.setUsuarioActual(usu);
            
            // Pasamos el nombre del usuario al constructor de la principal
            new VentanaPrincipal(usu.getNombreCompleto()).setVisible(true);
            this.dispose();
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Credenciales incorrectas", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnIngresarActionPerformed

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
            java.util.logging.Logger.getLogger(VentanaLogin.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new VentanaLogin().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnIngresar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPasswordField txtContra;
    private javax.swing.JTextField txtUsuario;
    // End of variables declaration//GEN-END:variables
}
