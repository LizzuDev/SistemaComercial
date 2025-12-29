package modelo;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class FormularioProveedor extends JDialog {

    private int xMouse, yMouse;
    private boolean guardadoExitoso = false;
    private boolean esEdicion = false; 
    
    // COMPONENTES GLOBALES
    private JLabel lblTitulo;
    private JButton btnGuardar; 
    private JTextField txtID, txtNombre, txtRUC, txtTelefono, txtCelular, txtEmail, txtDireccion;
    private JComboBox<Ciudad> cmbCiudad;
    
    private Font fontBold = new Font("SansSerif", Font.BOLD, 13);
    private Font fontPlain = new Font("SansSerif", Font.PLAIN, 13);

    public FormularioProveedor(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        setUndecorated(true);
        setSize(500, 650);
        setLocationRelativeTo(parent);
        setBackground(new Color(0,0,0,0));
        
        construirInterfazExclusiva(); 
        cargarCiudades();
        
        // Configurar restricciones de entrada para el RUC/Cédula
        configurarInputRuc();

        // Por defecto: Modo Insertar
        generarIdAutomatico();
    }
    
    // --- NUEVO: CONFIGURACIÓN DE RESTRICCIONES PARA RUC/CÉDULA ---
    private void configurarInputRuc() {
        txtRUC.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                String text = txtRUC.getText();
                
                // 1. Solo permitir números
                if (!Character.isDigit(c)) {
                    e.consume();
                    return;
                }
                
                // 2. Limitar longitud máxima a 13 (RUC)
                // Si el usuario quiere ingresar Cédula, se detendrá en 10.
                // Si quiere RUC, el sistema le deja seguir hasta 13.
                if (text.length() >= 13) {
                    e.consume();
                }
            }
        });
    }

    // --- MODO SOLO LECTURA (LUPITA) ---
    public void activarModoLectura() {
        lblTitulo.setText("Detalles del Proveedor");
        // Bloqueamos edición y quitamos el cursor
        txtNombre.setEditable(false); txtNombre.setFocusable(false);
        txtRUC.setEditable(false); txtRUC.setFocusable(false);
        txtTelefono.setEditable(false); txtTelefono.setFocusable(false);
        txtCelular.setEditable(false); txtCelular.setFocusable(false);
        txtEmail.setEditable(false); txtEmail.setFocusable(false);
        txtDireccion.setEditable(false); txtDireccion.setFocusable(false);
        cmbCiudad.setEnabled(false); 
        
        // Ocultar botón guardar
        btnGuardar.setVisible(false);
    }

    public void cargarDatosParaEdicion(Proveedor p) {
        this.esEdicion = true;
        lblTitulo.setText("Editar Proveedor"); 
        
        txtID.setText(p.getId());
        txtNombre.setText(p.getNombre());
        txtRUC.setText(p.getRuc());
        txtTelefono.setText(p.getTelefono());
        txtCelular.setText(p.getCelular());
        txtEmail.setText(p.getEmail());
        txtDireccion.setText(p.getDireccion());
        
        if (p.getIdCiudad() != null) {
            for (int i = 0; i < cmbCiudad.getItemCount(); i++) {
                Ciudad c = cmbCiudad.getItemAt(i);
                if (c.getId().equals(p.getIdCiudad())) {
                    cmbCiudad.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void generarIdAutomatico() {
        if (!esEdicion) { 
            GestionProveedores gestion = new GestionProveedores();
            String nuevoId = gestion.generarSiguienteId();
            txtID.setText(nuevoId);
        }
    }

    // --- VALIDACIONES COMPLETAS (CON ALGORITMO ECUADOR INTEGRADO) ---
    private boolean validarFormulario() {
        // 1. Obtenemos los textos limpios
        String nombre = txtNombre.getText().trim();
        String ruc = txtRUC.getText().trim();
        String telf = txtTelefono.getText().trim();
        String cel = txtCelular.getText().trim();
        String mail = txtEmail.getText().trim();
        String dir = txtDireccion.getText().trim();

        // 2. VALIDACIÓN DE CAMPOS VACÍOS
        if (nombre.isEmpty() || ruc.isEmpty() || telf.isEmpty() || 
            cel.isEmpty() || mail.isEmpty() || dir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Los campos no pueden ser nulos", "Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // 3. VALIDACIÓN DE NÚMEROS EN EL NOMBRE
        if (nombre.matches(".*\\d.*")) {
            JOptionPane.showMessageDialog(this, "El nombre no puede contener números", "Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // 4. VALIDACIÓN DE CARACTERES ESPECIALES EN EL NOMBRE
        if (!nombre.matches("^[a-zA-Z ñÑáéíóúÁÉÍÓÚ.]+$")) {
             JOptionPane.showMessageDialog(this, "El nombre contiene caracteres inválidos.", "Error", JOptionPane.WARNING_MESSAGE);
             return false;
        }

        // 5. VALIDACIÓN AVANZADA DE RUC/CÉDULA (ALGORITMO ECUADOR)
        if (!validarIdentificacionEcuador(ruc)) {
            JOptionPane.showMessageDialog(this, "El número de Cédula o RUC ingresado es INCORRECTO (Error de verificación).", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validar teléfono y celular
        if (!telf.matches("\\d+") || !cel.matches("\\d+")) {
             JOptionPane.showMessageDialog(this, "Teléfono y celular deben contener solo números.", "Error", JOptionPane.WARNING_MESSAGE);
             return false;
        }

        // Validar formato de correo
        if (mail.chars().filter(ch -> ch == '@').count() != 1) {
            JOptionPane.showMessageDialog(this, "El correo debe tener un formato válido.", "Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // 6. VALIDACIÓN DE DUPLICADOS EN BD
        GestionProveedores gestion = new GestionProveedores();
        String idExcluir = esEdicion ? txtID.getText() : null;
        
        if (gestion.existeProveedorConNombre(nombre, idExcluir)) {
            JOptionPane.showMessageDialog(this, "Ya existe un proveedor activo con ese nombre.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (gestion.existeProveedorConRUC(ruc, idExcluir)) {
            JOptionPane.showMessageDialog(this, "Ya existe un proveedor activo con ese RUC.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    // =========================================================================
    // ALGORITMO DE VALIDACIÓN ECUADOR (TRADUCIDO DE JS A JAVA)
    // =========================================================================
    private boolean validarIdentificacionEcuador(String identificacion) {
        if (identificacion == null) return false;
        String valor = identificacion.trim();

        // Debe ser numérico
        if (!valor.matches("\\d+")) return false;

        // Validación de longitud (10 para Cédula, 13 para RUC)
        int longitud = valor.length();
        if (longitud != 10 && longitud != 13) return false;

        // Validación de Provincia (dos primeros dígitos entre 1 y 24)
        int codigoProvincia = Integer.parseInt(valor.substring(0, 2));
        if (codigoProvincia < 1 || codigoProvincia > 24) return false;

        // Validación del tercer dígito
        int tercerDigito = Character.getNumericValue(valor.charAt(2));

        if (longitud == 10) {
            // --- CASO CÉDULA ---
            // El tercer dígito debe ser menor a 6
            if (tercerDigito >= 6) return false;
            return validarAlgoritmoModulo10(valor.substring(0, 9), Character.getNumericValue(valor.charAt(9)));
        } 
        else if (longitud == 13) {
            // --- CASO RUC ---
            
            // Los últimos tres dígitos no pueden ser 000
            String establecimiento = valor.substring(10, 13);
            if (establecimiento.equals("000")) return false;

            // RUC Personas Naturales (3er dígito < 6)
            if (tercerDigito < 6) {
                return validarAlgoritmoModulo10(valor.substring(0, 9), Character.getNumericValue(valor.charAt(9)));
            }
            // RUC Público (3er dígito == 6)
            else if (tercerDigito == 6) {
                // Coeficientes: 3, 2, 7, 6, 5, 4, 3, 2
                // Dígito verificador en posición 9 (índice 8)
                return validarAlgoritmoModulo11(valor, new int[]{3, 2, 7, 6, 5, 4, 3, 2}, 8);
            }
            // RUC Jurídico / Extranjeros (3er dígito == 9)
            else if (tercerDigito == 9) {
                // Coeficientes: 4, 3, 2, 7, 6, 5, 4, 3, 2
                // Dígito verificador en posición 10 (índice 9)
                return validarAlgoritmoModulo11(valor, new int[]{4, 3, 2, 7, 6, 5, 4, 3, 2}, 9);
            }
        }
        return false;
    }

    private boolean validarAlgoritmoModulo10(String digitosIniciales, int digitoVerificador) {
        int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;

        for (int i = 0; i < coeficientes.length; i++) {
            int valor = Character.getNumericValue(digitosIniciales.charAt(i)) * coeficientes[i];
            if (valor >= 10) {
                valor = valor - 9;
            }
            suma += valor;
        }

        int residuo = suma % 10;
        int resultado = (residuo == 0) ? 0 : 10 - residuo;

        return resultado == digitoVerificador;
    }

    private boolean validarAlgoritmoModulo11(String valor, int[] coeficientes, int posicionVerificador) {
        int suma = 0;
        for (int i = 0; i < coeficientes.length; i++) {
            suma += Character.getNumericValue(valor.charAt(i)) * coeficientes[i];
        }

        int residuo = suma % 11;
        int resultado = (residuo == 0) ? 0 : 11 - residuo;
        
        int digitoVerificadorReal = Character.getNumericValue(valor.charAt(posicionVerificador));

        return resultado == digitoVerificadorReal;
    }
    // =========================================================================

    private void guardarDatos() {
        if (!validarFormulario()) {
            return;
        }

        GestionProveedores gestion = new GestionProveedores();
        Ciudad ciudadSeleccionada = (Ciudad) cmbCiudad.getSelectedItem();
        String idCiudad = (ciudadSeleccionada != null) ? ciudadSeleccionada.getId() : null;

        boolean exito;
        
        if (esEdicion) {
            Proveedor p = new Proveedor(
                txtID.getText(), 
                txtNombre.getText().trim(),
                txtRUC.getText().trim(),
                txtTelefono.getText().trim(),
                txtCelular.getText().trim(),
                txtEmail.getText().trim(),
                txtDireccion.getText().trim(),
                idCiudad
            );
            exito = gestion.actualizarProveedor(p);
        } else {
            exito = gestion.registrarProveedor(
                txtID.getText(), 
                txtNombre.getText().trim(),
                txtRUC.getText().trim(),
                txtTelefono.getText().trim(),
                txtCelular.getText().trim(),
                txtEmail.getText().trim(),
                txtDireccion.getText().trim(),
                idCiudad
            );
        }

        if (exito) {
            String msg = esEdicion ? "Proveedor actualizado correctamente." : "Proveedor registrado exitosamente.";
            JOptionPane.showMessageDialog(this, msg);
            guardadoExitoso = true; 
            dispose(); 
        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar en la base de datos.", "Error Crítico", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void construirInterfazExclusiva() {
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

        lblTitulo = new JLabel("Nuevo Proveedor");
        lblTitulo.setFont(fontBold.deriveFont(24f));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setBounds(30, 20, 300, 30);
        panelFondo.add(lblTitulo);

        JPanel glass = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 10)); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
            }
        };
        glass.setLayout(null);
        glass.setBounds(30, 70, 440, 500);
        glass.setOpaque(false);
        panelFondo.add(glass);

        int y = 30;
        int gap = 55;

        crearEtiqueta("ID (Automático):", 30, y, glass);
        txtID = crearInput(30, y + 20, 180, glass);
        txtID.setEditable(false);
        txtID.setForeground(new Color(255, 255, 100));
        txtID.setFocusable(false); 

        crearEtiqueta("RUC / Cédula (10-13 dígitos):", 230, y, glass);
        txtRUC = crearInput(230, y + 20, 180, glass);

        y += gap;
        crearEtiqueta("Razón Social / Nombre:", 30, y, glass);
        txtNombre = crearInput(30, y + 20, 380, glass);

        y += gap;
        crearEtiqueta("Teléfono Fijo:", 30, y, glass);
        txtTelefono = crearInput(30, y + 20, 180, glass);

        crearEtiqueta("Celular:", 230, y, glass);
        txtCelular = crearInput(230, y + 20, 180, glass);

        y += gap;
        crearEtiqueta("Correo Electrónico:", 30, y, glass);
        txtEmail = crearInput(30, y + 20, 380, glass);

        y += gap;
        crearEtiqueta("Ciudad:", 30, y, glass);
        cmbCiudad = new JComboBox<>();
        cmbCiudad.setBounds(30, y + 20, 180, 35);
        cmbCiudad.setFont(fontPlain);
        glass.add(cmbCiudad);

        crearEtiqueta("Dirección:", 30, y + gap, glass);
        txtDireccion = crearInput(30, y + gap + 20, 380, glass);

        btnGuardar = new JButton("Guardar Datos");
        btnGuardar.setBounds(250, 590, 150, 40);
        btnGuardar.setBackground(Color.WHITE);
        btnGuardar.setForeground(new Color(0, 100, 200));
        btnGuardar.setFont(fontBold);
        btnGuardar.setFocusPainted(false);
        btnGuardar.addActionListener(e -> guardarDatos());
        panelFondo.add(btnGuardar);

        JButton btnCancelar = new JButton("Cerrar");
        btnCancelar.setBounds(100, 590, 130, 40);
        btnCancelar.setContentAreaFilled(false);
        btnCancelar.setForeground(new Color(200, 200, 220)); 
        btnCancelar.setFont(fontBold);
        btnCancelar.setBorder(null);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());
        panelFondo.add(btnCancelar);

        MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent evt) { xMouse = evt.getX(); yMouse = evt.getY(); }
            public void mouseDragged(MouseEvent evt) { setLocation(evt.getXOnScreen() - xMouse, evt.getYOnScreen() - yMouse); }
        };
        panelFondo.addMouseListener(ma);
        panelFondo.addMouseMotionListener(ma);
    }

    private void crearEtiqueta(String texto, int x, int y, JPanel panel) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(new Color(200, 200, 220));
        lbl.setFont(fontPlain);
        lbl.setBounds(x, y, 200, 20);
        panel.add(lbl);
    }

    private JTextField crearInput(int x, int y, int w, JPanel panel) {
        JTextField t = new JTextField();
        t.setBounds(x, y, w, 35);
        t.setBackground(new Color(255, 255, 255, 20)); 
        t.setForeground(Color.WHITE);
        t.setCaretColor(Color.WHITE);
        t.setFont(fontPlain);
        t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 50)), 
            new EmptyBorder(5, 10, 5, 10)
        ));
        panel.add(t);
        return t;
    }

    private void cargarCiudades() {
        GestionCiudades gestion = new GestionCiudades();
        List<Ciudad> ciudades = gestion.obtenerCiudades();
        for (Ciudad c : ciudades) {
            cmbCiudad.addItem(c);
        }
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
