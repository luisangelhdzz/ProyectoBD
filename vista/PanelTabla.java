package vista;
//Conexion: da la conexion abierta a MySQL con Conexion.obtener(),se usa para hacer los SELECT, INSERT, UPDATE y DELETE
import conexion.Conexion;
//Campo: describe una columna de la tabla (nombre en MySQL, etiqueta, tipo, si es llave, si es obligatoria...)
//con la lista de Campos esta pantalla sabe que cajas dibujar y que columnas consultar
import modelo.Campo;
//Opcion: cada renglon de una lista desplegable (JComboBox) de llave foranea,junta el id que va a MySQL y el texto que ve el usuario
import modelo.Opcion;
//Sesion: guarda quien inicio sesion,aqui se usa getIdUsuario() para dejar el id en el registro y getLogin() para el mensaje de exito
import modelo.Sesion;

//el * importa todas las clases de javax.swing: JPanel, JLabel, JButton, JTextField, JTable, JComboBox,
//JScrollPane, JOptionPane, JComponent, RowFilter, SwingUtilities, etc.
import javax.swing.*;
//CompoundBorder: junta dos bordes en uno (aqui una linea por fuera + espacio vacio por dentro en el formulario)
import javax.swing.border.CompoundBorder;
//EmptyBorder: borde invisible que solo deja margen (espacio) alrededor de un componente
import javax.swing.border.EmptyBorder;
//LineBorder: borde que dibuja una linea de color alrededor (el marco del formulario y de la tabla)
import javax.swing.border.LineBorder;
//DocumentEvent: el "aviso" que manda una caja de texto cuando su contenido cambia (se escribio o se borro algo)
import javax.swing.event.DocumentEvent;
//DocumentListener: el "escuchador" que recibe esos avisos,se usa en el buscador para filtrar la tabla en cada tecla que escribes
import javax.swing.event.DocumentListener;

//DefaultTableCellRenderer: decide COMO se dibuja cada celda (colores, renglones alternados, alineacion)
//se usa para darle estilo a las celdas y al encabezado de la tabla
import javax.swing.table.DefaultTableCellRenderer;
//DefaultTableModel: es donde se GUARDAN los datos de la tabla (renglones y columnas),la JTable solo los muestra
import javax.swing.table.DefaultTableModel;
//JTableHeader: el renglon de arriba de la tabla con los nombres de las columnas,se obtiene para cambiarle el estilo
import javax.swing.table.JTableHeader;
//TableRowSorter: permite ordenar la tabla dando clic en el encabezado y FILTRAR renglones (lo usa el buscador)
import javax.swing.table.TableRowSorter;


//el * importa BorderLayout, FlowLayout, GridBagLayout, GridBagConstraints, Insets, Dimension, Color, Font, Component...
//OJO: java.awt tambien tiene una clase llamada List; como abajo se importa java.util.List de forma explicita,
//Java usa la de java.util (un import con nombre exacto gana sobre un import con *)
import java.awt.*;

//BigDecimal: numero decimal exacto (sin errores de redondeo como double),se usa para las columnas DECIMAL de MySQL
import java.math.BigDecimal;
//el * importa todo JDBC: Statement (consulta sin huecos), PreparedStatement (consulta con ? que se llenan despues),
//ResultSet (los renglones que devuelve un SELECT), SQLException (error de base de datos) y Types (Types.NULL para mandar un NULL)
//las fechas se escriben como java.sql.Date con nombre completo para que no se confundan con java.util.Date
import java.sql.*;

//ArrayList: la implementacion concreta de una lista que crece sola (posiciones de las llaves, nombres de columnas...)
import java.util.ArrayList;
//LinkedHashMap: un mapa (clave -> valor) que RECUERDA el orden en que se metieron los datos
//se usa para relacionar cada Campo con su control (caja de texto o combo) respetando el orden del formulario
import java.util.LinkedHashMap;
//List: la interfaz "lista"; se declaran las variables como List y se crean como ArrayList
import java.util.List;
//Map: la interfaz "mapa"; se declara como Map y se crea como LinkedHashMap
import java.util.Map;
//Pattern: expresiones regulares; Pattern.quote() hace que lo que escribes en el buscador se busque como texto literal
//(asi un punto o un parentesis no se interpretan como simbolos especiales de regex)
import java.util.regex.Pattern;

//este es el molde del que heredan todas las pantallas de tablas como panelEstado,aqui se declara que datos guarda cada panel y el constructor que los recibe
public abstract class PanelTabla extends JPanel {

    //un nombre de columna fijo compartido por todas las instancias
    private static final String COLUMNA_USUARIO = "ID_Usuario";

    private final String tabla;//nombre real de la tabla en mysql
    private final String singular;//nombre en singular para los mensajes
    private final boolean femenino;//indica si el sustantivo es femenino
    private final String plural;//nombre en plural
    private final List<Campo> campos;//lista de objetos campo que describen cada columna
    private final List<Integer> posLlaves = new ArrayList<>();//posiciones (indices) de la columnas que froman la llave primaria,sirve para saber que valores usar en el where al editar o borrar
    private final Map<Campo, JComponent> controles = new LinkedHashMap<>();//relaciona cada campo con su componente del fromulario

    private DefaultTableModel modelo;//guarda los datos de la tabla filas y columnas que vienen de la bd
    private JTable vista;//el componente visual que muestra ese modeo
    private TableRowSorter<DefaultTableModel> ordenador;//permite ordenar al hacer clic en los encabezados y filtrar filas
    private JTextField txtBuscar;//caja de texxto de busqueda,lo que escribas se usa para filtrar con el ordenador
    private JLabel lblConteo;
    private JLabel lblEstado;

    //guarda los valores de la llave primaria del registro que estoy editando tal como estaban antes de editarlo
    //hace falta porque si el usuario selecciona el estado con ID y y cambia el iD a 7 en el formulario,el UPDATE tiene que buscar la fila
    private Object[] llaveOriginal;

    //sirve para explicar que es cada parametro de un metodo o constructor
    /**
     * @param tabla    nombre de la tabla en MySQL, por ejemplo "Estado"
     * @param singular cómo se llama un registro, por ejemplo "estado"
     * @param femenino true si es "la" (la posición), false si es "el" (el estado)
     * @param plural   título de la sección, por ejemplo "Estados"
     * @param campos   columnas de la tabla, en orden
     */

    //solo lo pueden llamar las subclases,tiene sentido porque la clase es abstracta
    //son varargs,permite pasar cuantos campos quieras separados por comas y dentro del metdodo campos se comporta como un arreglo campo[]
    protected PanelTabla(String tabla, String singular, boolean femenino, String plural, Campo... campos) {
        this.tabla = tabla;
        this.singular = singular;
        this.femenino = femenino;
        this.plural = plural;
        this.campos = List.of(campos);//convierte el arreglo en una lista inmutable,asi nadie puede quitar o agregar campos por accidente
        //recorre los campos y anota la posicion de los que son llave,
        //por ejemplo si IDESTADO es el campo 0 yes llave,entonces posLlaves=[0]
        //mas adelante basta con leer las columnas 0 del modelo
        for (int i = 0; i < campos.length; i++) {
            //valida,si declaraste una tabla sin ningun campo llave,el programa falla de inmediato con un mensaje claro
            //sin llave no habria forma de armar el where para editar y borrar,
            if (campos[i].esLlave()) {
                posLlaves.add(i);
            }
        }
        if (posLlaves.isEmpty()) {
            throw new IllegalArgumentException("La tabla " + tabla + " necesita al menos un campo llave.");
        }
        //llama a otro metodo que crea la JTable,la caja de busqueda,los botones y el formulario a partir de campos
        construir();
    }

    //un getter simple que devuelve el titulo de la seccion ,la usara quien arme el menu o las pestanas para mostrar el nombre
    public String getTitulo() {
        return plural;
    }

    // Se llama cada vez que se abre la sección desde el menú. 
    public void alMostrar() {
        txtBuscar.setText("");//borra la busqueda anterior para que no quede un filtro :pegado"
        cargarOpciones();//recarga las listas de los combos de llaves foraneas
        cargar();//vuelve a hacer el select y llena el modelo de la tabla
        limpiar();//vaica el formulario,dejando el panel listo para un registro nuevo
    }



    //arman la parte visual del panel
    private void construir() {
        setLayout(new BorderLayout(18, 14));
        setBackground(Estilo.CREMA);
        setBorder(new EmptyBorder(18, 20, 18, 20));//para que nada qued pegado al borde

        add(construirEncabezado(), BorderLayout.NORTH);
        add(construirFormulario(), BorderLayout.WEST);
        add(construirTabla(), BorderLayout.CENTER);
    }

    //para poner los titulos a la izquierda y la busqueda a la derecha
    private JComponent construirEncabezado() {
        JPanel enc = new JPanel(new BorderLayout());
        enc.setOpaque(false);

        JPanel titulos = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));//acomoda los componentes uno tras otro en fila,alineados a la izquierda y sin separacion
        titulos.setOpaque(false);
        titulos.add(Estilo.etiqueta(plural, 10, true, Estilo.VERDE));
        lblConteo = Estilo.etiqueta("", 0, false, Estilo.GRIS);//crea la etiqueta del conteo vacia
        lblConteo.setBorder(new EmptyBorder(0, 12, 0, 0));
        titulos.add(lblConteo);
        enc.add(titulos, BorderLayout.WEST);
        //otra fila alineada a la derecha
        JPanel buscar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buscar.setOpaque(false);
        buscar.add(Estilo.etiqueta("Buscar:", 0, true, Estilo.TEXTO));
        //buscar mientras escribes
        //el texto de un JTextField vive en un document y documentlistener es un escuchador que avisa cada vez que ese texto cambis
        //se crea con clase anonima;implementa la interfaz ahi mismo,sin crear un archivo aparte
        //la interfaz obliga a implementar los 3 metodos
        //insertUpdate se escribio un caracter o se pego texto
        //removeUpdate se borro texto
        //changedUpdate cambio el formato del texto
        //los tres llaman a filtrar() asi que la tabla se filtra en tiempo real con cada tecla
        txtBuscar = new JTextField(18);
        Estilo.estiloCampo(txtBuscar);
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        //se usa dcoumentlistener en lugar de actionlistener porque este ultimo solo se dispara al presionar enter,mientras que el primero reacciona a cualquier cambio
        buscar.add(txtBuscar);
        //agrega la caja al panel de busqueda lo coloca a la derecha del encabezado y devuelve el encabezado completo para que construir() lo ponga en north
        enc.add(buscar, BorderLayout.EAST);
        return enc;
    }

    //arma la columna izquierda del panel
    private JComponent construirFormulario() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Estilo.BLANCO);
        form.setBorder(new CompoundBorder(new LineBorder(Estilo.CREMA_2, 1), new EmptyBorder(16, 16, 16, 16)));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 12, 0);
        form.add(Estilo.etiqueta("Datos " + (femenino ? "de la " : "del ") + singular, 3, true, Estilo.TEXTO), g);

        for (Campo c : campos) {
            g.insets = new Insets(0, 0, 4, 0);
            form.add(Estilo.etiqueta(c.getEtiqueta() + (c.esObligatorio() ? " *" : ""), 0, true, Estilo.TEXTO), g);

            JComponent control = crearControl(c);
            controles.put(c, control);
            g.insets = new Insets(0, 0, c.getAyuda().isEmpty() ? 12 : 2, 0);
            form.add(control, g);

            if (!c.getAyuda().isEmpty()) {
                g.insets = new Insets(0, 0, 12, 0);
                form.add(Estilo.etiqueta(c.getAyuda(), -2, false, Estilo.GRIS), g);
            }
        }

        JButton btnInsertar = Estilo.boton("Insertar", Estilo.ARCILLA);
        JButton btnActualizar = Estilo.boton("Actualizar", Estilo.VERDE);
        JButton btnEliminar = Estilo.boton("Eliminar", Estilo.ROJO);
        JButton btnLimpiar = Estilo.boton("Limpiar", Estilo.GRIS);
        btnInsertar.addActionListener(e -> insertar());
        btnActualizar.addActionListener(e -> actualizar());
        btnEliminar.addActionListener(e -> eliminar());
        btnLimpiar.addActionListener(e -> {
            limpiar();
            lblEstado.setText(" ");
        });

        JPanel botones = new JPanel(new GridLayout(2, 2, 8, 8));
        botones.setOpaque(false);
        botones.add(btnInsertar);
        botones.add(btnActualizar);
        botones.add(btnEliminar);
        botones.add(btnLimpiar);
        g.insets = new Insets(6, 0, 10, 0);
        form.add(botones, g);

        lblEstado = Estilo.etiqueta(" ", -1, false, Estilo.GRIS);
        g.insets = new Insets(0, 0, 0, 0);
        form.add(lblEstado, g);

        // Relleno para que todo quede arriba
        g.weighty = 1;
        form.add(Box.createVerticalGlue(), g);

        form.setPreferredSize(new Dimension(300, form.getPreferredSize().height));
        return form;
    }

    private JComponent crearControl(Campo c) {
        if (c.getTipo() == Campo.Tipo.FORANEA) {
            JComboBox<Opcion> combo = new JComboBox<>();
            Estilo.estiloCampo(combo);
            return combo;
        }
        JTextField txt = new JTextField(18);
        Estilo.estiloCampo(txt);
        return txt;
    }

    private JComponent construirTabla() {
        String[] columnas = new String[campos.size() + 1];
        for (int i = 0; i < campos.size(); i++) {
            columnas[i] = campos.get(i).getEtiqueta();
        }
        columnas[campos.size()] = "Último cambio por";

        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        vista = new JTable(modelo);
        vista.setFont(Estilo.fuente(0, false));
        vista.setRowHeight(28);
        vista.setShowVerticalLines(false);
        vista.setGridColor(Estilo.CREMA_2);
        vista.setSelectionBackground(Estilo.ARCILLA);
        vista.setSelectionForeground(Estilo.BLANCO);
        vista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vista.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, f, c);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel) {
                    setBackground(f % 2 == 0 ? Estilo.BLANCO : Estilo.CREMA);
                    setForeground(c == campos.size() ? Estilo.GRIS : Estilo.TEXTO);
                }
                return this;
            }
        });

        JTableHeader encabezado = vista.getTableHeader();
        encabezado.setReorderingAllowed(false);
        encabezado.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                super.getTableCellRendererComponent(t, v, sel, foco, f, c);
                setBackground(Estilo.VERDE);
                setForeground(Estilo.BLANCO);
                setFont(Estilo.fuente(0, true));
                setBorder(new EmptyBorder(8, 8, 8, 8));
                return this;
            }
        });

        ordenador = new TableRowSorter<>(modelo);
        vista.setRowSorter(ordenador);
        vista.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionar();
            }
        });

        JScrollPane scroll = new JScrollPane(vista);
        scroll.setBorder(new LineBorder(Estilo.CREMA_2, 1));
        scroll.getViewport().setBackground(Estilo.BLANCO);
        return scroll;
    }

    // ==================================================================
    // CONSULTAS A MYSQL
    // ==================================================================

    /** Llena las listas desplegables de las llaves foráneas. */
    private void cargarOpciones() {
        for (Campo c : campos) {
            if (c.getTipo() != Campo.Tipo.FORANEA) {
                continue;
            }
            @SuppressWarnings("unchecked")
            JComboBox<Opcion> combo = (JComboBox<Opcion>) controles.get(c);
            combo.removeAllItems();
            combo.addItem(Opcion.NINGUNA);
            String sql = "SELECT `" + c.getColumna() + "`, `" + c.getColumnaMostrar() + "` FROM `"
                    + c.getTablaReferencia() + "` ORDER BY `" + c.getColumnaMostrar() + "`";
            try (Statement st = Conexion.obtener().createStatement(); ResultSet resultado = st.executeQuery(sql)) {
                while (resultado.next()) {
                    combo.addItem(new Opcion(resultado.getInt(1), resultado.getString(2)));
                }
            } catch (SQLException e) {
                error(traducir(e));
            }
        }
    }

    /** Trae todos los registros de la tabla junto con el usuario que hizo el último cambio. */
    private void cargar() {
        StringBuilder sql = new StringBuilder("SELECT ");
        for (Campo c : campos) {
            sql.append("t.`").append(c.getColumna()).append("`, ");
        }
        sql.append("u.Login_Usuario FROM `").append(tabla).append("` t ")
                .append("LEFT JOIN Usuario u ON u.ID_Usuario = t.").append(COLUMNA_USUARIO)
                .append(" ORDER BY ");
        for (int i = 0; i < posLlaves.size(); i++) {
            sql.append(i > 0 ? ", " : "").append("t.`").append(campos.get(posLlaves.get(i)).getColumna()).append("`");
        }

        modelo.setRowCount(0);
        try (Statement st = Conexion.obtener().createStatement(); ResultSet resultado = st.executeQuery(sql.toString())) {
            while (resultado.next()) {
                Object[] fila = new Object[campos.size() + 1];
                for (int i = 0; i < campos.size(); i++) {
                    fila[i] = valorParaTabla(campos.get(i), resultado, i + 1);
                }
                String usuario = resultado.getString(campos.size() + 1);
                fila[campos.size()] = usuario == null ? "" : usuario;
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
        actualizarConteo();
    }

    private Object valorParaTabla(Campo c, ResultSet resultado, int columna) throws SQLException {
        if (c.getTipo() == Campo.Tipo.FORANEA) {
            int id = resultado.getInt(columna);
            if (resultado.wasNull()) {
                return "";
            }
            // Se muestra el nombre del registro padre en lugar de su número
            JComboBox<?> combo = (JComboBox<?>) controles.get(c);
            for (int i = 0; i < combo.getItemCount(); i++) {
                Opcion o = (Opcion) combo.getItemAt(i);
                if (o.getId() != null && o.getId() == id) {
                    return o;
                }
            }
            return new Opcion(id, String.valueOf(id));
        }
        String valor = resultado.getString(columna);
        return valor == null ? "" : valor;
    }

    private void insertar() {
        Object[] valores;
        try {
            valores = leerFormulario(false);
        } catch (ValidacionException e) {
            avisoValidacion(e);
            return;
        }

        List<String> columnas = new ArrayList<>();
        List<Object> datos = new ArrayList<>();
        for (int i = 0; i < campos.size(); i++) {
            if (valores[i] == null && campos.get(i).esAutoIncremento()) {
                continue; // MySQL asigna el número
            }
            columnas.add("`" + campos.get(i).getColumna() + "`");
            datos.add(valores[i]);
        }
        columnas.add(COLUMNA_USUARIO);
        datos.add(Sesion.getIdUsuario());

        String sql = "INSERT INTO `" + tabla + "` (" + String.join(", ", columnas) + ") VALUES ("
                + "?, ".repeat(datos.size() - 1) + "?)";
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps, datos, 1);
            ps.executeUpdate();
            cargar();
            limpiar();
            exito(capital(singular) + " registrad" + o() + " por " + Sesion.getLogin() + ".");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    private void actualizar() {
        if (llaveOriginal == null) {
            error("Selecciona " + un() + singular + " de la tabla.");
            return;
        }
        Object[] valores;
        try {
            valores = leerFormulario(true);
        } catch (ValidacionException e) {
            avisoValidacion(e);
            return;
        }

        List<String> asignaciones = new ArrayList<>();
        List<Object> datos = new ArrayList<>();
        for (int i = 0; i < campos.size(); i++) {
            asignaciones.add("`" + campos.get(i).getColumna() + "` = ?");
            datos.add(valores[i]);
        }
        asignaciones.add(COLUMNA_USUARIO + " = ?");
        datos.add(Sesion.getIdUsuario());

        String sql = "UPDATE `" + tabla + "` SET " + String.join(", ", asignaciones) + " WHERE " + condicionLlave();
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            int siguiente = asignar(ps, datos, 1);
            asignar(ps, List.of(llaveOriginal), siguiente);
            int filas = ps.executeUpdate();
            cargar();
            limpiar();
            if (filas == 0) {
                error((femenino ? "Esa " : "Ese ") + singular + " ya no existe.");
            } else {
                exito(capital(singular) + " actualizad" + o() + " por " + Sesion.getLogin() + ".");
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    private void eliminar() {
        if (llaveOriginal == null) {
            error("Selecciona " + un() + singular + " de la tabla.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Seguro que deseas eliminar " + (femenino ? "esta " : "este ") + singular + "?",
                "Eliminar " + singular, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM `" + tabla + "` WHERE " + condicionLlave();
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps, List.of(llaveOriginal), 1);
            ps.executeUpdate();
            cargar();
            limpiar();
            exito(capital(singular) + " eliminad" + o() + ".");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    private String condicionLlave() {
        List<String> partes = new ArrayList<>();
        for (int pos : posLlaves) {
            partes.add("`" + campos.get(pos).getColumna() + "` = ?");
        }
        return String.join(" AND ", partes);
    }

    /** Pone los valores en los ? del PreparedStatement. Devuelve la siguiente posición libre. */
    private int asignar(PreparedStatement ps, List<Object> datos, int desde) throws SQLException {
        int i = desde;
        for (Object d : datos) {
            if (d == null) {
                ps.setNull(i++, Types.NULL);
            } else {
                ps.setObject(i++, d);
            }
        }
        return i;
    }

    // ==================================================================
    // FORMULARIO
    // ==================================================================

    /** Lee y valida lo que hay en el formulario. Devuelve un valor por campo (null = vacío). */
    private Object[] leerFormulario(boolean actualizando) throws ValidacionException {
        Object[] valores = new Object[campos.size()];
        for (int i = 0; i < campos.size(); i++) {
            Campo c = campos.get(i);
            JComponent control = controles.get(c);

            if (c.getTipo() == Campo.Tipo.FORANEA) {
                Opcion o = (Opcion) ((JComboBox<?>) control).getSelectedItem();
                if (o == null || o.getId() == null) {
                    if (c.esObligatorio()) {
                        throw new ValidacionException("Elige " + c.getEtiqueta().toLowerCase() + ".", control);
                    }
                    valores[i] = null;
                } else {
                    valores[i] = o.getId();
                }
                continue;
            }

            String texto = ((JTextField) control).getText().trim();
            if (texto.isEmpty()) {
                if (c.esAutoIncremento() && actualizando) {
                    throw new ValidacionException("El " + c.getEtiqueta() + " no puede quedar vacío al actualizar.", control);
                }
                if (c.esObligatorio()) {
                    throw new ValidacionException("Escribe " + c.getEtiqueta().toLowerCase() + ".", control);
                }
                valores[i] = null;
                continue;
            }

            switch (c.getTipo()) {
                case ENTERO:
                    try {
                        int n = Integer.parseInt(texto);
                        if (c.esLlave() && n <= 0) {
                            throw new NumberFormatException();
                        }
                        valores[i] = n;
                    } catch (NumberFormatException e) {
                        throw new ValidacionException(c.esLlave()
                                ? "El " + c.getEtiqueta() + " debe ser un número entero mayor que 0."
                                : c.getEtiqueta() + " debe ser un número entero.", control);
                    }
                    break;
                case DECIMAL:
                    try {
                        valores[i] = new BigDecimal(texto.replace(',', '.'));
                    } catch (NumberFormatException e) {
                        throw new ValidacionException(c.getEtiqueta() + " debe ser un número.", control);
                    }
                    break;
                case FECHA:
                    try {
                        valores[i] = java.sql.Date.valueOf(texto);
                    } catch (IllegalArgumentException e) {
                        throw new ValidacionException(c.getEtiqueta() + " debe tener el formato AAAA-MM-DD.", control);
                    }
                    break;
                default: // TEXTO
                    if (c.getLongitudMaxima() > 0 && texto.length() > c.getLongitudMaxima()) {
                        throw new ValidacionException(c.getEtiqueta() + " admite máximo "
                                + c.getLongitudMaxima() + " caracteres.", control);
                    }
                    valores[i] = texto;
            }
        }
        return valores;
    }

    /** Pasa la fila elegida en la tabla al formulario. */
    private void seleccionar() {
        int fila = vista.getSelectedRow();
        if (fila < 0) {
            return;
        }
        int f = vista.convertRowIndexToModel(fila);

        for (int i = 0; i < campos.size(); i++) {
            Campo c = campos.get(i);
            Object valor = modelo.getValueAt(f, i);
            JComponent control = controles.get(c);
            if (c.getTipo() == Campo.Tipo.FORANEA) {
                ((JComboBox<?>) control).setSelectedItem(valor instanceof Opcion ? valor : Opcion.NINGUNA);
            } else {
                ((JTextField) control).setText(valor.toString());
            }
        }

        llaveOriginal = new Object[posLlaves.size()];
        for (int i = 0; i < posLlaves.size(); i++) {
            Object valor = modelo.getValueAt(f, posLlaves.get(i));
            llaveOriginal[i] = valor instanceof Opcion ? ((Opcion) valor).getId() : valor;
        }
        lblEstado.setText(" ");
    }

    private void limpiar() {
        for (JComponent control : controles.values()) {
            if (control instanceof JComboBox) {
                JComboBox<?> combo = (JComboBox<?>) control;
                if (combo.getItemCount() > 0) {
                    combo.setSelectedIndex(0);
                }
            } else {
                ((JTextField) control).setText("");
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

    // ==================================================================
    // MENSAJES
    // ==================================================================

    private void exito(String texto) {
        lblEstado.setForeground(Estilo.VERDE);
        lblEstado.setText("<html>" + texto + "</html>");
    }

    private void error(String texto) {
        lblEstado.setForeground(Estilo.ROJO);
        lblEstado.setText("<html>" + texto + "</html>");
    }

    private void avisoValidacion(ValidacionException e) {
        error(e.getMessage());
        e.control.requestFocusInWindow();
    }

    /** Convierte los errores de MySQL en mensajes entendibles. */
    private String traducir(SQLException e) {
        switch (e.getErrorCode()) {
            case 1062:
                return "Ya existe " + un() + singular + " con ese ID.";
            case 1451:
                return "No se puede: hay registros en otras tablas que dependen de " + (femenino ? "esta " : "este ") + singular + ".";
            case 1452:
                return "El registro elegido en una lista ya no existe.";
            case 1406:
                return "Un texto es más largo de lo permitido.";
            case 3819:
            case 4025:
                return "Un valor no cumple las reglas de la tabla.";
            case 1142:
                return "El usuario de la aplicación no tiene permiso para esta operación.";
            case 1054:
                return "Falta la columna ID_Usuario en la tabla " + tabla + ". Ejecuta el script usuarios_finales.sql.";
            case 1146:
                return "No existe la tabla " + tabla + ".";
            default:
                return "Error de MySQL " + e.getErrorCode() + ": " + e.getMessage();
        }
    }

    private String un() {
        return femenino ? "una " : "un ";
    }

    private String o() {
        return femenino ? "a" : "o";
    }

    private static String capital(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static class ValidacionException extends Exception {
        final JComponent control;

        ValidacionException(String mensaje, JComponent control) {
            super(mensaje);
            this.control = control;
        }
    }
}
