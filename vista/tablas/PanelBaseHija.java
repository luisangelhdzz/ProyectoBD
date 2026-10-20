//esta es la clase BASE de las pantallas de las tablas HIJAS (Ciudad, Contrato, Alineacion...)
//una tabla hija es la que tiene llaves foraneas: guarda el ID de un registro de otra tabla,su tabla padre
//cada llave foranea se elige con un menu desplegable (JComboBox) que se llena con los registros de la tabla padre,
//asi solo se puede elegir algo que si existe y MySQL nunca marca el error de llave foranea (1452)
//la clase hija solo declara sus campos en el constructor (columna,etiqueta y tipo) y esta clase hace todo lo demas:
//el formulario,la tabla,el buscador,las validaciones,el CRUD,la bitacora y los mensajes
//PanelBase sigue siendo la base de las tablas padre,las que solo tienen ID y nombre

package vista.tablas;

import conexion.Conexion;
import modelo.Bitacora;
import modelo.Dependencias;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class PanelBaseHija extends JPanel {

    private static final Color VERDE = new Color(0, 128, 0);
    private static final int MAXIMO_ID = 10;//igual que en PanelBase: el int mas grande de java tiene 10 digitos

    //los tipos de campo que puede tener una tabla hija
    //ID = llave primaria que asigna MySQL,TEXTO = varchar,NUMERO = entero con rango,FECHA = date,MENU = llave foranea
    private enum Tipo { ID, TEXTO, NUMERO, FECHA, MENU }

    //un campo es una columna de la tabla en MySQL junto con la caja o el menu que la edita en el formulario
    private static class Campo {
        Tipo tipo;
        String columna;//nombre de la columna en MySQL,ej. "ID_Estado"
        String etiqueta;//como se llama en el formulario,en la tabla y en los mensajes,ej. "Estado"
        boolean llave;//true si la columna es parte de la llave primaria
        int minimo;//NUMERO: el valor mas chico permitido
        int maximo;//TEXTO: maximo de caracteres,NUMERO: el valor mas grande permitido
        String consulta;//MENU: el SELECT que llena el menu,primero el ID y luego el texto que se ve
        JTextField caja;//para ID,TEXTO,NUMERO y FECHA
        JComboBox<Opcion> menu;//para MENU
    }

    //una opcion del menu desplegable: el ID que se guarda en MySQL y el texto que se ve,ej. "6 - Nuevo León"
    //tambien es lo que se pone en las celdas de la tabla de la pantalla en las columnas de llaves foraneas
    private static class Opcion {
        final int id;
        final String texto;

        Opcion(int id, String texto) {
            this.id = id;
            this.texto = texto;
        }

        //lo que muestran el menu y la tabla
        @Override
        public String toString() {
            return texto;
        }

        //dos opciones son iguales si tienen el mismo ID,asi el menu encuentra la opcion al seleccionar un renglon
        @Override
        public boolean equals(Object otro) {
            return otro instanceof Opcion && ((Opcion) otro).id == id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    private final String tabla;//nombre de la tabla en MySQL,ej. "Ciudad"
    private final List<Campo> campos = new ArrayList<>();//en el orden en que los declara la clase hija
    //mensajes propios para cuando falla una regla UNIQUE o CHECK,por el nombre de la regla,ej. "UQ_Alineacion_Orden"
    private final Map<String, String> avisos = new LinkedHashMap<>();

    private final JTextField txtBuscar = new JTextField(18);
    private final JLabel lblConteo = new JLabel();
    private final JLabel lblMensaje = new JLabel(" ");

    //se crean en armarPantalla() porque las columnas dependen de los campos que declaro la clase hija
    private DefaultTableModel modelo;
    private JTable vista;
    private TableRowSorter<DefaultTableModel> ordenador;

    //los valores de la llave primaria del registro seleccionado,tal como estaban antes de editarlo
    //es lo mismo que idOriginal en PanelBase,pero aqui la llave puede tener varias columnas,ej. Contrato (equipo,patrocinador)
    private Object[] llaveOriginal;

    protected PanelBaseHija(String tabla) {
        this.tabla = tabla;
    }

    // ==================================================================
    // LO QUE USA LA CLASE HIJA PARA DECLARAR SUS CAMPOS
    // ==================================================================

    //llave primaria numerica que MySQL asigna solo (AUTO_INCREMENT),ej. ID_Ciudad
    protected void llaveAutomatica(String columna) {
        Campo c = agregar(Tipo.ID, columna, "ID", true);
        c.caja = new JTextField(18);
        limitar(c.caja, MAXIMO_ID, "[0-9]*");
    }

    //llave foranea normal que se elige con un menu,ej. ID_Estado en Ciudad
    protected void menu(String columna, String etiqueta, String consulta) {
        agregarMenu(columna, etiqueta, consulta, false);
    }

    //llave foranea que ademas es parte de la llave primaria,ej. ID_Equipo y ID_Patrocinador en Contrato
    protected void llaveMenu(String columna, String etiqueta, String consulta) {
        agregarMenu(columna, etiqueta, consulta, true);
    }

    //texto obligatorio con un maximo de caracteres (el tamano del VARCHAR)
    protected void texto(String columna, String etiqueta, int maximo) {
        Campo c = agregar(Tipo.TEXTO, columna, etiqueta, false);
        c.maximo = maximo;
        c.caja = new JTextField(18);
        limitar(c.caja, maximo, null);//null: acepta cualquier caracter
    }

    //numero entero obligatorio entre un minimo y un maximo,ej. el orden al bat del 1 al 9
    protected void numero(String columna, String etiqueta, int minimo, int maximo) {
        Campo c = agregar(Tipo.NUMERO, columna, etiqueta, false);
        c.minimo = minimo;
        c.maximo = maximo;
        c.caja = new JTextField(18);
        limitar(c.caja, String.valueOf(maximo).length(), "[0-9]*");//9 tiene un digito,asi que deja escribir uno
    }

    //fecha obligatoria con el formato de MySQL: AAAA-MM-DD
    protected void fecha(String columna, String etiqueta) {
        Campo c = agregar(Tipo.FECHA, columna, etiqueta, false);
        c.caja = new JTextField(18);
        limitar(c.caja, 10, "[0-9-]*");//"2026-03-15" son 10 caracteres,solo digitos y guiones
    }

    //mensaje que se muestra cuando MySQL rechaza un registro por una regla UNIQUE,CHECK o la llave primaria (PRIMARY)
    protected void avisoRegla(String nombreRegla, String mensaje) {
        avisos.put(nombreRegla, mensaje);
    }

    //reglas propias de cada tabla que no son de un solo campo,ej. que la fecha de fin no sea antes que la de inicio
    //la clase hija la sobreescribe si las necesita,regresa el problema o null si todo esta bien
    protected String reglaExtra() throws SQLException {
        return null;
    }

    //la clase hija lo llama al final de su constructor,cuando ya declaro todos sus campos
    protected void armarPantalla() {
        //una columna por campo y al final quien hizo el ultimo cambio
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

            //las columnas de numeros se ordenan como numeros (1, 2, 10) y no como texto (1, 10, 2)
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

        //cada vez que se abre la pestana se vuelven a leer los menus y la tabla,
        //asi aparece en los menus lo que agregaste en otra pestana (ej. un estado nuevo en el menu de Ciudad)
        addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent e) { cargar(); }
            public void ancestorRemoved(AncestorEvent e) { }
            public void ancestorMoved(AncestorEvent e) { }
        });
    }

    // ==================================================================
    // AYUDAS PARA reglaExtra() DE LA CLASE HIJA
    // ==================================================================

    //el ID de lo que esta elegido en el menu de esa columna,ej. idElegido("ID_Equipo")
    protected Integer idElegido(String columna) {
        Opcion o = (Opcion) buscarCampo(columna).menu.getSelectedItem();
        return o == null ? null : o.id;
    }

    //la fecha escrita en la caja de esa columna (ya se reviso que sea valida)
    protected LocalDate fechaEscrita(String columna) {
        return leerFecha(buscarCampo(columna).caja.getText().trim());
    }

    //true si el SELECT regresa al menos un renglon,los ? se llenan con los valores en orden
    protected boolean existe(String sql, Object... valores) throws SQLException {
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            for (int i = 0; i < valores.length; i++) {
                ps.setObject(i + 1, valores[i]);
            }
            try (ResultSet resultado = ps.executeQuery()) {
                return resultado.next();
            }
        }
    }

    // ==================================================================
    // PANTALLA
    // ==================================================================

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

    //igual que en PanelBase,pero las cajas y menus salen de la lista de campos
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

    //el texto de la etiqueta de cada campo,el * indica que es obligatorio
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
            default:
                return c.etiqueta + " *";
        }
    }

    private JComponent construirTabla() {
        vista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vista.getTableHeader().setReorderingAllowed(false);
        vista.setRowSorter(ordenador);
        vista.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionar();
            }
        });
        return new JScrollPane(vista);
    }

    // ==================================================================
    // CONSULTAS A MYSQL
    // ==================================================================

    //vuelve a llenar los menus con lo que hay en las tablas padre y luego lee los registros de esta tabla
    private void cargar() {
        modelo.setRowCount(0);
        try {
            for (Campo c : campos) {
                if (c.tipo == Tipo.MENU) {
                    llenarMenu(c);
                }
            }
            //ej. SELECT t.ID_Ciudad, t.Nombre_Ciudad, t.ID_Estado, u.Login_Usuario FROM Ciudad t
            //    LEFT JOIN Usuario u ON u.ID_Usuario = t.ID_Usuario ORDER BY t.ID_Ciudad
            String sql = "SELECT " + columnas("t.", false) + ", u.Login_Usuario FROM " + tabla + " t "
                    + "LEFT JOIN Usuario u ON u.ID_Usuario = t.ID_Usuario ORDER BY " + columnas("t.", true);
            try (Statement st = Conexion.obtener().createStatement(); ResultSet resultado = st.executeQuery(sql)) {
                while (resultado.next()) {
                    Object[] fila = new Object[campos.size() + 1];
                    for (int i = 0; i < campos.size(); i++) {
                        fila[i] = leer(resultado, i + 1, campos.get(i));
                    }
                    fila[campos.size()] = resultado.getString(campos.size() + 1);//null si nadie lo ha cambiado
                    modelo.addRow(fila);
                }
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
        actualizarConteo();
    }

    //llena un menu con el SELECT que dio la clase hija,cada renglon es una opcion "ID - texto"
    //si ya habia algo elegido lo vuelve a elegir,siempre que todavia exista
    private void llenarMenu(Campo c) throws SQLException {
        Object elegida = c.menu.getSelectedItem();
        c.menu.removeAllItems();
        try (Statement st = Conexion.obtener().createStatement(); ResultSet resultado = st.executeQuery(c.consulta)) {
            while (resultado.next()) {
                int id = resultado.getInt(1);
                String texto = resultado.getString(2);
                c.menu.addItem(new Opcion(id, id + " - " + (texto == null ? "(sin nombre)" : texto)));
            }
        }
        //al agregar la primera opcion el menu la elige solo,asi que primero se deja sin nada elegido
        c.menu.setSelectedIndex(-1);
        c.menu.setSelectedItem(elegida);//si ya no existe no elige nada
    }

    //convierte el valor que vino de MySQL en lo que se pone en la celda de la tabla
    private Object leer(ResultSet resultado, int posicion, Campo c) throws SQLException {
        if (c.tipo == Tipo.ID || c.tipo == Tipo.NUMERO || c.tipo == Tipo.MENU) {
            int numero = resultado.getInt(posicion);
            if (resultado.wasNull()) {
                return null;
            }
            //en las llaves foraneas se pone la opcion del menu con ese ID,que ya trae el texto que se ve
            return c.tipo == Tipo.MENU ? buscarOpcion(c, numero) : numero;
        }
        return resultado.getString(posicion);//TEXTO y FECHA,la fecha llega como "2026-03-15"
    }

    private void insertar() {
        if (!validar(false)) {
            return;
        }
        //ej. INSERT INTO Ciudad (ID_Ciudad, Nombre_Ciudad, ID_Estado, ID_Usuario) VALUES (?, ?, ?, ?)
        String sql = "INSERT INTO " + tabla + " (" + columnas("", false) + ", ID_Usuario) VALUES (" + signos(campos.size() + 1) + ")";
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps);
            ps.executeUpdate();
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
        //ej. UPDATE Contrato SET ID_Equipo = ?, ID_Patrocinador = ?, Inicio_Contrato = ?, Fin_Contrato = ?, ID_Usuario = ?
        //    WHERE ID_Equipo = ? AND ID_Patrocinador = ?
        //el WHERE usa la llave original,por si en el formulario cambiaste un dato de la llave
        String sql = "UPDATE " + tabla + " SET " + asignaciones() + ", ID_Usuario = ? WHERE " + condicionLlave();
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps);
            int hueco = campos.size() + 2;//el primer ? del WHERE
            for (Object valor : llaveOriginal) {
                ps.setObject(hueco++, valor);
            }
            int filas = ps.executeUpdate();
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
        //antes de preguntar,revisa si otras tablas tienen registros ligados a este por llave foranea
        //ej. una ciudad que ya tiene equipos o estadios: MySQL no la deja borrar (ON DELETE RESTRICT)
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
            //si no se pudo revisar se sigue: MySQL de todos modos no deja borrar un registro con hijos y traducir() lo explica
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Seguro que deseas eliminar este registro?",
                "Eliminar registro", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try (PreparedStatement ps = Conexion.obtener().prepareStatement("DELETE FROM " + tabla + " WHERE " + condicionLlave())) {
            for (int i = 0; i < llaveOriginal.length; i++) {
                ps.setObject(i + 1, llaveOriginal[i]);
            }
            if (ps.executeUpdate() > 0) {
                Bitacora.registrar(tabla, "ELIMINAR", describir());
            }
            cargar();
            limpiar();
            exito("Registro eliminado por " + Sesion.getLogin() + ".");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    //toma lo que hay en el formulario y lo pone en los huecos ?,en el mismo orden que los campos,y al final el usuario
    private void asignar(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < campos.size(); i++) {
            Campo c = campos.get(i);
            int hueco = i + 1;
            String texto = c.caja == null ? null : c.caja.getText().trim();
            switch (c.tipo) {
                case ID:
                    if (texto.isEmpty()) {
                        ps.setNull(hueco, Types.INTEGER);//MySQL asigna el numero
                    } else {
                        ps.setInt(hueco, Integer.parseInt(texto));
                    }
                    break;
                case MENU:
                    ps.setInt(hueco, ((Opcion) c.menu.getSelectedItem()).id);//se guarda el ID,no el texto
                    break;
                case NUMERO:
                    ps.setInt(hueco, Integer.parseInt(texto));
                    break;
                case FECHA:
                    ps.setString(hueco, leerFecha(texto).toString());//MySQL recibe "2026-03-15"
                    break;
                default:
                    ps.setString(hueco, texto);
            }
        }
        ps.setInt(campos.size() + 1, Sesion.getIdUsuario());//el id del usuario que inicio sesion
    }

    //cuando MySQL rechaza un alta o un cambio
    private void fallo(SQLException e) {
        //1216 o 1452: lo elegido en un menu ya no existe (alguien lo borro despues de abrir la pestana)
        //se recargan los menus para que ya no aparezca
        if (e.getErrorCode() == 1216 || e.getErrorCode() == 1452) {
            cargar();
        }
        error(traducir(e));
    }

    // ==================================================================
    // VALIDACION Y COMPORTAMIENTO DE LA PANTALLA
    // ==================================================================

    //revisa los campos en orden y luego las reglas propias de la tabla
    //en cuanto encuentra el primer problema lo muestra en rojo y pone el cursor en ese campo
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

    //revisa un campo,regresa el problema que tiene o null si esta bien
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
            default:
                return null;
        }
    }

    //true si el texto es un numero entero entre minimo y maximo
    private static boolean esNumero(String texto, long minimo, long maximo) {
        try {
            long n = Long.parseLong(texto);//long para que un numero de 10 digitos no se desborde
            return n >= minimo && n <= maximo;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    //convierte "2026-03-15" en fecha,o regresa null si no es una fecha real (ej. 2026-02-30)
    //el año debe tener 4 digitos,el rango que acepta el tipo DATE de MySQL
    private static LocalDate leerFecha(String texto) {
        try {
            LocalDate fecha = LocalDate.parse(texto);
            return fecha.getYear() >= 1000 ? fecha : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    //igual que limitar() de PanelBase,pero en vez de "solo numeros si o no" recibe los caracteres permitidos:
    //null = cualquiera,"[0-9]*" = solo digitos,"[0-9-]*" = digitos y guion (para las fechas)
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

    //pone en el formulario los datos del renglon seleccionado y guarda su llave original
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
                c.menu.setSelectedItem(valor);//la busca por ID (equals de Opcion)
            } else {
                c.caja.setText(valor == null ? "" : valor.toString());
            }
            if (c.llave) {
                //de una llave foranea se guarda su ID,no su texto
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
    // PEQUENAS AYUDAS
    // ==================================================================

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
        //el ancho del menu no depende del texto mas largo,asi no se sale del formulario
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

    //la opcion del menu que tiene ese ID,si no esta (no deberia pasar) se muestra solo el numero
    private static Opcion buscarOpcion(Campo c, int id) {
        for (int i = 0; i < c.menu.getItemCount(); i++) {
            if (c.menu.getItemAt(i).id == id) {
                return c.menu.getItemAt(i);
            }
        }
        return new Opcion(id, String.valueOf(id));
    }

    //"t.ID_Ciudad, t.Nombre_Ciudad, t.ID_Estado",o solo las de la llave si soloLlave es true
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

    //"ID_Equipo = ?, ID_Patrocinador = ?, Inicio_Contrato = ?, Fin_Contrato = ?"
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

    //"ID_Equipo = ? AND ID_Patrocinador = ?"
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

    //"?, ?, ?, ?"
    private static String signos(int cuantos) {
        StringBuilder sb = new StringBuilder("?");
        for (int i = 1; i < cuantos; i++) {
            sb.append(", ?");
        }
        return sb.toString();
    }

    //texto corto para la bitacora,ej. "Equipo: 2, Patrocinador: 3, Inicio del contrato: 2026-01-01"
    //de las llaves foraneas se anota el ID,la columna Detalle de Bitacora admite 100 caracteres
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

    // ==================================================================
    // MENSAJES
    // ==================================================================

    private void exito(String texto) {
        lblMensaje.setForeground(VERDE);
        lblMensaje.setText("<html>" + texto + "</html>");
    }

    private void error(String texto) {
        lblMensaje.setForeground(Color.RED);
        lblMensaje.setText("<html>" + texto + "</html>");
    }

    /** Convierte los errores de MySQL en mensajes entendibles. */
    private String traducir(SQLException e) {
        String detalle = e.getMessage() == null ? "" : e.getMessage();
        //1062 = valor repetido (llave primaria o UNIQUE),3819 = no se cumple un CHECK
        //si la clase hija dio un mensaje para esa regla,se usa ese
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
            //el usuario APP_LIGA recibe 1216 y 1217,root recibe 1452 y 1451: son los mismos errores de llave foranea
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
