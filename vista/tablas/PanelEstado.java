//este codigo crea la pantalla para manejar la tabla estado de la base de datos
//aqui mismo estan el formulario, la tabla, el buscador, las validaciones y las consultas a MySQL

package vista.tablas;

//Conexion: da la conexion abierta a MySQL con Conexion.obtener(),se usa para hacer los SELECT, INSERT, UPDATE y DELETE
import conexion.Conexion;
//Sesion: guarda quien inicio sesion,aqui se usa getIdUsuario() para dejar el id en el registro y getLogin() para el mensaje de exito
import modelo.Sesion;

//el * importa todas las clases de javax.swing: JPanel, JLabel, JButton, JTextField, JTable, JComboBox,
//JScrollPane, JOptionPane, JComponent, RowFilter, SwingUtilities, etc.
import javax.swing.*;
//CompoundBorder: junta dos bordes en uno (aqui una linea por fuera + espacio vacio por dentro en el formulario)
import javax.swing.border.CompoundBorder;
//EmptyBorder: borde invisible que solo deja margen (espacio) alrededor de un componente
import javax.swing.border.EmptyBorder;
//DocumentEvent: el "aviso" que manda una caja de texto cuando su contenido cambia (se escribio o se borro algo)
import javax.swing.event.DocumentEvent;
//DocumentListener: el "escuchador" que recibe esos avisos,se usa en el buscador para filtrar la tabla en cada tecla que escribes
import javax.swing.event.DocumentListener;
//DefaultTableModel: es donde se GUARDAN los datos de la tabla (renglones y columnas),la JTable solo los muestra
import javax.swing.table.DefaultTableModel;
//TableRowSorter: permite ordenar la tabla dando clic en el encabezado y FILTRAR renglones (lo usa el buscador)
import javax.swing.table.TableRowSorter;

//el * importa BorderLayout, FlowLayout, GridBagLayout, GridBagConstraints, Insets, Dimension, Color, Font, Component...
import java.awt.*;
//el * importa todo JDBC: Statement (consulta sin huecos), PreparedStatement (consulta con ? que se llenan despues),
//ResultSet (los renglones que devuelve un SELECT), SQLException (error de base de datos) y Types (Types.NULL para mandar un NULL)
import java.sql.*;
//Pattern: expresiones regulares; Pattern.quote() hace que lo que escribes en el buscador se busque como texto literal
//(asi un punto o un parentesis no se interpretan como simbolos especiales de regex)
import java.util.regex.Pattern;

public class PanelEstado extends JPanel {

    private static final int MAXIMO_NOMBRE = 20;//el 20 de Nombre_Estado VARCHAR(20)
    private static final Color VERDE = new Color(0, 128, 0);//color de los mensajes de exito

    private final JTextField txtId = new JTextField(18);
    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtBuscar = new JTextField(18);//caja de texxto de busqueda,lo que escribas se usa para filtrar con el ordenador
    private final JLabel lblConteo = new JLabel();
    private final JLabel lblMensaje = new JLabel(" ");

    //guarda los datos de la tabla filas y columnas que vienen de la bd
    private final DefaultTableModel modelo = new DefaultTableModel(new String[]{"ID", "Nombre del estado", "Último cambio por"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }

        //la columna 0 (ID) es numero,asi al dar clic en su encabezado ordena 1, 2, 10 y no 1, 10, 2
        @Override
        public Class<?> getColumnClass(int columna) {
            return columna == 0 ? Integer.class : String.class;
        }
    };
    private final JTable vista = new JTable(modelo);//el componente visual que muestra ese modeo
    private final TableRowSorter<DefaultTableModel> ordenador = new TableRowSorter<>(modelo);//permite ordenar al hacer clic en los encabezados y filtrar filas

    //guarda los valores de la llave primaria del registro que estoy editando tal como estaban antes de editarlo
    //hace falta porque si el usuario selecciona el estado con ID y y cambia el iD a 7 en el formulario,el UPDATE tiene que buscar la fila
    private Integer idOriginal;

    //arman la parte visual del panel
    public PanelEstado() {
        setLayout(new BorderLayout(18, 14));
        setBorder(new EmptyBorder(18, 20, 18, 20));//para que nada qued pegado al borde

        add(construirEncabezado(), BorderLayout.NORTH);
        add(construirFormulario(), BorderLayout.WEST);
        add(construirTabla(), BorderLayout.CENTER);
        cargar();//vuelve a hacer el select y llena el modelo de la tabla
    }

    //para poner los titulos a la izquierda y la busqueda a la derecha
    private JComponent construirEncabezado() {
        JPanel enc = new JPanel(new BorderLayout());
        enc.add(lblConteo, BorderLayout.WEST);
        //otra fila alineada a la derecha
        JPanel buscar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buscar.add(new JLabel("Buscar:"));
        //buscar mientras escribes
        //el texto de un JTextField vive en un document y documentlistener es un escuchador que avisa cada vez que ese texto cambis
        //se crea con clase anonima;implementa la interfaz ahi mismo,sin crear un archivo aparte
        //la interfaz obliga a implementar los 3 metodos
        //insertUpdate se escribio un caracter o se pego texto
        //removeUpdate se borro texto
        //changedUpdate cambio el formato del texto
        //los tres llaman a filtrar() asi que la tabla se filtra en tiempo real con cada tecla
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

    //este metodo arma el fromulario del panel,el recuadro donde escribes los datos de un estado
    private JComponent construirFormulario() {
        //crea el panel contenedor y le asigna el layout
        //un layout es el acomodador descide donde y de que tamano va cada componente
        JPanel form = new JPanel(new GridBagLayout());//trabaja como una cuadricula donde a cada compomente le das reglas individuales
        //le pone un borde compuedsto,exterior e interior
        form.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Datos del estado"), new EmptyBorder(8, 10, 8, 10)));

        //Este objeto g es una "hoja de instrucciones" que se le pasa a cada componente al agregarlo. Se reutiliza el mismo objeto, cambiando solo lo necesario antes de cada add.
        GridBagConstraints g = new GridBagConstraints();
        //La combinación "columna fija + fila relativa" hace que cada componente nuevo se coloque debajo del anterior. Así se logra que todo quede apilado verticalmente sin tener que llevar la cuenta de filas a mano.
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        //la etiqueta del campo
        //Insets son márgenes externos del componente (arriba, izquierda, abajo, derecha). Aquí: 4 píxeles de espacio abajo, para que la etiqueta quede pegada a su caja.
        g.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel("ID (vacío = se asigna solo)"), g);
        //el control donde el usuario escribe o elige
        g.insets = new Insets(0, 0, 12, 0);
        form.add(txtId, g);
        g.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel("Nombre del estado * (máximo " + MAXIMO_NOMBRE + ")"), g);
        g.insets = new Insets(0, 0, 12, 0);
        form.add(txtNombre, g);

        //despues de los campos agrega los botones de accion
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
        form.add(botones, g);

        //la etiqueta de mensajes va al final y se queda con todo el espacio que sobra hacia abajo (weighty = 1)
        //asi todo lo demas queda arriba y un mensaje largo tiene lugar para partirse en varios renglones
        lblMensaje.setVerticalAlignment(SwingConstants.TOP);
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        form.add(lblMensaje, g);

        //ancho fijo de 300 para que el formulario no cambie de tamano cuando aparece un mensaje largo
        form.setPreferredSize(new Dimension(300, form.getPreferredSize().height));
        return form;
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

    /** Trae todos los registros de la tabla junto con el usuario que hizo el último cambio. */
    private void cargar() {
        String sql = "SELECT e.ID_Estado, e.Nombre_Estado, u.Login_Usuario FROM Estado e "
                + "LEFT JOIN Usuario u ON u.ID_Usuario = e.ID_Usuario ORDER BY e.ID_Estado";
        modelo.setRowCount(0);
        try (Statement st = Conexion.obtener().createStatement(); ResultSet resultado = st.executeQuery(sql)) {
            while (resultado.next()) {
                modelo.addRow(new Object[]{resultado.getInt(1), resultado.getString(2), resultado.getString(3)});
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
        actualizarConteo();
    }

    private void insertar() {
        if (!validar(false)) {
            return;
        }
        String sql = "INSERT INTO Estado (ID_Estado, Nombre_Estado, ID_Usuario) VALUES (?, ?, ?)";
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps);
            ps.executeUpdate();
            cargar();
            limpiar();
            exito("Estado registrado por " + Sesion.getLogin() + ".");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    private void actualizar() {
        if (idOriginal == null) {
            error("Selecciona un estado de la tabla.");
            return;
        }
        if (!validar(true)) {
            return;
        }
        String sql = "UPDATE Estado SET ID_Estado = ?, Nombre_Estado = ?, ID_Usuario = ? WHERE ID_Estado = ?";
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps);
            ps.setInt(4, idOriginal);
            int filas = ps.executeUpdate();
            cargar();
            limpiar();
            if (filas == 0) {
                error("Ese estado ya no existe.");
            } else {
                exito("Estado actualizado por " + Sesion.getLogin() + ".");
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    private void eliminar() {
        if (idOriginal == null) {
            error("Selecciona un estado de la tabla.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Seguro que deseas eliminar este estado?",
                "Eliminar estado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try (PreparedStatement ps = Conexion.obtener().prepareStatement("DELETE FROM Estado WHERE ID_Estado = ?")) {
            ps.setInt(1, idOriginal);
            ps.executeUpdate();
            cargar();
            limpiar();
            exito("Estado eliminado.");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    /** Pone los valores del formulario (ya validados) en los tres primeros ? del PreparedStatement. */
    private void asignar(PreparedStatement ps) throws SQLException {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            ps.setNull(1, Types.NULL); // MySQL asigna el número
        } else {
            ps.setInt(1, Integer.parseInt(id));
        }
        ps.setString(2, txtNombre.getText().trim());
        ps.setInt(3, Sesion.getIdUsuario());
    }

    // ==================================================================
    // FORMULARIO
    // ==================================================================

    /** Valida lo que hay en el formulario. Si algo está mal avisa, pone el cursor en esa caja y devuelve false. */
    private boolean validar(boolean actualizando) {
        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        String problema = null;
        JTextField caja = txtId;

        if (id.isEmpty()) {
            if (actualizando) {
                problema = "El ID no puede quedar vacío al actualizar.";
            }
        } else {
            try {
                if (Integer.parseInt(id) <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                problema = "El ID debe ser un número entero mayor que 0.";
            }
        }
        if (problema == null) {
            caja = txtNombre;
            if (nombre.isEmpty()) {
                problema = "Escribe el nombre del estado.";
            } else if (nombre.length() > MAXIMO_NOMBRE) {
                problema = "El nombre del estado admite máximo " + MAXIMO_NOMBRE + " caracteres.";
            }
        }
        if (problema != null) {
            error(problema);
            caja.requestFocusInWindow();
        }
        return problema == null;
    }

    /** Pasa la fila elegida en la tabla al formulario. */
    private void seleccionar() {
        int fila = vista.getSelectedRow();
        if (fila < 0) {
            return;
        }
        int f = vista.convertRowIndexToModel(fila);
        idOriginal = (Integer) modelo.getValueAt(f, 0);
        txtId.setText(idOriginal.toString());
        txtNombre.setText((String) modelo.getValueAt(f, 1));
        lblMensaje.setText(" ");
    }

    private void limpiar() {
        txtId.setText("");
        txtNombre.setText("");
        vista.clearSelection();
        idOriginal = null;
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
        lblMensaje.setForeground(VERDE);
        lblMensaje.setText("<html>" + texto + "</html>");
    }

    private void error(String texto) {
        lblMensaje.setForeground(Color.RED);
        lblMensaje.setText("<html>" + texto + "</html>");
    }

    /** Convierte los errores de MySQL en mensajes entendibles. */
    private String traducir(SQLException e) {
        switch (e.getErrorCode()) {
            case 1062:
                return "Ya existe un estado con ese ID.";
            case 1451:
                return "No se puede: hay registros en otras tablas que dependen de este estado.";
            case 1406:
                return "Un texto es más largo de lo permitido.";
            case 1142:
                return "El usuario de la aplicación no tiene permiso para esta operación.";
            case 1054:
                return "Falta la columna ID_Usuario en la tabla Estado. Ejecuta el script usuarios_finales.sql.";
            case 1146:
                return "No existe la tabla Estado.";
            default:
                return "Error de MySQL " + e.getErrorCode() + ": " + e.getMessage();
        }
    }
}


//en resumen dice :quiero una pantalla para la tabla estado que tiene un id y un nombre de hasta 20 caracteres
