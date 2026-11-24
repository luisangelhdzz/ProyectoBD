package vista.tablas;
import conexion.BaseDatos;
import modelo.Bitacora;
import modelo.Dependencias;
import modelo.Ids;
import modelo.Sesion;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;


//declaracion de la clase
//es como un molde,trae todo lo que es igual en todas las pantallas,pero no es una pantalla terminada
//lo que trae hecho
//El formulario y la tabla.
//El buscador.
//Los botones Insertar, Actualizar, Eliminar y Limpiar.
//Las validaciones.
//El CRUD contra MySQL.
//La bitácora.
//El aviso de llave foránea al eliminar.
public abstract class PanelTabla extends JPanel {

    private static final Color VERDE = new Color(0, 128, 0);//constante para el color verde de los mensajes de éxito
    private static final int MAXIMO_ID = 10;//constante para el tamaño máximo del ID
    //un enum es una lista cerrada de opciones, en este caso los tipos de datos que puede tener un campo
    //sirve para aue el codigo decida que hacer segun el tipo de campo
    //por ejemplo en valores() hay un switch (c.tipo) que si es fecha convierte la fecha>
    private enum Tipo { ID, TEXTO, NUMERO, FECHA, FECHAHORA, MENU }
    //el patron significa
    //uuuu: año de 4 dígitos.
    //MM: mes (con M mayúscula).
    //dd: día.
    //HH: hora de 24 horas (con H mayúscula).
    //mm: minutos (con m minúscula). Ojo: MM es mes y mm es minutos.
    //.withResolverStyle(ResolverStyle.STRICT) hace el formato estricto:
    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
            //es la regla para escribir la fecha hacia mysql,(esta lleva segundos)
            //este formato apaga lose segundos, porque en la pantalla no se escriben segundos, solo minutos
    private static final DateTimeFormatter FORMATO_MYSQL = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    //es la ficha de una columna de la tabla de mysql,junto con la caja o el menu que lo edita en pantalla
    //no todos los datos se usan siempre,cada tipo de campo llena solo lo que necesita,lo demas se queda en su valor por defecto de java.
    private static class Campo {
        Tipo tipo;
        String columna;
        String etiqueta;
        boolean llave;
        int minimo;
        int maximo;
        String consulta;
        JTextField caja;
        //el <opcion> significa que el menu guarda objetos de tipo Opcion,una clase pequena que hicimos
        JComboBox<Opcion> menu;//menu depslegable
    }

    //representa una opcion del menu desplegable,cada renglon al abrirr un menu es un objeto opcion
    private static class Opcion {
        //un menu de llave foranea necesita dos cosas al mismo tiempo,lo que se ve y lo que se guarda en mysql
        final int id;//el numero que se guarda en mysql
        final String texto;//lo que se muestra en pantalla
        Opcion(int id, String texto) {//constrcutor
            this.id = id;
            this.texto = texto;
        }
        @Override//estoy reemplazando un metodo que ya existe(toString)
        public String toString() {
            return texto;
        }
        @Override//dos objetos sin iguales si tienen el mismo id aunque sean objetos distintos
        //puede llegar cualquier otra cosa por eso el parametro es object
        public boolean equals(Object otro) {
            //si lo que llego es realmente una opcion,si es texto o numero responde false
            // && solo si lo anterior fue true(es opcion) sigue
            //y convierte otro a opcion para poderr leer el id y comparar
            return otro instanceof Opcion && ((Opcion) otro).id == id;
        }
        @Override//si dos objetos son iguales deben tener el mismo hashcode,por eso lo hacemos igual al id
        public int hashCode() {
            return id;
        }
    }
    private final String tabla;//el nombre de la tabla en mysql
    //la lista de fichas campo,en el orden en que la pantalla hija las declara
    //una lista que solo acepta objetos Campo
    //crea la lista vacia,pero no la puede cambiar de lugar,solo agregar o quitar elementos
    private final List<Campo> campos = new ArrayList<>();//
    //un map guarda parejas de clave -> valor,como un diccionario
    //aqui lapareja es el nombre de la regla de mysql -> mensaje para el usuario
    //cuando mysql rechaza un registro traducir () busca en este map y muestra el mensaje en ves del error tecnico
    private final Map<String, String> avisos = new LinkedHashMap<>();
    //otro diccionario nombre de la columna -> pintor de colores de esa columna
    private final Map<String, ColorColumna> colores = new LinkedHashMap<>();
    //la caja del buscador de arriba,el 18 es su ancho aproximado en caracteres y no limita lo que puedes escriibir
    private final JTextField txtBuscar = new JTextField(18);
    //La etiqueta que dice "5 registros" o "2 de 5 registros". Empieza vacía y actualizarConteo() le pone el texto.
    private final JLabel lblConteo = new JLabel();
    //La etiqueta de mensajes debajo de los botones, donde salen en verde los éxitos y en rojo los errores. Empieza con un espacio, no vacía. Una etiqueta vacía mide cero de alto, y al aparecer el primer mensaje todo el formulario daría un salto. Con el espacio, ya ocupa su lugar.
    private final JLabel lblMensaje = new JLabel(" ");
    private DefaultTableModel modelo;//los datos,las filas y las columnas en memoria
    private JTable vista;//el componente que dibuja esos datos en pantalla
    private TableRowSorter<DefaultTableModel> ordenador;
    private Object[] llaveOriginal;//los valores de la llave primaria del registro seleccionado,tal como estaban antes de editarlo
    //es un arreglo porque puede tener varias columnas
    //para que sirve?
    //si cambias en el formulario un dato de la llave el UPDATE necesita ecnontrar la fila con el valor viejo,ese valor viejo esta aqui y se usa en ek where
    private boolean padre;//es true si otras tablas apuntan a esta llave foranea

    
    protected PanelTabla(String tabla) {
        this.tabla = tabla;
    }
    protected void llaveAutomatica(String columna) {
        Campo c = agregar(Tipo.ID, columna, "ID", true);
        c.caja = new JTextField(18);
        limitar(c.caja, MAXIMO_ID, "[0-9]*");
    }
    protected void menu(String columna, String etiqueta, String consulta) {
        agregarMenu(columna, etiqueta, consulta, false);
    }
    protected void llaveMenu(String columna, String etiqueta, String consulta) {
        agregarMenu(columna, etiqueta, consulta, true);
    }
    protected void texto(String columna, String etiqueta, int maximo) {
        Campo c = agregar(Tipo.TEXTO, columna, etiqueta, false);
        c.maximo = maximo;
        c.caja = new JTextField(18);
        limitar(c.caja, maximo, null);
    }
    protected void numero(String columna, String etiqueta, int minimo, int maximo) {
        Campo c = agregar(Tipo.NUMERO, columna, etiqueta, false);
        c.minimo = minimo;
        c.maximo = maximo;
        c.caja = new JTextField(18);
        limitar(c.caja, String.valueOf(maximo).length(), "[0-9]*");
    }
    protected void fecha(String columna, String etiqueta) {
        Campo c = agregar(Tipo.FECHA, columna, etiqueta, false);
        c.caja = new JTextField(18);
        limitar(c.caja, 10, "[0-9-]*");
    }
    protected void fechaHora(String columna, String etiqueta) {
        Campo c = agregar(Tipo.FECHAHORA, columna, etiqueta, false);
        c.caja = new JTextField(18);
        limitar(c.caja, 16, "[0-9: -]*");
    }
    protected void avisoRegla(String nombreRegla, String mensaje) {
        avisos.put(nombreRegla, mensaje);
    }
    protected void colorear(String columna, Color color, Predicate<String> cuando) {
        colores.computeIfAbsent(columna, k -> new ColorColumna()).agregar(cuando, color);
    }
    protected void colorear(String columna, Color color, String... valores) {
        colorear(columna, color, texto -> java.util.Arrays.asList(valores).contains(texto));
    }
    protected String reglaExtra() throws SQLException {
        return null;
    }
    protected void armarPantalla() {
        padre = Ids.esPadre(tabla);
        String[] titulos = new String[campos.size() + 1];
        for (int i = 0; i < campos.size(); i++) {
            titulos[i] = campos.get(i).etiqueta;
        }
        titulos[campos.size()] = "Último cambio por";
        modelo = new DefaultTableModel(titulos, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columna) {
                if (columna < campos.size()) {
                    Tipo tipo = campos.get(columna).tipo;
                    if (tipo == Tipo.ID || tipo == Tipo.NUMERO) {
                        return Integer.class;
                    }
                }
                return Object.class;
            }
        };
        vista = new JTable(modelo);
        ordenador = new TableRowSorter<>(modelo);
        setLayout(new BorderLayout(18, 14));
        setBorder(new EmptyBorder(18, 20, 18, 20));
        add(construirEncabezado(), BorderLayout.NORTH);
        add(construirFormulario(), BorderLayout.WEST);
        add(construirTabla(), BorderLayout.CENTER);
        addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent e) { cargar(); }
            public void ancestorRemoved(AncestorEvent e) { }
            public void ancestorMoved(AncestorEvent e) { }
        });
    }
    protected Integer idElegido(String columna) {
        Opcion o = (Opcion) buscarCampo(columna).menu.getSelectedItem();
        return o == null ? null : o.id;
    }
    protected LocalDate fechaEscrita(String columna) {
        return leerFecha(buscarCampo(columna).caja.getText().trim());
    }
    protected boolean existe(String sql, Object... valores) throws SQLException {
        return BaseDatos.existe(sql, valores);
    }
    private JComponent construirEncabezado() {
        JPanel enc = new JPanel(new BorderLayout());
        enc.add(lblConteo, BorderLayout.WEST);
        JPanel buscar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buscar.add(new JLabel("Buscar:"));
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        buscar.add(txtBuscar);
        enc.add(buscar, BorderLayout.EAST);
        return enc;
    }
    private JComponent construirFormulario() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Datos del registro"), new EmptyBorder(8, 10, 8, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        for (Campo c : campos) {
            g.insets = new Insets(0, 0, 4, 0);
            form.add(new JLabel(textoEtiqueta(c)), g);
            g.insets = new Insets(0, 0, 10, 0);
            form.add(c.tipo == Tipo.MENU ? c.menu : c.caja, g);
        }
        JButton btnInsertar = new JButton("Insertar");
        JButton btnActualizar = new JButton("Actualizar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnLimpiar = new JButton("Limpiar");
        btnInsertar.addActionListener(e -> insertar());
        btnActualizar.addActionListener(e -> actualizar());
        btnEliminar.addActionListener(e -> eliminar());
        btnLimpiar.addActionListener(e -> {
            limpiar();
            lblMensaje.setText(" ");
        });
        JPanel botones = new JPanel(new GridLayout(2, 2, 8, 8));
        botones.add(btnInsertar);
        botones.add(btnActualizar);
        botones.add(btnEliminar);
        botones.add(btnLimpiar);
        g.insets = new Insets(4, 0, 12, 0);
        form.add(botones, g);
        lblMensaje.setVerticalAlignment(SwingConstants.TOP);
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        form.add(lblMensaje, g);
        form.setPreferredSize(new Dimension(300, form.getPreferredSize().height));
        return form;
    }
    private String textoEtiqueta(Campo c) {
        switch (c.tipo) {
            case ID:
                return "ID (vacío = se asigna solo)";
            case TEXTO:
                return c.etiqueta + " * (máximo " + c.maximo + ")";
            case NUMERO:
                return c.etiqueta + " * (del " + c.minimo + " al " + c.maximo + ")";
            case FECHA:
                return c.etiqueta + " * (AAAA-MM-DD)";
            case FECHAHORA:
                return c.etiqueta + " * (AAAA-MM-DD HH:MM)";
            default:
                return c.etiqueta + " *";
        }
    }
    private JComponent construirTabla() {
        vista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vista.getTableHeader().setReorderingAllowed(false);
        vista.setRowSorter(ordenador);
        for (Map.Entry<String, ColorColumna> color : colores.entrySet()) {
            Campo c = buscarCampo(color.getKey());
            vista.getColumnModel().getColumn(campos.indexOf(c)).setCellRenderer(color.getValue());
        }
        vista.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionar();
            }
        });
        return new JScrollPane(vista);
    }
    private void cargar() {
        modelo.setRowCount(0);
        try {
            for (Campo c : campos) {
                if (c.tipo == Tipo.MENU) {
                    llenarMenu(c);
                }
            }
            String sql = "SELECT " + columnas("t.", false) + ", u.Login_Usuario FROM " + tabla + " t "
                    + "LEFT JOIN Usuario u ON u.ID_Usuario = t.ID_Usuario ORDER BY " + columnas("t.", true);
            for (Object[] renglon : BaseDatos.consultar(sql)) {
                Object[] fila = new Object[campos.size() + 1];
                for (int i = 0; i < campos.size(); i++) {
                    fila[i] = leer(renglon[i], campos.get(i));
                }
                fila[campos.size()] = renglon[campos.size()];
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
        actualizarConteo();
    }
    private void llenarMenu(Campo c) throws SQLException {
        Object elegida = c.menu.getSelectedItem();
        c.menu.removeAllItems();
        for (Object[] renglon : BaseDatos.consultar(c.consulta)) {
            int id = ((Number) renglon[0]).intValue();
            c.menu.addItem(new Opcion(id, id + " - " + (renglon[1] == null ? "(sin nombre)" : renglon[1])));
        }
        c.menu.setSelectedIndex(-1);
        c.menu.setSelectedItem(elegida);
    }
    private Object leer(Object valor, Campo c) {
        if (valor == null) {
            return null;
        }
        if (c.tipo == Tipo.ID || c.tipo == Tipo.NUMERO || c.tipo == Tipo.MENU) {
            int numero = ((Number) valor).intValue();
            return c.tipo == Tipo.MENU ? buscarOpcion(c, numero) : numero;
        }
        String texto = valor.toString();
        if (c.tipo == Tipo.FECHAHORA && texto.length() > 16) {
            texto = texto.substring(0, 16);
        }
        return texto;
    }
    private void insertar() {
        if (llaveOriginal != null) {
            error("Hay un registro seleccionado. Usa Limpiar para agregar uno nuevo.");
            return;
        }
        if (!validar(false)) {
            return;
        }
        String sql = "INSERT INTO " + tabla + " (" + columnas("", false) + ", ID_Usuario) VALUES (" + signos(campos.size() + 1) + ")";
        try {
            BaseDatos.ejecutar(sql, valores());
            Bitacora.registrar(tabla, "INSERTAR", describir());
            cargar();
            limpiar();
            exito("Registro agregado por " + Sesion.getLogin() + ".");
        } catch (SQLException e) {
            fallo(e);
        }
    }
    private void actualizar() {
        if (llaveOriginal == null) {
            error("Selecciona un registro de la tabla.");
            return;
        }
        if (!validar(true)) {
            return;
        }
        String sql = "UPDATE " + tabla + " SET " + asignaciones() + ", ID_Usuario = ? WHERE " + condicionLlave();
        try {
            List<Object> todos = new ArrayList<>(java.util.Arrays.asList(valores()));
            todos.addAll(java.util.Arrays.asList(llaveOriginal));
            int filas = BaseDatos.ejecutar(sql, todos.toArray());
            if (filas > 0) {
                Bitacora.registrar(tabla, "ACTUALIZAR", describir());
            }
            cargar();
            limpiar();
            if (filas == 0) {
                error("Ese registro ya no existe.");
            } else {
                exito("Registro actualizado por " + Sesion.getLogin() + ".");
            }
        } catch (SQLException e) {
            fallo(e);
        }
    }
    private void eliminar() {
        if (llaveOriginal == null) {
            error("Selecciona un registro de la tabla.");
            return;
        }
        try {
            StringBuilder ligados = new StringBuilder();
            int k = 0;
            for (Campo c : campos) {
                if (c.llave) {
                    String texto = Dependencias.buscar(tabla, c.columna, llaveOriginal[k++]);
                    if (texto != null) {
                        ligados.append(texto).append("\n");
                    }
                }
            }
            if (ligados.length() > 0) {
                JOptionPane.showMessageDialog(this,
                        "No se puede eliminar este registro porque está ligado por llave foránea con:\n\n"
                                + ligados + "\nPrimero elimina esos registros o cámbialos a otro.",
                        "Registro con datos ligados", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException e) {
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Seguro que deseas eliminar este registro?",
                "Eliminar registro", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (BaseDatos.ejecutar("DELETE FROM " + tabla + " WHERE " + condicionLlave(), llaveOriginal) > 0) {
                Bitacora.registrar(tabla, "ELIMINAR", describir());
            }
            cargar();
            limpiar();
            exito("Registro eliminado por " + Sesion.getLogin() + ".");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }
    private Object[] valores() throws SQLException {
        Object[] valores = new Object[campos.size() + 1];
        for (int i = 0; i < campos.size(); i++) {
            Campo c = campos.get(i);
            String texto = c.caja == null ? null : c.caja.getText().trim();
            switch (c.tipo) {
                case ID:
                    if (texto.isEmpty()) {
                        valores[i] = Ids.siguienteLibre(tabla, c.columna);
                    } else if (padre && llaveOriginal != null) {
                        valores[i] = llaveOriginal[0];
                    } else {
                        valores[i] = Integer.parseInt(texto);
                    }
                    break;
                case MENU:
                    valores[i] = ((Opcion) c.menu.getSelectedItem()).id;
                    break;
                case NUMERO:
                    valores[i] = Integer.parseInt(texto);
                    break;
                case FECHA:
                    valores[i] = leerFecha(texto).toString();
                    break;
                case FECHAHORA:
                    valores[i] = leerFechaHora(texto).format(FORMATO_MYSQL);
                    break;
                default:
                    valores[i] = texto;
            }
        }
        valores[campos.size()] = Sesion.getIdUsuario();
        return valores;
    }
    private void fallo(SQLException e) {
        if (e.getErrorCode() == 1216 || e.getErrorCode() == 1452) {
            cargar();
        }
        error(traducir(e));
    }
    private boolean validar(boolean actualizando) {
        String problema = null;
        JComponent donde = null;
        for (Campo c : campos) {
            problema = revisar(c, actualizando);
            if (problema != null) {
                donde = c.tipo == Tipo.MENU ? c.menu : c.caja;
                break;
            }
        }
        if (problema == null) {
            try {
                problema = reglaExtra();
            } catch (SQLException e) {
                problema = traducir(e);
            }
        }
        if (problema != null) {
            error(problema);
            if (donde != null) {
                donde.requestFocusInWindow();
            }
        }
        return problema == null;
    }
    private String revisar(Campo c, boolean actualizando) {
        if (c.tipo == Tipo.MENU) {
            return c.menu.getSelectedItem() == null ? "Elige una opción en «" + c.etiqueta + "»." : null;
        }
        String texto = c.caja.getText().trim();
        if (c.tipo == Tipo.ID) {
            if (texto.isEmpty()) {
                return actualizando ? "El ID no puede quedar vacío al actualizar." : null;
            }
            return esNumero(texto, 1, Integer.MAX_VALUE) ? null : "El ID debe ser un número entre 1 y 2147483647.";
        }
        if (texto.isEmpty()) {
            return "Escribe «" + c.etiqueta + "».";
        }
        switch (c.tipo) {
            case TEXTO:
                return texto.length() > c.maximo ? "«" + c.etiqueta + "» admite máximo " + c.maximo + " caracteres." : null;
            case NUMERO:
                return esNumero(texto, c.minimo, c.maximo) ? null
                        : "«" + c.etiqueta + "» debe ser un número del " + c.minimo + " al " + c.maximo + ".";
            case FECHA:
                return leerFecha(texto) != null ? null
                        : "«" + c.etiqueta + "» no es una fecha válida. Usa el formato AAAA-MM-DD, por ejemplo 2026-03-15.";
            case FECHAHORA:
                return leerFechaHora(texto) != null ? null
                        : "«" + c.etiqueta + "» no es válida. Usa el formato AAAA-MM-DD HH:MM (hora de 24 horas), por ejemplo 2026-03-15 19:30.";
            default:
                return null;
        }
    }
    private static boolean esNumero(String texto, long minimo, long maximo) {
        try {
            long n = Long.parseLong(texto);
            return n >= minimo && n <= maximo;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private static LocalDate leerFecha(String texto) {
        try {
            LocalDate fecha = LocalDate.parse(texto);
            return fecha.getYear() >= 1000 ? fecha : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    private static LocalDateTime leerFechaHora(String texto) {
        try {
            LocalDateTime fechaHora = LocalDateTime.parse(texto, FORMATO_FECHA_HORA);
            return fechaHora.getYear() >= 1000 ? fechaHora : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    private static void limitar(JTextField caja, int maximo, String permitidos) {
        ((AbstractDocument) caja.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int inicio, int borrados, String texto, AttributeSet atributos) throws BadLocationException {
                if (texto != null) {
                    if (permitidos != null && !texto.matches(permitidos)) {
                        return;
                    }
                    int caben = maximo - (fb.getDocument().getLength() - borrados);
                    if (texto.length() > caben) {
                        texto = texto.substring(0, Math.max(caben, 0));
                    }
                }
                super.replace(fb, inicio, borrados, texto, atributos);
            }
            @Override
            public void insertString(FilterBypass fb, int inicio, String texto, AttributeSet atributos) throws BadLocationException {
                replace(fb, inicio, 0, texto, atributos);
            }
        });
    }
    private void seleccionar() {
        int fila = vista.getSelectedRow();
        if (fila < 0) {
            return;
        }
        int f = vista.convertRowIndexToModel(fila);
        List<Object> llave = new ArrayList<>();
        for (int i = 0; i < campos.size(); i++) {
            Campo c = campos.get(i);
            Object valor = modelo.getValueAt(f, i);
            if (c.tipo == Tipo.MENU) {
                c.menu.setSelectedIndex(-1);
                c.menu.setSelectedItem(valor);
            } else {
                c.caja.setText(valor == null ? "" : valor.toString());
                c.caja.setEnabled(c.tipo != Tipo.ID || !padre);
            }
            if (c.llave) {
                llave.add(valor instanceof Opcion ? ((Opcion) valor).id : valor);
            }
        }
        llaveOriginal = llave.toArray();
        lblMensaje.setText(" ");
    }
    private void limpiar() {
        for (Campo c : campos) {
            if (c.tipo == Tipo.MENU) {
                c.menu.setSelectedIndex(-1);
            } else {
                c.caja.setText("");
                c.caja.setEnabled(true);
            }
        }
        vista.clearSelection();
        llaveOriginal = null;
    }
    private void filtrar() {
        String texto = txtBuscar.getText().trim();
        ordenador.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
        actualizarConteo();
    }
    private void actualizarConteo() {
        int total = modelo.getRowCount();
        int visibles = vista.getRowCount();
        String palabra = total == 1 ? " registro" : " registros";
        lblConteo.setText(visibles == total ? total + palabra : visibles + " de " + total + palabra);
    }
    private Campo agregar(Tipo tipo, String columna, String etiqueta, boolean llave) {
        Campo c = new Campo();
        c.tipo = tipo;
        c.columna = columna;
        c.etiqueta = etiqueta;
        c.llave = llave;
        campos.add(c);
        return c;
    }
    private void agregarMenu(String columna, String etiqueta, String consulta, boolean llave) {
        Campo c = agregar(Tipo.MENU, columna, etiqueta, llave);
        c.consulta = consulta;
        c.menu = new JComboBox<>();
        c.menu.setPrototypeDisplayValue(new Opcion(0, "Opción de ejemplo"));
    }
    private Campo buscarCampo(String columna) {
        for (Campo c : campos) {
            if (c.columna.equals(columna)) {
                return c;
            }
        }
        throw new IllegalArgumentException("No hay un campo con la columna " + columna);
    }
    private static Opcion buscarOpcion(Campo c, int id) {
        for (int i = 0; i < c.menu.getItemCount(); i++) {
            if (c.menu.getItemAt(i).id == id) {
                return c.menu.getItemAt(i);
            }
        }
        return new Opcion(id, String.valueOf(id));
    }
    private String columnas(String prefijo, boolean soloLlave) {
        StringBuilder sb = new StringBuilder();
        for (Campo c : campos) {
            if (soloLlave && !c.llave) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(prefijo).append(c.columna);
        }
        return sb.toString();
    }
    private String asignaciones() {
        StringBuilder sb = new StringBuilder();
        for (Campo c : campos) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(c.columna).append(" = ?");
        }
        return sb.toString();
    }
    private String condicionLlave() {
        StringBuilder sb = new StringBuilder();
        for (Campo c : campos) {
            if (c.llave) {
                if (sb.length() > 0) {
                    sb.append(" AND ");
                }
                sb.append(c.columna).append(" = ?");
            }
        }
        return sb.toString();
    }
    private static String signos(int cuantos) {
        StringBuilder sb = new StringBuilder("?");
        for (int i = 1; i < cuantos; i++) {
            sb.append(", ?");
        }
        return sb.toString();
    }
    private String describir() {
        StringBuilder sb = new StringBuilder();
        for (Campo c : campos) {
            String valor;
            if (c.tipo == Tipo.MENU) {
                Opcion o = (Opcion) c.menu.getSelectedItem();
                valor = o == null ? "" : String.valueOf(o.id);
            } else {
                valor = c.caja.getText().trim();
            }
            if (valor.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(c.etiqueta).append(": ").append(valor);
        }
        return sb.length() > 100 ? sb.substring(0, 100) : sb.toString();
    }
    private void exito(String texto) {
        lblMensaje.setForeground(VERDE);
        lblMensaje.setText("<html>" + texto + "</html>");
    }
    private void error(String texto) {
        lblMensaje.setForeground(Color.RED);
        lblMensaje.setText("<html>" + texto + "</html>");
    }
    private String traducir(SQLException e) {
        String detalle = e.getMessage() == null ? "" : e.getMessage();
        if (e.getErrorCode() == 1062 || e.getErrorCode() == 3819) {
            for (Map.Entry<String, String> aviso : avisos.entrySet()) {
                if (detalle.contains(aviso.getKey())) {
                    return aviso.getValue();
                }
            }
        }
        switch (e.getErrorCode()) {
            case 1062:
                return "Ya existe un registro con esos mismos datos de llave.";
            case 3819:
                return "Un dato no cumple una regla de la tabla: " + detalle;
            case 1216:
            case 1452:
                return "Algo que elegiste en un menú ya no existe en su tabla. Ya se actualizaron los menús, vuelve a elegirlo.";
            case 1217:
            case 1451:
                return "No se puede: hay registros en otras tablas que dependen de este registro.";
            case 1406:
                return "Un texto es más largo de lo permitido.";
            case 1292:
                return "Una fecha o un número no es válido.";
            case 1142:
                return "El usuario de la aplicación no tiene permiso para esta operación.";
            case 1054:
                return "Falta la columna ID_Usuario en la tabla " + tabla + ". Ejecuta el script usuarios_finales.sql.";
            case 1146:
                return "Falta una tabla (" + tabla + " o Bitacora). Ejecuta el script usuarios_finales.sql.";
            default:
                return "Error de MySQL " + e.getErrorCode() + ": " + detalle;
        }
    }
}
