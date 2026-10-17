//esta es la clase BASE de todas las pantallas de tablas (Estado, Posicion, Patrocinador...)
//aqui esta todo lo que es igual en todas: el formulario, la tabla, el buscador, las validaciones, el CRUD y los mensajes
//cada tabla solo crea una clase hija que le dice a esta como se llama su tabla y sus columnas
//abstract: esta clase no se usa sola,solo sirve para que otras hereden de ella

package vista.tablas;

import conexion.Conexion;
import modelo.Sesion;
import modelo.Bitacora;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AbstractDocument;//el tipo de cuaderno que acepta un filtro
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;//el portero
import javax.swing.text.StyleConstants;

import java.awt.*;
import java.sql.*;
import java.util.regex.Pattern;

public abstract class PanelBase extends JPanel {

    private static final Color VERDE = new Color(0, 128, 0);//color de los mensajes de exito
    //cuantos digitos deja escribir la caja del id: el id se lee con Integer.parseInt
    //y el int mas grande de java es 2147483647,que tiene 10 digitos
    private static final int MAXIMO_ID = 10;

    //datos propios de cada tabla,los manda la clase hija en el constructor
    private final String tabla;//nombre de la tabla en MySQL,ej. "Estado"
    private final String idColumna;//columna de la llave primaria,ej. "ID_Estado"
    private final String nombreColumna;//columna del nombre,ej. "Nombre_Estado"
    private final String tituloNombre;//como se llama la columna y la etiqueta del nombre,ej. "Nombre del estado"
    private final int maximoNombre;//el tamano del VARCHAR del nombre,ej. 20

    private final JTextField txtId = new JTextField(18);
    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtBuscar = new JTextField(18);//caja de busqueda,lo que escribas se usa para filtrar con el ordenador
    private final JLabel lblConteo = new JLabel();
    private final JLabel lblMensaje = new JLabel(" ");

    //estos tres se crean en el constructor porque el titulo de la columna del nombre depende de la tabla
    private final DefaultTableModel modelo;//guarda los datos de la tabla filas y columnas que vienen de la bd
    private final JTable vista;//el componente visual que muestra ese modelo
    private final TableRowSorter<DefaultTableModel> ordenador;//permite ordenar con los encabezados y filtrar filas

    //guarda el id del registro que estoy editando tal como estaba antes de editarlo
    //hace falta porque si seleccionas el id 5 y lo cambias a 7 en el formulario,el UPDATE tiene que buscar la fila con el 5
    private Integer idOriginal;

    //la clase hija llama a este constructor con super(...) y le pasa los datos de su tabla
    protected PanelBase(String tabla, String idColumna, String nombreColumna, String tituloNombre, int maximoNombre) {
        this.tabla = tabla;
        this.idColumna = idColumna;
        this.nombreColumna = nombreColumna;
        this.tituloNombre = tituloNombre;
        this.maximoNombre = maximoNombre;

        modelo = new DefaultTableModel(new String[]{"ID", tituloNombre, "Último cambio por"}, 0) {
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
        vista = new JTable(modelo);
        ordenador = new TableRowSorter<>(modelo);

        setLayout(new BorderLayout(18, 14));
        setBorder(new EmptyBorder(18, 20, 18, 20));//para que nada quede pegado al borde

        add(construirEncabezado(), BorderLayout.NORTH);
        add(construirFormulario(), BorderLayout.WEST);
        add(construirTabla(), BorderLayout.CENTER);
        cargar();//hace el select y llena el modelo de la tabla
    }

    // ==================================================================
    // PANTALLA
    // ==================================================================

    //conteo a la izquierda y buscador a la derecha
    private JComponent construirEncabezado() {
        JPanel enc = new JPanel(new BorderLayout());
        enc.add(lblConteo, BorderLayout.WEST);
        JPanel buscar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buscar.add(new JLabel("Buscar:"));
        //buscar mientras escribes: el documentlistener avisa cada vez que cambia el texto de la caja
        //insertUpdate se escribio o pego texto,removeUpdate se borro texto,changedUpdate cambio el formato
        //los tres llaman a filtrar() asi que la tabla se filtra en tiempo real con cada tecla
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        buscar.add(txtBuscar);
        enc.add(buscar, BorderLayout.EAST);
        return enc;
    }

    //el formulario del panel,el recuadro donde escribes los datos de un registro
    private JComponent construirFormulario() {
        JPanel form = new JPanel(new GridBagLayout());//cuadricula donde a cada componente le das reglas individuales
        form.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Datos del registro"), new EmptyBorder(8, 10, 8, 10)));

        //g es una "hoja de instrucciones" que se reutiliza en cada add,cambiando solo lo necesario
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;//columna fija + fila relativa:cada componente queda debajo del anterior
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        //al llegar al maximo las cajas ya no dejan escribir ni pegar mas caracteres
        limitar(txtId, MAXIMO_ID, true);//a la caja del id ponle un limite de 10 y que solo acepte numeros
        limitar(txtNombre, maximoNombre, false);//false: el nombre acepta cualquier caracter

        g.insets = new Insets(0, 0, 4, 0);//4 pixeles abajo para que la etiqueta quede pegada a su caja
        form.add(new JLabel("ID (vacío = se asigna solo)"), g);
        g.insets = new Insets(0, 0, 12, 0);
        form.add(txtId, g);
        g.insets = new Insets(0, 0, 4, 0);
        form.add(new JLabel(tituloNombre + " * (máximo " + maximoNombre + ")"), g);
        g.insets = new Insets(0, 0, 12, 0);
        form.add(txtNombre, g);

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

    //lee todos los registros de la tabla de MySQL y los pone en la tabla de la pantalla
    private void cargar() {
        String sql = "SELECT e." + idColumna + ", e." + nombreColumna + ", u.Login_Usuario FROM " + tabla + " e "
                + "LEFT JOIN Usuario u ON u.ID_Usuario = e.ID_Usuario ORDER BY e." + idColumna;
        modelo.setRowCount(0);//poner los renglones en 0 borra todo lo que tenia
        try (Statement st = Conexion.obtener().createStatement(); ResultSet resultado = st.executeQuery(sql)) {
            while (resultado.next()) {
                //si el left join no encontro usuario,regresa null y la celda sale vacia
                modelo.addRow(new Object[]{resultado.getInt(1), resultado.getString(2), resultado.getString(3)});
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
        actualizarConteo();
    }

    private void insertar() {
        //si algo esta mal,validar muestra el mensaje en rojo y regresa false,asi nunca se manda a mysql un dato invalido
        if (!validar(false)) {
            return;
        }
        String sql = "INSERT INTO " + tabla + " (" + idColumna + ", " + nombreColumna + ", ID_Usuario) VALUES (?, ?, ?)";
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps);//llena los tres ?
            ps.executeUpdate();//se guarda en mysql
            Bitacora.registrar(tabla, "INSERTAR", txtNombre.getText().trim());
            cargar();//vuelve a hacer select para que el registro nuevo aparezca con su id y ultimo cambio por
            limpiar();
            exito("Registro agregado por " + Sesion.getLogin() + ".");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    private void actualizar() {
        //si idOriginal es null no has elegido ningun registro
        if (idOriginal == null) {
            error("Selecciona un registro de la tabla.");
            return;
        }
        if (!validar(true)) {
            return;
        }
        //? 1 id nuevo,? 2 nombre,? 3 quien hizo el cambio,? 4 cual renglon cambiar
        String sql = "UPDATE " + tabla + " SET " + idColumna + " = ?, " + nombreColumna + " = ?, ID_Usuario = ? WHERE " + idColumna + " = ?";
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps);
            ps.setInt(4, idOriginal);//llena el cuarto hueco a mano
            //1 encontro el registro y lo cambio,0 no existe ninguno con ese id
            int filas = ps.executeUpdate();
            if (filas > 0) {
                Bitacora.registrar(tabla, "ACTUALIZAR", "ID " + idOriginal + " - " + txtNombre.getText().trim());
            }
            cargar();
            limpiar();
            if (filas == 0) {
                error("Ese registro ya no existe.");
            } else {
                exito("Registro actualizado por " + Sesion.getLogin() + ".");
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    private void eliminar() {
        if (idOriginal == null) {
            error("Selecciona un registro de la tabla.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Seguro que deseas eliminar este registro?",
                "Eliminar registro", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try (PreparedStatement ps = Conexion.obtener().prepareStatement("DELETE FROM " + tabla + " WHERE " + idColumna + " = ?")) {
            ps.setInt(1, idOriginal);
            if (ps.executeUpdate() > 0) {
                //el registro ya no existe en la tabla,por eso queda anotado en la bitacora quien lo borro
                Bitacora.registrar(tabla, "ELIMINAR", "ID " + idOriginal + " - " + txtNombre.getText().trim());
            }
            cargar();
            limpiar();
            exito("Registro eliminado por " + Sesion.getLogin() + ".");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    //toma lo que escribiste en el formulario y lo pone en los huecos ?
    private void asignar(PreparedStatement ps) throws SQLException {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            ps.setNull(1, Types.NULL); // MySQL asigna el número
        } else {
            ps.setInt(1, Integer.parseInt(id));
        }
        ps.setString(2, txtNombre.getText().trim());
        ps.setInt(3, Sesion.getIdUsuario());//el id del usuario que inicio sesion
    }

    // ==================================================================
    // VALIDACION Y COMPORTAMIENTO DE LA PANTALLA
    // ==================================================================

    //revisa primero el id y despues el nombre,en cuanto encuentra el primer problema lo guarda junto con la caja donde esta
    //y al final muestra el mensaje y pone el cursor en esa caja
    private boolean validar(boolean actualizando) {
        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        String problema = null;//null significa "todavia no hay ningun problema"
        JTextField caja = txtId;//la caja donde esta el problema,empieza en el id porque es lo primero que se revisa

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
                problema = "El ID debe ser un número entre 1 y 2147483647.";
            }
        }
        //revisa el nombre solo si el id estuvo bien
        if (problema == null) {
            caja = txtNombre;
            if (nombre.isEmpty()) {
                problema = "Escribe el nombre.";
            } else if (nombre.length() > maximoNombre) {
                problema = "El nombre admite máximo " + maximoNombre + " caracteres.";
            }
        }
        if (problema != null) {
            error(problema);
            caja.requestFocusInWindow();
        }
        return problema == null;
    }


    //antes la caja dejaba entrar todo y la revision pasaba hasta que das clic
    //ahora cada caja tiene un portero,que ahora lo revisa
    //en java ese portero se llama documentfilter.un jtextField no guarda el texto el mismo
    //lo guarda en un objeto llamado document,como un cuaderno detras de la caja
    //todo lo que entra a la caja pasa por ese cuaderni y si el cuaderno tiene un filtro se ejecuta antes
 //recibe 3 cosas :la caja el maximo de caracteres y si solo acepta numeros(true o false)
    private static void limitar(JTextField caja, int maximo, boolean soloNumeros) {
        //saca el cuaderno de la caja.crea el portero y se lo pone al cuaderno,es una clase anonima
        ((AbstractDocument) caja.getDocument()).setDocumentFilter(new DocumentFilter() {
            //se llama al escribir una tecla,al pegar y con setText
            //borrados: cuantos caracteres se van a reemplazar (si seleccionas texto y escribes encima)
            @Override
            //fb=la puerta de atras para escribir de verdad en el cuaderno sin volver a pasar por el portero
            //inicio=la posicion donde va a entrar el texto,0 es el principio
            //borrados=cuantos caracteres se van a reemplazar
            //texto=lo que quiere entrar
            //atributos=informacion extra(acento)
            public void replace(FilterBypass fb, int inicio, int borrados, String texto, AttributeSet atributos) throws BadLocationException {
                //ESTO ES POR LOS ACENTOS DE LA MAC
                boolean temporal = atributos != null && atributos.isDefined(StyleConstants.ComposedTextAttribute);
                if (!temporal && texto != null) {
                    //[0-9]* significa "puros digitos del 0 al 9",si trae una letra,espacio o signo no entra nada
                    if (soloNumeros && !texto.matches("[0-9]*")) {
                        return;
                    }
                    int caben = maximo - (fb.getDocument().getLength() - borrados);//espacio que queda libre en la caja
                    if (texto.length() > caben) {
                        texto = texto.substring(0, Math.max(caben, 0));//corta lo que sobra
                    }
                }
                super.replace(fb, inicio, borrados, texto, atributos);
            }

            //la otra forma de meter texto a la caja,hace lo mismo que replace pero sin reemplazar nada
            @Override
            public void insertString(FilterBypass fb, int inicio, String texto, AttributeSet atributos) throws BadLocationException {
                replace(fb, inicio, 0, texto, atributos);
            }
        });
    }

    //se ejecuta cuando el usuario selecciona una fila de la tabla,pone sus datos en el formulario para editarlos
    private void seleccionar() {
        int fila = vista.getSelectedRow();//si no hay ninguna seleccionada devuelve -1
        if (fila < 0) {
            return;
        }
        int f = vista.convertRowIndexToModel(fila);//indice real en el modelo,por si la tabla esta ordenada o filtrada
        idOriginal = (Integer) modelo.getValueAt(f, 0);
        txtId.setText(idOriginal.toString());
        txtNombre.setText((String) modelo.getValueAt(f, 1));
        lblMensaje.setText(" ");
    }

    private void limpiar() {
        txtId.setText("");
        txtNombre.setText("");
        vista.clearSelection();
        idOriginal = null;//con null indicamos que no hay ningun registro en edicion
    }

    private void filtrar() {
        String texto = txtBuscar.getText().trim();
        //si el campo esta vacio,null quita el filtro y muestra todas las filas
        //(?i) ignora mayusculas y Pattern.quote toma el texto de forma literal
        ordenador.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
        actualizarConteo();
    }

    //actualiza la etiqueta que muestra cuantos registros hay y cuantos se ven cuando hay un filtro activo
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
                return "Ya existe un registro con ese ID.";
            case 1451:
                return "No se puede: hay registros en otras tablas que dependen de este registro.";
            case 1406:
                return "Un texto es más largo de lo permitido.";
            case 1142:
                return "El usuario de la aplicación no tiene permiso para esta operación.";
            case 1054:
                return "Falta la columna ID_Usuario en la tabla " + tabla + ". Ejecuta el script usuarios_finales.sql.";
            case 1146:
                return "Falta una tabla (" + tabla + " o Bitacora). Ejecuta el script usuarios_finales.sql.";
            default:
                return "Error de MySQL " + e.getErrorCode() + ": " + e.getMessage();
        }
    }
}
