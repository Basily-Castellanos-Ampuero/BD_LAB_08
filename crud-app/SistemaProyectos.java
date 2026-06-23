import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * Sistema de Gestión de Proyectos – BD26A / UNSA
 * Arquitectura: Panel Maestro-Detalle con jerarquía de tablas
 *   Referenciales → Maestras → Transacciones
 */
public class SistemaProyectos extends JFrame {

    // ─── Conexión ───────────────────────────────────────────────────
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
        DB_URL  = p.getProperty("db.url",  "jdbc:mysql://localhost:3306/control_proyectos?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        DB_USER = p.getProperty("db.user", "root");
        DB_PASS = p.getProperty("db.pass", "");
    }

    // ─── Paleta de colores ───────────────────────────────────────────
    private static final Color C_SIDEBAR_BG  = new Color(248, 249, 250);
    private static final Color C_TOPBAR_BG   = new Color(255, 255, 255);
    private static final Color C_HEADER_BG   = new Color(245, 247, 249);
    private static final Color C_SEL_BG      = new Color(230, 241, 251);
    private static final Color C_SEL_FG      = new Color(24,  95, 165);
    private static final Color C_ACCENT      = new Color(24,  95, 165);
    private static final Color C_BORDER      = new Color(220, 220, 220);
    private static final Color C_TEXT_SEC    = new Color(100, 110, 120);
    private static final Color C_NAV_ACTIVE  = new Color(235, 245, 255);
    private static final Color C_BTN_NEW     = new Color(24,  95, 165);
    private static final Color C_BTN_EDIT    = new Color(255, 255, 255);
    private static final Color C_BTN_DEL     = new Color(255, 255, 255);
    private static final Color C_STATUS_BG   = new Color(245, 247, 249);

    // ─── Modelo de tabla en el catálogo ─────────────────────────────
    private static class TablaConfig {
        String id, label, nombreFisico, categoria;
        String relacionadaCon; // id de tabla padre (para detalle)
        String fkColumna;      // columna FK que referencia al padre
        TablaConfig(String id, String label, String nombreFisico, String categoria,
                    String relacionadaCon, String fkColumna) {
            this.id             = id;
            this.label          = label;
            this.nombreFisico   = nombreFisico;
            this.categoria      = categoria;
            this.relacionadaCon = relacionadaCon;
            this.fkColumna      = fkColumna;
        }
    }

    // Catálogo de tablas del sistema (adaptar a BD real)
    private static final TablaConfig[] CATALOGO = {
        // Referenciales
        new TablaConfig("tip_cli",  "Tipos Cliente",   "GZZ_TIP_CLI",    "Referencial", null,      null),
        new TablaConfig("tip_pro",  "Tipos Proyecto",  "GZZ_TIP_PRO",    "Referencial", null,      null),
        new TablaConfig("cargos",   "Cargos",          "GZZ_CARGOS",     "Referencial", null,      null),
        new TablaConfig("estados",  "Estados",         "GZZ_ESTADOS",    "Referencial", null,      null),
        // Maestras
        new TablaConfig("clientes", "Clientes",        "G1M_CLIENTES",   "Maestra",     null,      null),
        new TablaConfig("cli_cred", "Clientes Crédito","G1M_CLI_CREDITO","Maestra",     "clientes","COD_CLI"),
        new TablaConfig("empleados","Empleados",       "G1M_EMPLEADOS",  "Maestra",     null,      null),
        // Transacciones
        new TablaConfig("proyectos","Proyectos",       "G2T_PROYECTOS",  "Transacción", "clientes","COD_CLI"),
        new TablaConfig("tareas",   "Tareas",          "G3T_TAREAS",     "Transacción", "proyectos","COD_PRO"),
        new TablaConfig("asignac",  "Asignaciones",    "G3T_ASIGNACION", "Transacción", "proyectos","COD_PRO"),
    };

    // ─── Estado de la aplicación ─────────────────────────────────────
    private Connection conn;
    private TablaConfig tablaActiva;
    private int filaSelMaestro  = -1;
    private int filaSelDetalle  = -1;

    // Metadata
    private List<String>  colsMaestro  = new ArrayList<>();
    private List<String>  pksMaestro   = new ArrayList<>();
    private List<String>  colsDetalle  = new ArrayList<>();
    private List<String>  pksDetalle   = new ArrayList<>();
    private List<int[]>   tiposMaestro = new ArrayList<>(); // [tipoSql, nullable, autoInc]
    private List<int[]>   tiposDetalle = new ArrayList<>();

    // ─── Componentes UI ──────────────────────────────────────────────
    private JPanel          panelNav;
    private JLabel          lblTitulo;
    private JTextField      txtBuscar;
    private JTable          tablaMaestro, tablaDetalle;
    private DefaultTableModel modeloMaestro, modeloDetalle;
    private JLabel          lblEstadoMaestro, lblEstadoDetalle;
    private JLabel          lblStatusBar;
    private JButton         btnNuevo, btnEditar, btnEliminar, btnRefresh;
    private Map<String, JButton> navButtons = new LinkedHashMap<>();

    // ─── Constructor ─────────────────────────────────────────────────
    public SistemaProyectos() {
        setTitle("Sistema de Gestión – Control de Proyectos  |  BD26A · UNSA");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 680);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        buildUI();
        conectar();

        if (CATALOGO.length > 0)
            activarTabla(CATALOGO[4]); // Clientes por defecto
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE UI
    // ═══════════════════════════════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildSidebar(),  BorderLayout.WEST);
        add(buildCentro(),   BorderLayout.CENTER);
        add(buildStatusBar(),BorderLayout.SOUTH);
    }

    // ── Sidebar de navegación ──────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(195, 0));
        sidebar.setBackground(C_SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));

        // Cabecera del sidebar
        JLabel logo = new JLabel("  Control Proyectos");
        logo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        logo.setForeground(C_ACCENT);
        logo.setBorder(new EmptyBorder(14, 8, 14, 8));
        logo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,C_BORDER),
            new EmptyBorder(12,10,12,8)));
        sidebar.add(logo, BorderLayout.NORTH);

        panelNav = new JPanel();
        panelNav.setLayout(new BoxLayout(panelNav, BoxLayout.Y_AXIS));
        panelNav.setBackground(C_SIDEBAR_BG);

        // Agrupar por categoría
        String[] cats = {"Referencial","Maestra","Transacción"};
        for (String cat : cats) {
            JLabel lcat = new JLabel("  " + cat.toUpperCase() + "S");
            lcat.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            lcat.setForeground(C_TEXT_SEC);
            lcat.setBorder(new EmptyBorder(10, 6, 3, 6));
            lcat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            panelNav.add(lcat);

            for (TablaConfig tc : CATALOGO) {
                if (!tc.categoria.equals(cat)) continue;
                JButton btn = makeNavBtn(tc);
                navButtons.put(tc.id, btn);
                panelNav.add(btn);
            }
        }
        panelNav.add(Box.createVerticalGlue());

        JScrollPane sp = new JScrollPane(panelNav);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(sp, BorderLayout.CENTER);
        return sidebar;
    }

    private JButton makeNavBtn(TablaConfig tc) {
        JButton btn = new JButton("  " + tc.label);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btn.setForeground(C_TEXT_SEC);
        btn.setBackground(C_SIDEBAR_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btn.setPreferredSize(new Dimension(195, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (tablaActiva == null || !tablaActiva.id.equals(tc.id))
                    btn.setBackground(new Color(238, 243, 248));
            }
            public void mouseExited(MouseEvent e) {
                if (tablaActiva == null || !tablaActiva.id.equals(tc.id))
                    btn.setBackground(C_SIDEBAR_BG);
            }
        });
        btn.addActionListener(e -> activarTabla(tc));
        return btn;
    }

    // ── Panel central ──────────────────────────────────────────────
    private JPanel buildCentro() {
        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Color.WHITE);

        // Topbar
        JPanel topbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topbar.setBackground(C_TOPBAR_BG);
        topbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        lblTitulo = new JLabel("Clientes");
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        topbar.add(lblTitulo);
        topbar.add(Box.createHorizontalStrut(12));

        // Búsqueda
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        searchBox.setBackground(C_HEADER_BG);
        searchBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER, 1, true),
            new EmptyBorder(2, 6, 2, 6)));
        JLabel icoSearch = new JLabel("🔍");
        icoSearch.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        txtBuscar = new JTextField(18);
        txtBuscar.setBorder(BorderFactory.createEmptyBorder());
        txtBuscar.setBackground(C_HEADER_BG);
        txtBuscar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrarMaestro(); }
            public void removeUpdate(DocumentEvent e) { filtrarMaestro(); }
            public void changedUpdate(DocumentEvent e) {}
        });
        searchBox.add(icoSearch);
        searchBox.add(txtBuscar);
        topbar.add(searchBox);

        // Botones acción
        btnNuevo   = makeBtn("+ Nuevo",   C_BTN_NEW,  Color.WHITE);
        btnEditar  = makeBtn("✎ Editar",  C_BTN_EDIT, C_ACCENT);
        btnEliminar= makeBtn("✕ Eliminar",C_BTN_DEL,  new Color(163, 45, 45));
        btnRefresh = makeBtn("↻",         C_BTN_EDIT, C_TEXT_SEC);

        btnNuevo.addActionListener(e -> nuevoEnMaestro());
        btnEditar.addActionListener(e -> editarEnMaestro());
        btnEliminar.addActionListener(e -> eliminarEnMaestro());
        btnRefresh.addActionListener(e -> { if (tablaActiva != null) cargarMaestro(txtBuscar.getText()); });

        topbar.add(btnNuevo);
        topbar.add(btnEditar);
        topbar.add(btnEliminar);
        topbar.add(btnRefresh);

        centro.add(topbar, BorderLayout.NORTH);

        // Split Maestro / Detalle
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.55);
        split.setDividerSize(6);
        split.setBackground(C_BORDER);

        split.setTopComponent(buildPanelMaestro());
        split.setBottomComponent(buildPanelDetalle());

        centro.add(split, BorderLayout.CENTER);
        return centro;
    }

    private JButton makeBtn(String txt, Color bg, Color fg) {
        JButton btn = new JButton(txt);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 1, true),
            new EmptyBorder(4, 10, 4, 10)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Panel maestro ──────────────────────────────────────────────
    private JPanel buildPanelMaestro() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        lblEstadoMaestro = new JLabel(" ");
        lblEstadoMaestro.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        lblEstadoMaestro.setForeground(C_TEXT_SEC);
        lblEstadoMaestro.setBorder(new EmptyBorder(4, 10, 4, 10));
        lblEstadoMaestro.setBackground(C_HEADER_BG);
        lblEstadoMaestro.setOpaque(true);
        lblEstadoMaestro.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
            new EmptyBorder(5, 10, 5, 10)));

        modeloMaestro = new DefaultTableModel() {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaMaestro = buildTabla(modeloMaestro);
        tablaMaestro.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                filaSelMaestro = tablaMaestro.getSelectedRow();
                onSeleccionMaestro();
            }
        });

        p.add(lblEstadoMaestro, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaMaestro), BorderLayout.CENTER);
        return p;
    }

    // ── Panel detalle ──────────────────────────────────────────────
    private JPanel buildPanelDetalle() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        // Cabecera del panel detalle con botones propios
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        hdr.setBackground(new Color(248, 251, 255));
        hdr.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, C_BORDER));

        lblEstadoDetalle = new JLabel("↳ Seleccione un registro arriba");
        lblEstadoDetalle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        lblEstadoDetalle.setForeground(C_ACCENT);
        hdr.add(lblEstadoDetalle);

        JButton btnNuevoDet = makeBtn("+ Nuevo detalle", new Color(200, 225, 255), C_ACCENT);
        JButton btnEditDet  = makeBtn("✎ Editar",        C_BTN_EDIT, C_TEXT_SEC);
        JButton btnDelDet   = makeBtn("✕",               C_BTN_DEL,  new Color(163,45,45));
        btnNuevoDet.addActionListener(e -> nuevoEnDetalle());
        btnEditDet.addActionListener(e  -> editarEnDetalle());
        btnDelDet.addActionListener(e   -> eliminarEnDetalle());
        hdr.add(btnNuevoDet);
        hdr.add(btnEditDet);
        hdr.add(btnDelDet);

        modeloDetalle = new DefaultTableModel() {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaDetalle = buildTabla(modeloDetalle);
        tablaDetalle.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                filaSelDetalle = tablaDetalle.getSelectedRow();
        });

        p.add(hdr, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaDetalle), BorderLayout.CENTER);
        return p;
    }

    private JTable buildTabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo) {
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(C_SEL_BG);
                    c.setForeground(C_SEL_FG);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(252, 253, 254));
                    c.setForeground(Color.DARK_GRAY);
                }
                return c;
            }
        };
        t.setRowHeight(26);
        t.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setGridColor(C_BORDER);

        JTableHeader th = t.getTableHeader();
        th.setBackground(C_HEADER_BG);
        th.setForeground(C_TEXT_SEC);
        th.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        th.setReorderingAllowed(false);
        return t;
    }

    // ── Status bar ────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel sb = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        sb.setBackground(C_STATUS_BG);
        sb.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
        lblStatusBar = new JLabel("  BD: control_proyectos");
        lblStatusBar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        lblStatusBar.setForeground(C_TEXT_SEC);
        sb.add(lblStatusBar);
        return sb;
    }

    // ═══════════════════════════════════════════════════════════════
    //  LÓGICA DE DATOS
    // ═══════════════════════════════════════════════════════════════
    private void conectar() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            lblStatusBar.setText("  BD: " + conn.getCatalog() + "  |  Usuario: " + DB_USER + "  |  Conectado ✓");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "No se pudo conectar a la base de datos:\n" + e.getMessage(),
                "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void activarTabla(TablaConfig tc) {
        tablaActiva = tc;
        filaSelMaestro = -1;
        filaSelDetalle = -1;
        txtBuscar.setText("");
        lblTitulo.setText(tc.label + "  ·  " + tc.nombreFisico);

        // Resaltar botón activo en sidebar
        navButtons.forEach((id, btn) -> {
            boolean activo = id.equals(tc.id);
            btn.setBackground(activo ? C_NAV_ACTIVE : C_SIDEBAR_BG);
            btn.setForeground(activo ? C_ACCENT : C_TEXT_SEC);
            btn.setFont(new Font(Font.SANS_SERIF, activo ? Font.BOLD : Font.PLAIN, 12));
        });

        cargarMetadata(tc.nombreFisico, colsMaestro, pksMaestro, tiposMaestro);
        cargarMaestro("");
        limpiarDetalle();
    }

    private void cargarMetadata(String tabla, List<String> cols, List<String> pks, List<int[]> tipos) {
        cols.clear(); pks.clear(); tipos.clear();
        if (conn == null) return;
        try {
            DatabaseMetaData md = conn.getMetaData();
            String cat = conn.getCatalog();
            try (ResultSet rs = md.getColumns(cat, null, tabla, "%")) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME"));
                    tipos.add(new int[]{
                        rs.getInt("DATA_TYPE"),
                        "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")) ? 1 : 0,
                        "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")) ? 1 : 0
                    });
                }
            }
            try (ResultSet rs = md.getPrimaryKeys(cat, null, tabla)) {
                while (rs.next()) pks.add(rs.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            setStatus("Error metadata: " + e.getMessage());
        }
    }

    private void cargarMaestro(String filtro) {
        if (conn == null || tablaActiva == null) return;
        String tabla = tablaActiva.nombreFisico;
        modeloMaestro.setColumnIdentifiers(colsMaestro.toArray());
        modeloMaestro.setRowCount(0);
        String sql = "SELECT * FROM `" + tabla + "`";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            String f = filtro.trim().toLowerCase();
            while (rs.next()) {
                Object[] fila = new Object[colsMaestro.size()];
                for (int i = 0; i < colsMaestro.size(); i++)
                    fila[i] = rs.getObject(i + 1);
                if (f.isEmpty() || Arrays.stream(fila).anyMatch(v -> v != null && v.toString().toLowerCase().contains(f)))
                    modeloMaestro.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar " + tabla + ":\n" + e.getMessage(),
                "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
        ajustarAnchos(tablaMaestro);
        lblEstadoMaestro.setText(tabla + "  |  " + modeloMaestro.getRowCount() + " registros  |  PK: " + pksMaestro);
        setStatus("Tabla: " + tabla + "  |  " + modeloMaestro.getRowCount() + " registros cargados");
    }

    private void onSeleccionMaestro() {
        if (filaSelMaestro < 0) { limpiarDetalle(); return; }
        // ¿Existe tabla hija relacionada a la activa?
        TablaConfig hija = findHija(tablaActiva.id);
        if (hija == null) { limpiarDetalle(); return; }

        // Obtener valor de PK del maestro seleccionado
        if (pksMaestro.isEmpty()) return;
        int pkIdx = colsMaestro.indexOf(pksMaestro.get(0));
        if (pkIdx < 0) return;
        Object pkVal = tablaMaestro.getValueAt(filaSelMaestro, pkIdx);

        cargarDetalle(hija, hija.fkColumna, pkVal);
    }

    private TablaConfig findHija(String parentId) {
        for (TablaConfig tc : CATALOGO)
            if (parentId.equals(tc.relacionadaCon)) return tc;
        return null;
    }

    private void cargarDetalle(TablaConfig tc, String fkCol, Object fkVal) {
        cargarMetadata(tc.nombreFisico, colsDetalle, pksDetalle, tiposDetalle);
        modeloDetalle.setColumnIdentifiers(colsDetalle.toArray());
        modeloDetalle.setRowCount(0);
        String sql = "SELECT * FROM `" + tc.nombreFisico + "` WHERE `" + fkCol + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, fkVal);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Object[] fila = new Object[colsDetalle.size()];
                for (int i = 0; i < colsDetalle.size(); i++) fila[i] = rs.getObject(i + 1);
                modeloDetalle.addRow(fila);
            }
        } catch (SQLException e) {
            setStatus("Error detalle: " + e.getMessage());
        }
        ajustarAnchos(tablaDetalle);
        lblEstadoDetalle.setText("↳ " + tc.label + " de [" + fkVal + "]  —  " + modeloDetalle.getRowCount() + " registro(s)");
    }

    private void limpiarDetalle() {
        modeloDetalle.setRowCount(0);
        modeloDetalle.setColumnCount(0);
        lblEstadoDetalle.setText("↳ Seleccione un registro arriba para ver el detalle relacionado");
    }

    private void filtrarMaestro() {
        filaSelMaestro = -1;
        limpiarDetalle();
        cargarMaestro(txtBuscar.getText());
    }

    private void ajustarAnchos(JTable t) {
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setPreferredWidth(130);
    }

    private void setStatus(String msg) {
        lblStatusBar.setText("  " + msg);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CRUD MAESTRO
    // ═══════════════════════════════════════════════════════════════
    private void nuevoEnMaestro() {
        if (tablaActiva == null) return;
        String[] vals = pedirDatos("Nuevo registro — " + tablaActiva.label, colsMaestro, tiposMaestro, pksMaestro, null);
        if (vals == null) return;
        ejecutarInsert(tablaActiva.nombreFisico, colsMaestro, tiposMaestro, vals);
        cargarMaestro(txtBuscar.getText());
    }

    private void editarEnMaestro() {
        if (tablaActiva == null || filaSelMaestro < 0) { avisar("Seleccione un registro en la tabla superior."); return; }
        String[] actuales = getFilaActual(tablaMaestro, filaSelMaestro, colsMaestro.size());
        String[] nuevos = pedirDatos("Editar — " + tablaActiva.label, colsMaestro, tiposMaestro, pksMaestro, actuales);
        if (nuevos == null) return;
        ejecutarUpdate(tablaActiva.nombreFisico, colsMaestro, tiposMaestro, pksMaestro, nuevos, actuales);
        cargarMaestro(txtBuscar.getText());
    }

    private void eliminarEnMaestro() {
        if (tablaActiva == null || filaSelMaestro < 0) { avisar("Seleccione un registro en la tabla superior."); return; }
        String pk0 = tablaMaestro.getValueAt(filaSelMaestro, 0) + "";
        int r = JOptionPane.showConfirmDialog(this,
            "¿Eliminar el registro [" + pk0 + "] de " + tablaActiva.nombreFisico + "?\n" +
            "Verifique que no tenga registros dependientes.",
            "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        String[] actuales = getFilaActual(tablaMaestro, filaSelMaestro, colsMaestro.size());
        ejecutarDelete(tablaActiva.nombreFisico, pksMaestro, colsMaestro, tiposMaestro, actuales);
        filaSelMaestro = -1;
        cargarMaestro(txtBuscar.getText());
        limpiarDetalle();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CRUD DETALLE
    // ═══════════════════════════════════════════════════════════════
    private TablaConfig getTablaHijaActiva() {
        return tablaActiva != null ? findHija(tablaActiva.id) : null;
    }

    private void nuevoEnDetalle() {
        TablaConfig hija = getTablaHijaActiva();
        if (hija == null) { avisar("Esta tabla no tiene una sub-tabla relacionada configurada."); return; }
        if (filaSelMaestro < 0) { avisar("Seleccione primero un registro padre en la tabla superior."); return; }
        String[] vals = pedirDatos("Nuevo detalle — " + hija.label, colsDetalle, tiposDetalle, pksDetalle, null);
        if (vals == null) return;
        ejecutarInsert(hija.nombreFisico, colsDetalle, tiposDetalle, vals);
        // Recargar detalle
        int pkIdx = colsMaestro.indexOf(pksMaestro.get(0));
        Object pkVal = tablaMaestro.getValueAt(filaSelMaestro, pkIdx);
        cargarDetalle(hija, hija.fkColumna, pkVal);
    }

    private void editarEnDetalle() {
        TablaConfig hija = getTablaHijaActiva();
        if (hija == null || filaSelDetalle < 0) { avisar("Seleccione un registro en el detalle."); return; }
        String[] actuales = getFilaActual(tablaDetalle, filaSelDetalle, colsDetalle.size());
        String[] nuevos = pedirDatos("Editar detalle — " + hija.label, colsDetalle, tiposDetalle, pksDetalle, actuales);
        if (nuevos == null) return;
        ejecutarUpdate(hija.nombreFisico, colsDetalle, tiposDetalle, pksDetalle, nuevos, actuales);
        int pkIdx = colsMaestro.indexOf(pksMaestro.get(0));
        Object pkVal = tablaMaestro.getValueAt(filaSelMaestro, pkIdx);
        cargarDetalle(hija, hija.fkColumna, pkVal);
    }

    private void eliminarEnDetalle() {
        TablaConfig hija = getTablaHijaActiva();
        if (hija == null || filaSelDetalle < 0) { avisar("Seleccione un registro en el detalle."); return; }
        String pk0 = tablaDetalle.getValueAt(filaSelDetalle, 0) + "";
        int r = JOptionPane.showConfirmDialog(this,
            "¿Eliminar [" + pk0 + "] de " + hija.nombreFisico + "?",
            "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        String[] actuales = getFilaActual(tablaDetalle, filaSelDetalle, colsDetalle.size());
        ejecutarDelete(hija.nombreFisico, pksDetalle, colsDetalle, tiposDetalle, actuales);
        filaSelDetalle = -1;
        int pkIdx = colsMaestro.indexOf(pksMaestro.get(0));
        Object pkVal = tablaMaestro.getValueAt(filaSelMaestro, pkIdx);
        cargarDetalle(hija, hija.fkColumna, pkVal);
    }

    // ═══════════════════════════════════════════════════════════════
    //  SQL GENÉRICO
    // ═══════════════════════════════════════════════════════════════
    private void ejecutarInsert(String tabla, List<String> cols, List<int[]> tipos, String[] vals) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < tipos.size(); i++) if (tipos.get(i)[2] == 0) idx.add(i); // no auto-inc
        String campos = String.join(", ", idx.stream().map(i -> "`" + cols.get(i) + "`").toArray(String[]::new));
        String marks  = String.join(", ", Collections.nCopies(idx.size(), "?"));
        String sql = "INSERT INTO `" + tabla + "` (" + campos + ") VALUES (" + marks + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int k = 0; k < idx.size(); k++) setParam(ps, k+1, vals[idx.get(k)], cols.get(idx.get(k)), tipos.get(idx.get(k)));
            ps.executeUpdate();
            setStatus("Registro insertado correctamente en " + tabla);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al insertar:\n" + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ejecutarUpdate(String tabla, List<String> cols, List<int[]> tipos,
                                 List<String> pks, String[] nuevos, String[] actuales) {
        List<Integer> setCols = new ArrayList<>(), setIdx = new ArrayList<>();
        for (int i = 0; i < cols.size(); i++) {
            if (!pks.contains(cols.get(i))) { setCols.add(i); setIdx.add(i); }
        }
        String setClause   = String.join(", ", setCols.stream().map(i -> "`" + cols.get(i) + "` = ?").toArray(String[]::new));
        String whereClause = String.join(" AND ", pks.stream().map(c -> "`" + c + "` = ?").toArray(String[]::new));
        String sql = "UPDATE `" + tabla + "` SET " + setClause + " WHERE " + whereClause;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (int k : setCols) setParam(ps, idx++, nuevos[k], cols.get(k), tipos.get(k));
            for (String pk : pks) {
                int ci = cols.indexOf(pk);
                setParam(ps, idx++, actuales[ci], pk, tipos.get(ci));
            }
            ps.executeUpdate();
            setStatus("Registro actualizado correctamente en " + tabla);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al actualizar:\n" + e.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ejecutarDelete(String tabla, List<String> pks, List<String> cols, List<int[]> tipos, String[] actuales) {
        String where = String.join(" AND ", pks.stream().map(c -> "`" + c + "` = ?").toArray(String[]::new));
        String sql = "DELETE FROM `" + tabla + "` WHERE " + where;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String pk : pks) {
                int ci = cols.indexOf(pk);
                setParam(ps, idx++, actuales[ci], pk, tipos.get(ci));
            }
            ps.executeUpdate();
            setStatus("Registro eliminado de " + tabla);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar:\n" + e.getMessage() +
                "\n\n(Posible integridad referencial: existen registros dependientes.)", "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setParam(PreparedStatement ps, int idx, String valor, String col, int[] tipo) throws SQLException {
        int tipoSql = tipo[0]; boolean nullable = tipo[1]==1;
        if (valor == null || valor.isEmpty()) {
            if (nullable) { ps.setNull(idx, tipoSql); return; }
        }
        try {
            switch (tipoSql) {
                case Types.TINYINT: case Types.SMALLINT: case Types.INTEGER:
                    ps.setInt(idx, Integer.parseInt(valor)); break;
                case Types.BIGINT:
                    ps.setLong(idx, Long.parseLong(valor)); break;
                case Types.DECIMAL: case Types.NUMERIC:
                    ps.setBigDecimal(idx, new BigDecimal(valor)); break;
                case Types.DOUBLE: case Types.FLOAT: case Types.REAL:
                    ps.setDouble(idx, Double.parseDouble(valor)); break;
                case Types.DATE:
                    ps.setDate(idx, Date.valueOf(valor)); break;
                case Types.TIMESTAMP:
                    ps.setTimestamp(idx, Timestamp.valueOf(valor)); break;
                default:
                    ps.setString(idx, valor);
            }
        } catch (IllegalArgumentException ex) {
            throw new SQLException("Valor inválido en '" + col + "': \"" + valor + "\"");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIÁLOGO DE DATOS
    // ═══════════════════════════════════════════════════════════════
    private String[] pedirDatos(String titulo, List<String> cols, List<int[]> tipos, List<String> pks, String[] iniciales) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField[] fields = new JTextField[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            String col = cols.get(i);
            int[] t = tipos.get(i);
            boolean esPK = pks.contains(col);
            boolean esAI = t[2] == 1;

            gbc.gridx = 0; gbc.gridy = i; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            String etq = col + (esPK ? " (PK)" : "") + (t[1]==0 ? " *" : "") + (esAI ? " [auto]" : "");
            JLabel lbl = new JLabel(etq);
            lbl.setFont(new Font(Font.SANS_SERIF, t[0]==Types.DATE?Font.ITALIC:Font.PLAIN, 12));
            lbl.setForeground(esPK ? C_ACCENT : Color.DARK_GRAY);
            panel.add(lbl, gbc);

            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
            JTextField tf = new JTextField(22);
            tf.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            if (iniciales != null) tf.setText(iniciales[i]);
            if (esAI || (esPK && iniciales != null)) { tf.setEditable(false); tf.setBackground(new Color(240,240,240)); }
            if (t[0] == Types.DATE) tf.setToolTipText("Formato: yyyy-MM-dd  (ej: 2024-03-15)");
            fields[i] = tf;
            panel.add(tf, gbc);
        }

        JScrollPane sp = new JScrollPane(panel);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setPreferredSize(new Dimension(440, Math.min(460, 60 + cols.size() * 38)));

        int op = JOptionPane.showConfirmDialog(this, sp, titulo,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return null;

        String[] res = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) res[i] = fields[i].getText().trim();
        return res;
    }

    private String[] getFilaActual(JTable t, int fila, int ncols) {
        String[] vals = new String[ncols];
        for (int i = 0; i < ncols; i++) {
            Object v = t.getValueAt(fila, i);
            vals[i] = v == null ? "" : v.toString();
        }
        return vals;
    }

    private void avisar(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════
    //  MAIN
    // ═══════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new SistemaProyectos().setVisible(true);
        });
    }
}
