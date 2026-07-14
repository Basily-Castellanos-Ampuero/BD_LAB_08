import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CrudApp extends JFrame {
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

    private Connection conn;
    private JComboBox<String> comboTablas;
    private JTable grilla;
    private DefaultTableModel modeloGrilla;
    private JLabel lblEstado;
    private String tablaActual;
    private List<Columna> columnas = new ArrayList<>();
    private List<String> clavesPrimarias = new ArrayList<>();

    private static class Columna {
        String nombre;
        int    tipoSql;       
        boolean nullable;
        boolean autoIncrement;
        String tipoNombre;    
        int    tamano;
    }

    public CrudApp() {
        setTitle("CRUD - Base de Datos Proyectos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        construirUI();
        conectarBD();
        cargarListaTablas();
    }

    private void construirUI() {
        JPanel principal = new JPanel(new BorderLayout(8, 8));
        principal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel barraSup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barraSup.add(new JLabel("Tabla:"));
        comboTablas = new JComboBox<>();
        comboTablas.setPreferredSize(new Dimension(260, 26));
        barraSup.add(comboTablas);

        JButton btnRefrescar = new JButton("Refrescar");
        barraSup.add(btnRefrescar);
        modeloGrilla = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        grilla = new JTable(modeloGrilla);
        grilla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grilla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scroll = new JScrollPane(grilla);
        JPanel barraInf = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        JButton btnNuevo    = new JButton("Nuevo");
        JButton btnEditar   = new JButton("Editar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnSalir    = new JButton("Salir");

        btnNuevo.setBackground(new Color(144, 238, 144));
        btnEditar.setBackground(new Color(173, 216, 230));
        btnEliminar.setBackground(new Color(255, 140, 140));
        barraInf.add(btnNuevo);
        barraInf.add(btnEditar);
        barraInf.add(btnEliminar);
        barraInf.add(btnSalir);
        lblEstado = new JLabel(" ");
        lblEstado.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(barraInf, BorderLayout.CENTER);
        sur.add(lblEstado, BorderLayout.SOUTH);
        principal.add(barraSup, BorderLayout.NORTH);
        principal.add(scroll,   BorderLayout.CENTER);
        principal.add(sur,      BorderLayout.SOUTH);
        add(principal);
        comboTablas.addActionListener(e -> {
            Object sel = comboTablas.getSelectedItem();
            if (sel != null) cargarDatos(sel.toString());
        });
        btnRefrescar.addActionListener(e -> { if (tablaActual != null) cargarDatos(tablaActual); });
        btnNuevo.addActionListener(e -> nuevo());
        btnEditar.addActionListener(e -> editar());
        btnEliminar.addActionListener(e -> eliminar());
        btnSalir.addActionListener(e -> { cerrar(); System.exit(0); });
    }
    
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

    private void cerrar() {
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException ignored) {}
    }

    private void cargarListaTablas() {
        if (conn == null) return;
        try {
            DatabaseMetaData md = conn.getMetaData();
            String catalogo = conn.getCatalog();
            try (ResultSet rs = md.getTables(catalogo, null, "%", new String[]{"TABLE"})) {
                List<String> tablas = new ArrayList<>();
                while (rs.next()) tablas.add(rs.getString("TABLE_NAME"));
                tablas.sort(String.CASE_INSENSITIVE_ORDER);
                comboTablas.removeAllItems();
                for (String t : tablas) comboTablas.addItem(t);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al listar las tablas:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarDatos(String tabla) {
        tablaActual = tabla;
        columnas.clear();
        clavesPrimarias.clear();

        try {
            DatabaseMetaData md = conn.getMetaData();
            String catalogo = conn.getCatalog();
            try (ResultSet rs = md.getColumns(catalogo, null, tabla, "%")) {
                while (rs.next()) {
                    Columna c = new Columna();
                    c.nombre        = rs.getString("COLUMN_NAME");
                    c.tipoSql       = rs.getInt("DATA_TYPE");
                    c.tipoNombre    = rs.getString("TYPE_NAME");
                    c.tamano        = rs.getInt("COLUMN_SIZE");
                    c.nullable      = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                    c.autoIncrement = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                    columnas.add(c);
                }
            }
            try (ResultSet rs = md.getPrimaryKeys(catalogo, null, tabla)) {
                while (rs.next()) clavesPrimarias.add(rs.getString("COLUMN_NAME"));
            }
            String[] cols = columnas.stream().map(c -> c.nombre).toArray(String[]::new);
            modeloGrilla.setColumnIdentifiers(cols);
            modeloGrilla.setRowCount(0);
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM `" + tabla + "`")) {
                while (rs.next()) {
                    Object[] fila = new Object[cols.length];
                    for (int i = 0; i < cols.length; i++) fila[i] = rs.getObject(i + 1);
                    modeloGrilla.addRow(fila);
                }
            }
            for (int i = 0; i < grilla.getColumnCount(); i++) {
                grilla.getColumnModel().getColumn(i).setPreferredWidth(120);
            }
            lblEstado.setText("Tabla: " + tabla
                + "  |  Registros: " + modeloGrilla.getRowCount()
                + "  |  PK: " + clavesPrimarias);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar la tabla " + tabla + ":\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void nuevo() {
        if (tablaActual == null) return;
        List<Columna> editables = new ArrayList<>();
        for (Columna c : columnas) if (!c.autoIncrement) editables.add(c);
        String[] valores = pedirDatos("Nuevo registro - " + tablaActual, editables, null);
        if (valores == null) return; // cancelado
        String campos = String.join(", ",
                editables.stream().map(c -> "`" + c.nombre + "`").toArray(String[]::new));
        String marcas = String.join(", ",
                editables.stream().map(c -> "?").toArray(String[]::new));
        String sql = "INSERT INTO `" + tablaActual + "` (" + campos + ") VALUES (" + marcas + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < editables.size(); i++)
                setParam(ps, i + 1, valores[i], editables.get(i));
            ps.executeUpdate();
            cargarDatos(tablaActual);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al insertar:\n" + e.getMessage(),
                "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editar() {
        if (tablaActual == null) return;
        int fila = grilla.getSelectedRow();
        if (fila < 0) { avisoSeleccion(); return; }

        if (clavesPrimarias.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "La tabla no tiene clave primaria; no se puede editar de forma segura.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] valoresActuales = new String[columnas.size()];
        for (int i = 0; i < columnas.size(); i++) {
            Object v = modeloGrilla.getValueAt(fila, i);
            valoresActuales[i] = (v == null) ? "" : v.toString();
        }

        String[] nuevos = pedirDatos("Editar registro - " + tablaActual, columnas, valoresActuales);
        if (nuevos == null) return;

        List<Columna> setCols = new ArrayList<>();
        List<Integer> setIdx  = new ArrayList<>();
        for (int i = 0; i < columnas.size(); i++) {
            if (!clavesPrimarias.contains(columnas.get(i).nombre)) {
                setCols.add(columnas.get(i));
                setIdx.add(i);
            }
        }

        String setClause = String.join(", ",
                setCols.stream().map(c -> "`" + c.nombre + "` = ?").toArray(String[]::new));
        String whereClause = String.join(" AND ",
                clavesPrimarias.stream().map(c -> "`" + c + "` = ?").toArray(String[]::new));
        String sql = "UPDATE `" + tablaActual + "` SET " + setClause + " WHERE " + whereClause;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (int k = 0; k < setCols.size(); k++)
                setParam(ps, idx++, nuevos[setIdx.get(k)], setCols.get(k));
            for (String pk : clavesPrimarias) {
                int ci = indiceColumna(pk);
                setParam(ps, idx++, valoresActuales[ci], columnas.get(ci));
            }
            ps.executeUpdate();
            cargarDatos(tablaActual);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al actualizar:\n" + e.getMessage(),
                "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        if (tablaActual == null) return;
        int fila = grilla.getSelectedRow();
        if (fila < 0) { avisoSeleccion(); return; }
        if (clavesPrimarias.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "La tabla no tiene clave primaria; no se puede eliminar de forma segura.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resp = JOptionPane.showConfirmDialog(this,
            "¿Eliminar el registro seleccionado de " + tablaActual + "?",
            "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resp != JOptionPane.YES_OPTION) return;

        String whereClause = String.join(" AND ",
                clavesPrimarias.stream().map(c -> "`" + c + "` = ?").toArray(String[]::new));
        String sql = "DELETE FROM `" + tablaActual + "` WHERE " + whereClause;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String pk : clavesPrimarias) {
                int ci = indiceColumna(pk);
                Object v = modeloGrilla.getValueAt(fila, ci);
                setParam(ps, idx++, v == null ? "" : v.toString(), columnas.get(ci));
            }
            ps.executeUpdate();
            cargarDatos(tablaActual);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al eliminar:\n" + e.getMessage()
                + "\n\n(Posible causa: el registro está referenciado por otra tabla.)",
                "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String[] pedirDatos(String titulo, List<Columna> campos, String[] valoresIniciales) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        boolean editando = valoresIniciales != null;
        JTextField[] cajas = new JTextField[campos.size()];

        for (int i = 0; i < campos.size(); i++) {
            Columna c = campos.get(i);
            gbc.gridx = 0; gbc.gridy = i; gbc.fill = GridBagConstraints.NONE;
            String etiqueta = c.nombre + "  (" + c.tipoNombre
                    + (clavesPrimarias.contains(c.nombre) ? ", PK" : "")
                    + (c.nullable ? "" : ", req") + ")";
            panel.add(new JLabel(etiqueta), gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
            JTextField caja = new JTextField(22);

            if (editando) {
                int ci = indiceColumna(c.nombre);
                if (ci >= 0 && ci < valoresIniciales.length)
                    caja.setText(valoresIniciales[ci]);
                if (clavesPrimarias.contains(c.nombre)) {
                    caja.setEditable(false);
                    caja.setBackground(new Color(225, 225, 225));
                }
            }
            if (c.tipoSql == Types.DATE) caja.setToolTipText("Formato: yyyy-MM-dd");
            cajas[i] = caja;
            panel.add(caja, gbc);
        }
        JScrollPane sp = new JScrollPane(panel);
        sp.setPreferredSize(new Dimension(440,
                Math.min(420, 60 + campos.size() * 36)));
        int opcion = JOptionPane.showConfirmDialog(this, sp, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) return null;
        String[] res = new String[campos.size()];
        for (int i = 0; i < campos.size(); i++) res[i] = cajas[i].getText().trim();
        return res;
    }

    private int indiceColumna(String nombre) {
        for (int i = 0; i < columnas.size(); i++)
            if (columnas.get(i).nombre.equalsIgnoreCase(nombre)) return i;
        return -1;
    }
    private void avisoSeleccion() {
        JOptionPane.showMessageDialog(this,
            "Seleccione primero un registro de la grilla.",
            "Aviso", JOptionPane.WARNING_MESSAGE);
    }
    private void setParam(PreparedStatement ps, int idx, String valor, Columna c) throws SQLException {
        if (valor == null || valor.isEmpty()) {
            if (c.nullable) { ps.setNull(idx, c.tipoSql); return; }
        }
        try {
            switch (c.tipoSql) {
                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                    ps.setInt(idx, Integer.parseInt(valor));
                    break;
                case Types.BIGINT:
                    ps.setLong(idx, Long.parseLong(valor));
                    break;
                case Types.DECIMAL:
                case Types.NUMERIC:
                    ps.setBigDecimal(idx, new BigDecimal(valor));
                    break;
                case Types.DOUBLE:
                case Types.FLOAT:
                case Types.REAL:
                    ps.setDouble(idx, Double.parseDouble(valor));
                    break;
                case Types.DATE:
                    ps.setDate(idx, Date.valueOf(valor)); 
                    break;
                case Types.TIMESTAMP:
                    ps.setTimestamp(idx, Timestamp.valueOf(valor));
                    break;
                default:
                    ps.setString(idx, valor);
            }
        } catch (IllegalArgumentException ex) {
            throw new SQLException("Valor inválido para la columna '" + c.nombre
                    + "' (" + c.tipoNombre + "): \"" + valor + "\"");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new CrudApp().setVisible(true);
        });
    }
}
