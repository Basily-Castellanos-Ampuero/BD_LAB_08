import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * R13001 - TIPO DE PROYECTO
 * Mantenimiento de tabla GZZ_TIP_PRO
 */
public class TipPro extends JFrame {

    // ============================================================
    //  CONFIGURACIÓN DE BASE DE DATOS
    //  Copia "db.properties.example" -> "db.properties" y llénalo.
    // ============================================================
    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASS;
    static {
        java.util.Properties p = new java.util.Properties();
        for (String ruta : new String[]{"db.properties", "../db.properties"}) {
            try (java.io.FileInputStream f = new java.io.FileInputStream(ruta)) {
                p.load(f); break;
            } catch (java.io.IOException ignored) {}
        }
        DB_URL  = p.getProperty("db.url",  "jdbc:mysql://localhost:3306/Proyectos?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        DB_USER = p.getProperty("db.user", "root");
        DB_PASS = p.getProperty("db.pass", "");
    }
    // ============================================================

    // Flag de actualización (0 = sin operación pendiente, 1 = operación pendiente)
    private int tipProFlaAct = 0;

    // Operación en curso: ADICIONAR | MODIFICAR | ELIMINAR | INACTIVAR | REACTIVAR
    private String operacionActual = "";

    // Componentes del formulario
    private JTextField txtTipProCod;
    private JTextField txtTipProDes;
    private JTextField txtTipProTam;
    private JTextField txtTipProEstReg;

    // Grilla
    private JTable grilla;
    private DefaultTableModel modeloGrilla;

    // Botones
    private JButton btnAdicionar, btnModificar, btnEliminar;
    private JButton btnInactivar, btnReactivar, btnActualizar;
    private JButton btnCancelar, btnSalir;

    private Connection conn;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public TipPro() {
        setTitle("R13001 - TIPO DE PROYECTO");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        inicializarComponentes();
        conectarBD();
        insertarDatosPrueba();
        cargarGrilla();
        estadoInicial();
    }

    // ----------------------------------------------------------------
    //  Construcción de la interfaz
    // ----------------------------------------------------------------
    private void inicializarComponentes() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(8, 8));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel de Registro ---
        JPanel panelRegistro = new JPanel(new GridBagLayout());
        panelRegistro.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Registro de Tipo de Proyecto"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 8, 5, 8);
        gbc.anchor  = GridBagConstraints.WEST;

        // Fila 0 – Código
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE;
        panelRegistro.add(new JLabel("Código (TipProCod):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtTipProCod = new JTextField(8);
        panelRegistro.add(txtTipProCod, gbc);

        // Fila 1 – Descripción
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        panelRegistro.add(new JLabel("Descripción (TipProDes):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.gridwidth = 3;
        txtTipProDes = new JTextField(32);
        panelRegistro.add(txtTipProDes, gbc);
        gbc.gridwidth = 1;

        // Fila 2 – Tamaño  /  Estado de Registro
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        panelRegistro.add(new JLabel("Tamaño (TipProTam):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtTipProTam = new JTextField(4);
        panelRegistro.add(txtTipProTam, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE;
        panelRegistro.add(new JLabel("Estado Reg. (TipProEstReg):"), gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtTipProEstReg = new JTextField(4);
        txtTipProEstReg.setEditable(false);
        txtTipProEstReg.setBackground(new Color(220, 220, 220));
        txtTipProEstReg.setFont(txtTipProEstReg.getFont().deriveFont(Font.BOLD));
        panelRegistro.add(txtTipProEstReg, gbc);

        // --- Panel de Botones ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        panelBotones.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Comandos"));

        btnAdicionar  = new JButton("Adicionar");
        btnModificar  = new JButton("Modificar");
        btnEliminar   = new JButton("Eliminar");
        btnInactivar  = new JButton("Inactivar");
        btnReactivar  = new JButton("Reactivar");
        btnActualizar = new JButton("Actualizar");
        btnCancelar   = new JButton("Cancelar");
        btnSalir      = new JButton("Salir");

        // Colores para distinguir grupos de botones
        btnActualizar.setBackground(new Color(144, 238, 144));
        btnCancelar.setBackground(new Color(255, 200, 130));
        btnSalir.setBackground(new Color(255, 140, 140));

        panelBotones.add(btnAdicionar);
        panelBotones.add(btnModificar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnInactivar);
        panelBotones.add(btnReactivar);
        panelBotones.add(new JSeparator(JSeparator.VERTICAL));
        panelBotones.add(btnActualizar);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnSalir);

        // --- Grilla ---
        String[] columnas = {"TipProCod", "TipProDes", "TipProTam", "TipProEstReg"};
        modeloGrilla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        grilla = new JTable(modeloGrilla);
        grilla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grilla.getTableHeader().setReorderingAllowed(false);
        grilla.setRowHeight(20);

        // Anchos de columna
        grilla.getColumnModel().getColumn(0).setPreferredWidth(90);
        grilla.getColumnModel().getColumn(1).setPreferredWidth(280);
        grilla.getColumnModel().getColumn(2).setPreferredWidth(70);
        grilla.getColumnModel().getColumn(3).setPreferredWidth(110);

        JScrollPane scrollGrilla = new JScrollPane(grilla);
        scrollGrilla.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Registros de Tipo de Proyecto"));
        scrollGrilla.setPreferredSize(new Dimension(740, 220));

        // --- Ensamblar ---
        JPanel panelSuperior = new JPanel(new BorderLayout(5, 5));
        panelSuperior.add(panelRegistro, BorderLayout.CENTER);
        panelSuperior.add(panelBotones,  BorderLayout.SOUTH);

        panelPrincipal.add(panelSuperior, BorderLayout.NORTH);
        panelPrincipal.add(scrollGrilla,  BorderLayout.CENTER);

        add(panelPrincipal);

        // --- Eventos de botones ---
        btnAdicionar .addActionListener(e -> accionAdicionar());
        btnModificar .addActionListener(e -> accionModificar());
        btnEliminar  .addActionListener(e -> accionEliminar());
        btnInactivar .addActionListener(e -> accionInactivar());
        btnReactivar .addActionListener(e -> accionReactivar());
        btnActualizar.addActionListener(e -> accionActualizar());
        btnCancelar  .addActionListener(e -> accionCancelar());
        btnSalir     .addActionListener(e -> accionSalir());
    }

    // ----------------------------------------------------------------
    //  Conexión a la base de datos
    // ----------------------------------------------------------------
    private void conectarBD() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al conectar a la base de datos:\n" + e.getMessage(),
                "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----------------------------------------------------------------
    //  Inserción automática de datos de prueba (solo si la tabla está vacía)
    // ----------------------------------------------------------------
    private void insertarDatosPrueba() {
        if (conn == null) return;
        try {
            // Prerequisito: poblar GZZ_EST_REG si está vacía
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                    "INSERT IGNORE INTO GZZ_EST_REG (EstRegCod, EstRegDes, EstRegEstReg) VALUES " +
                    "('A','Activo','A'), ('I','Inactivo','A'), ('*','Eliminado','A')");
            }

            // Insertar en GZZ_TIP_PRO solo si no hay registros aún
            try (Statement chk = conn.createStatement();
                 ResultSet rs  = chk.executeQuery("SELECT COUNT(*) FROM GZZ_TIP_PRO")) {
                rs.next();
                if (rs.getInt(1) > 0) return; // ya tiene datos, no tocar
            }

            String sql =
                "INSERT INTO GZZ_TIP_PRO (TipProCod, TipProDes, TipProTam, TipProEstReg) VALUES " +
                "(1,'Desarrollo de Software','G','A')," +
                "(2,'Consultoría Empresarial','M','A')," +
                "(3,'Implementación de Sistemas','G','A')," +
                "(4,'Mantenimiento de Aplicaciones','P','A')," +
                "(5,'Auditoría de Sistemas','M','A')," +
                "(6,'Migración de Datos','M','A')," +
                "(7,'Integración de Plataformas','G','A')," +
                "(8,'Soporte Técnico','P','A')," +
                "(9,'Análisis de Requerimientos','P','A')," +
                "(10,'Capacitación y Formación','P','A')";

            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al insertar datos de prueba:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----------------------------------------------------------------
    //  Carga / recarga de la grilla desde la BD
    // ----------------------------------------------------------------
    private void cargarGrilla() {
        modeloGrilla.setRowCount(0);
        if (conn == null) return;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT TipProCod, TipProDes, TipProTam, TipProEstReg " +
                 "FROM GZZ_TIP_PRO ORDER BY TipProCod")) {
            while (rs.next()) {
                modeloGrilla.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2),
                    rs.getString(3), rs.getString(4)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar la grilla:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----------------------------------------------------------------
    //  Gestión de estado de la interfaz
    // ----------------------------------------------------------------
    private void limpiarFormulario() {
        txtTipProCod.setText("");
        txtTipProDes.setText("");
        txtTipProTam.setText("");
        txtTipProEstReg.setText("");
        grilla.clearSelection();
    }

    private void estadoInicial() {
        tipProFlaAct   = 0;
        operacionActual = "";
        limpiarFormulario();

        // Todos los campos de entrada bloqueados en reposo
        txtTipProCod.setEditable(false);
        txtTipProDes.setEditable(false);
        txtTipProTam.setEditable(false);
        txtTipProCod.setBackground(new Color(220, 220, 220));
        txtTipProDes.setBackground(Color.WHITE);
        txtTipProTam.setBackground(new Color(220, 220, 220));

        // Todos los botones operativos habilitados
        btnAdicionar .setEnabled(true);
        btnModificar .setEnabled(true);
        btnEliminar  .setEnabled(true);
        btnInactivar .setEnabled(true);
        btnReactivar .setEnabled(true);
        btnActualizar.setEnabled(true);
        btnCancelar  .setEnabled(true);
        btnSalir     .setEnabled(true);
    }

    private void estadoOperacion() {
        // Durante una operación, sólo Actualizar, Cancelar y Salir quedan activos
        btnAdicionar .setEnabled(false);
        btnModificar .setEnabled(false);
        btnEliminar  .setEnabled(false);
        btnInactivar .setEnabled(false);
        btnReactivar .setEnabled(false);
    }

    private void cargarFilaEnFormulario(int fila) {
        txtTipProCod.setText(modeloGrilla.getValueAt(fila, 0).toString());
        txtTipProDes.setText(modeloGrilla.getValueAt(fila, 1).toString());
        txtTipProTam.setText(modeloGrilla.getValueAt(fila, 2).toString());
        txtTipProEstReg.setText(modeloGrilla.getValueAt(fila, 3).toString());
    }

    // ----------------------------------------------------------------
    //  Acciones de los botones
    // ----------------------------------------------------------------

    /** ADICIONAR: habilita código, descripción y tamaño; EstReg = A (bloqueado) */
    private void accionAdicionar() {
        limpiarFormulario();
        operacionActual = "ADICIONAR";
        tipProFlaAct    = 1;

        txtTipProCod.setEditable(true);
        txtTipProDes.setEditable(true);
        txtTipProTam.setEditable(true);
        txtTipProCod.setBackground(Color.WHITE);
        txtTipProTam.setBackground(Color.WHITE);
        txtTipProEstReg.setText("A");

        estadoOperacion();
        txtTipProCod.requestFocus();
    }

    /** MODIFICAR: carga el registro seleccionado; sólo descripción modificable */
    private void accionModificar() {
        int fila = grilla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un registro de la grilla para modificar.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargarFilaEnFormulario(fila);
        operacionActual = "MODIFICAR";
        tipProFlaAct    = 1;

        txtTipProCod.setEditable(false);
        txtTipProDes.setEditable(true);
        txtTipProTam.setEditable(false);
        txtTipProCod.setBackground(new Color(220, 220, 220));
        txtTipProTam.setBackground(new Color(220, 220, 220));

        estadoOperacion();
        txtTipProDes.requestFocus();
    }

    /** ELIMINAR: carga el registro; pone EstReg = * (ningún campo editable) */
    private void accionEliminar() {
        int fila = grilla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un registro de la grilla para eliminar.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargarFilaEnFormulario(fila);
        txtTipProEstReg.setText("*");
        operacionActual = "ELIMINAR";
        tipProFlaAct    = 1;

        txtTipProCod.setEditable(false);
        txtTipProDes.setEditable(false);
        txtTipProTam.setEditable(false);
        txtTipProCod.setBackground(new Color(220, 220, 220));
        txtTipProTam.setBackground(new Color(220, 220, 220));

        estadoOperacion();
    }

    /** INACTIVAR: carga el registro; pone EstReg = I (ningún campo editable) */
    private void accionInactivar() {
        int fila = grilla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un registro de la grilla para inactivar.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargarFilaEnFormulario(fila);
        txtTipProEstReg.setText("I");
        operacionActual = "INACTIVAR";
        tipProFlaAct    = 1;

        txtTipProCod.setEditable(false);
        txtTipProDes.setEditable(false);
        txtTipProTam.setEditable(false);
        txtTipProCod.setBackground(new Color(220, 220, 220));
        txtTipProTam.setBackground(new Color(220, 220, 220));

        estadoOperacion();
    }

    /** REACTIVAR: carga el registro; pone EstReg = A (ningún campo editable) */
    private void accionReactivar() {
        int fila = grilla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un registro de la grilla para reactivar.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargarFilaEnFormulario(fila);
        txtTipProEstReg.setText("A");
        operacionActual = "REACTIVAR";
        tipProFlaAct    = 1;

        txtTipProCod.setEditable(false);
        txtTipProDes.setEditable(false);
        txtTipProTam.setEditable(false);
        txtTipProCod.setBackground(new Color(220, 220, 220));
        txtTipProTam.setBackground(new Color(220, 220, 220));

        estadoOperacion();
    }

    /**
     * ACTUALIZAR: verifica el flag TipProFlaAct; si es 1 graba en BD,
     * recarga la grilla y vuelve al estado inicial.
     */
    private void accionActualizar() {
        if (tipProFlaAct != 1) {
            // Mensaje con sólo botón "Cancelar" (según especificación)
            JOptionPane.showOptionDialog(this,
                "No se ha seleccionado un comando para actualizar un registro de la BD",
                "Aviso",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{"Cancelar"},
                "Cancelar");
            return;
        }

        String codStr  = txtTipProCod.getText().trim();
        String des     = txtTipProDes.getText().trim();
        String tam     = txtTipProTam.getText().trim();
        String estReg  = txtTipProEstReg.getText().trim();

        // Validaciones básicas
        if (codStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El código no puede estar vacío.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (des.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La descripción no puede estar vacía.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (tam.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El tamaño no puede estar vacío.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cod;
        try {
            cod = Integer.parseInt(codStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El código debe ser un número entero.",
                "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if ("ADICIONAR".equals(operacionActual)) {
                // Verificar que el código no exista ya
                try (PreparedStatement chk = conn.prepareStatement(
                        "SELECT COUNT(*) FROM GZZ_TIP_PRO WHERE TipProCod = ?")) {
                    chk.setInt(1, cod);
                    ResultSet rs = chk.executeQuery();
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this,
                            "Ya existe un registro con el código " + cod + ".",
                            "Código duplicado", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO GZZ_TIP_PRO (TipProCod, TipProDes, TipProTam, TipProEstReg) " +
                        "VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, cod);
                    ps.setString(2, des);
                    ps.setString(3, tam.substring(0, 1)); // CHAR(1)
                    ps.setString(4, estReg);
                    ps.executeUpdate();
                }
            } else {
                // MODIFICAR / ELIMINAR / INACTIVAR / REACTIVAR → UPDATE
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE GZZ_TIP_PRO SET TipProDes = ?, TipProTam = ?, TipProEstReg = ? " +
                        "WHERE TipProCod = ?")) {
                    ps.setString(1, des);
                    ps.setString(2, tam.substring(0, 1));
                    ps.setString(3, estReg);
                    ps.setInt(4, cod);
                    ps.executeUpdate();
                }
            }

            cargarGrilla();
            estadoInicial();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al guardar en la base de datos:\n" + e.getMessage(),
                "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** CANCELAR: borra el formulario, desactiva la operación en curso y vuelve al estado inicial */
    private void accionCancelar() {
        estadoInicial();
    }

    /** SALIR: cierra la conexión y termina la aplicación */
    private void accionSalir() {
        accionCancelar();
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignored) {}
        dispose();
        System.exit(0);
    }

    // ----------------------------------------------------------------
    //  Punto de entrada
    // ----------------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new TipPro().setVisible(true);
        });
    }
}
