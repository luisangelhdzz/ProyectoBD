//este codigo crea la pantalla para manejar la tabla estado de la base de datos
//aqui mismo estan el formulario, la tabla, el buscador, las validaciones y las consultas a MySQL

package vista.tablas;

//Conexion: da la conexion abierta a MySQL con Conexion.obtener(),se usa para hacer los SELECT, INSERT, UPDATE y DELETE
import conexion.Conexion;
//Sesion: guarda quien inicio sesion,aqui se usa getIdUsuario() para dejar el id en el registro y getLogin() para el mensaje de exito
import modelo.Sesion;
//Bitacora: guarda quien hizo cada movimiento (tambien las bajas, que ya no dejan renglon en Estado)
import modelo.Bitacora;

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
    //hace falta porque si el usuario selecciona el estado con ID 5 y cambia el ID a 7 en el formulario,el UPDATE tiene que buscar la fila con el 5
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



    //lee todos los estados de mySQL y los pone en la tabla de la pantalla
    private void cargar() {
        String sql = "SELECT e.ID_Estado, e.Nombre_Estado, u.Login_Usuario FROM Estado e "
                + "LEFT JOIN Usuario u ON u.ID_Usuario = e.ID_Usuario ORDER BY e.ID_Estado";
        modelo.setRowCount(0);//es el defaultTableModel donde se guardan los datos que la JTable muestra
        //poner los renglones en 0 borra todo lo que tenia
        //te da la conexion abierta a mysql con el usuario app_Liga
        //crea un statment que es el objeto que manda sql a mysql
        //manda el select y regresa un resultset,los renglones de la tabla de arriba que se leen uno por uno
        try (Statement st = Conexion.obtener().createStatement(); ResultSet resultado = st.executeQuery(sql)) {
            //el resultSet tiene un cursor que empieza antes del primer renglon cada next avanza al siguiente renglon y regresa
            //true si habia renglon y entonces entra al while
            //false cuando ya no hay mas y el ciclo ter,ina
            while (resultado.next()) {
                //lee la columna 1 del renglon actual como numero
                //lee nombre_Estado como texto
                //si el left join no encontro usuario,regresa null y la celda sale vacia
                //arma un arreglo con esos tres valores,es object porque mezcla un numero con textos
                //agrega ese arreglo como renglon nuevo
                modelo.addRow(new Object[]{resultado.getInt(1), resultado.getString(2), resultado.getString(3)});
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
        actualizarConteo();
    }

    private void insertar() {
        //validar las cajas del formulario\
        //si algo esta mal,validar muestra el mensaje ene rojo,pone el cursior en la caja con el problema y regresa false
        //el ! lo invierte a true,entra el if y el return corta el metodo ahi
        //asi nunca se manda a mysql un dato invalido
        if (!validar(false)) {
            return;
        }
        //los huecos ? se llenan despues
        //id_estado
        //nombre_Estado
        //id_usuario
        String sql = "INSERT INTO Estado (ID_Estado, Nombre_Estado, ID_Usuario) VALUES (?, ?, ?)";
        //da la conexion a mysql
        //le manda a mysql la consulta con sus huecos y regresa ps que todavia esta vacio
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps);//llama al metodo asignaa que llena los tres ?
            ps.executeUpdate();//se guarda en mysql
            //guarda cada renglon en la tabla bitacora
            Bitacora.registrar("Estado", "INSERTAR", txtNombre.getText().trim());
            cargar();//vuelve a hacer select para que el estado nuevo aparezca en la tabla con su id y ultimo cambio por
            limpiar();
            //muestra en verde denajo de los botones que salio tod con exito
            exito("Estado registrado por " + Sesion.getLogin() + ".");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    private void actualizar() {
        //seleccionaste un estado?
        //si es null no has elegido ningun estado
        if (idOriginal == null) {
            error("Selecciona un estado de la tabla.");
            return;
        }
        //si estas actualizando
        //significado de "?
        // id nuevo lo que esta en la caja id
        // id nombre o que esta en la caja nombre
        //quien hizo el ccambio
        //cual renglon cambiar
        if (!validar(true)) {
            return;
        }
        String sql = "UPDATE Estado SET ID_Estado = ?, Nombre_Estado = ?, ID_Usuario = ? WHERE ID_Estado = ?";
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            asignar(ps);//llena los huecos ?
            ps.setInt(4, idOriginal);//llenaq el cuarto hueco a mano
            //ejecuta el update y guarda el numero que regresa cuantos renglones encontro en el where
            //1 encontro el estwdo y lo cambio
            //0 no existe ningun esatdo con su id
            int filas = ps.executeUpdate();
            //solo cambia la bitacora si de verdad se actualizo
            if (filas > 0) {
                Bitacora.registrar("Estado", "ACTUALIZAR", "ID " + idOriginal + " - " + txtNombre.getText().trim());
            }
            cargar();//refrescar la pantalla
            limpiar();//vacia las cajas,quita la seleciion y regresa id original a null para hacer otro cambios tienes que volver a seleccionar
            if (filas == 0) {//0 aviso en rojo porque el estadfo ya no estaba en la base
                error("Ese estado ya no existe.");
            } else {//mas de 0 aviso en verde
                exito("Estado actualizado por " + Sesion.getLogin() + ".");
            }
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    private void eliminar() {
        //
        if (idOriginal == null) {
            error("Selecciona un estado de la tabla.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Seguro que deseas eliminar este estado?",
                "Eliminar estado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {//si no dijo que si
            return;//cierra el metodo
        }
        try (PreparedStatement ps = Conexion.obtener().prepareStatement("DELETE FROM Estado WHERE ID_Estado = ?")) {
            ps.setInt(1, idOriginal);
            if (ps.executeUpdate() > 0) {
                //el registro ya no existe en Estado, por eso queda anotado en la bitacora quien lo borro
                Bitacora.registrar("Estado", "ELIMINAR", "ID " + idOriginal + " - " + txtNombre.getText().trim());
            }
            cargar();
            limpiar();
            exito("Estado eliminado por " + Sesion.getLogin() + ".");
        } catch (SQLException e) {
            error(traducir(e));
        }
    }

    //toma lo que escribiste en el formulario y lo pone en los heucos ? 
    //private:solo se puede usar dentro de panelEstado
    //void :no regresa nada,su trabajo es modificar el ps que recibe
    //preparedStatment ps:recibe la consulta ya preparada
    private void asignar(PreparedStatement ps) throws SQLException {
        //lee el texto de la caja id,trim le quita los espacios al inicio y al final
        String id = txtId.getText().trim();
        //si la caja esta vacia
        if (id.isEmpty()) {
            //pone null en el primer ?
            ps.setNull(1, Types.NULL); // MySQL asigna el número
        } else {//si la caja tiene algo comnvierte el texto y lo pone en el primer ?
            ps.setInt(1, Integer.parseInt(id));
        }
        //lee la caja nombre,le quita los espacios de las orillas y las pone en el segundo ?
        ps.setString(2, txtNombre.getText().trim());
        //pone en el tercer ? el id del usuario que iniciuo sesion
        ps.setInt(3, Sesion.getIdUsuario());
    }


    //rveis aprimero el id y despues el nombre,en cuanto encuentra el primer problema lo guarda junto con la caja donde esta 
    //y al final muestra el mensaje y pone el cursor en esa caja
    private boolean validar(boolean actualizando) {
        String id = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        //aqui se guarda el mensaje de error 
        // empieza en null
        //que significa:"todavia no hay ningun problema "
        String problema = null;
        //la caja donde esta el problema para poner ahi el cursor al final
        //empieza apuntando a txtID porque el id es lo primero que se revisa
        JTextField caja = txtId;

        //si la caja esta vacia
        if (id.isEmpty()) {
            //y estas actualizando
            if (actualizando) {
                //es un problema porque no puedes cambiar el id de un estado a nada
                problema = "El ID no puede quedar vacío al actualizar.";
            }
        } else {
            try {
                //si la caja id tiene algo,revisa que sea un enetero mayora 0
                if (Integer.parseInt(id) <= 0) {//si es 0 o negativo lanza error
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                problema = "El ID debe ser un número entero mayor que 0.";
            }
        }
        //revisa el nombre solo si el id estuvo bien
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
        //sin problema:regresa true i insertar() o actualizar() continuan
        //con problema regeresa false y corta el metodo
        return problema == null;
    }

    //este metodo se ejcuta cuando el usuario seleccion una fila de la tabla
    //toma los datos de lafila y los pome e los cmapos de texto de formulario para editarlos
    private void seleccionar() {
        //obtiene el indice de la fila seleccionada en la tabla(vista) si no hay ninguna seleccionada devuelve -1
        int fila = vista.getSelectedRow();
        if (fila < 0) {//si no hay fila seleccionada en la tabla
            return;//el metodo termina sin hacer nada
        }
        //comvierte el indice de la fila visible al indice real en el modelo de datos
        int f = vista.convertRowIndexToModel(fila);
        //lee el valor de la columna 0(o el id) de esa fila y lo guarda en idOriginal
        //conservq el id con el que se cargo el registro
        idOriginal = (Integer) modelo.getValueAt(f, 0);
        //muestra es id en el campo de texto
        txtId.setText(idOriginal.toString());
        //lee la columna 1(el nombre) y la comvierte a string
        txtNombre.setText((String) modelo.getValueAt(f, 1));
        //limpia la etiqueta de mensajes
        lblMensaje.setText(" ");
    }

    private void limpiar() {
        //vacia el campo de texto del id
        txtId.setText("");
        txtNombre.setText("");
        //quita la selecciojn de la tabla,asi que nugnuna fila queda resaltada
        vista.clearSelection();
        //borramos el id guardado por seleccionar() con null indicamos que no hay ningun registro en edicion
        idOriginal = null;
    }

    private void filtrar() {
        //lee el texto del campo de busqueda y le quita loes espacios al incio y al final
        String texto = txtBuscar.getText().trim();
        //aplica un filtro ordenador de filas de la tabla
        //este filrtro decide que filas se muestran y cuaales se ocultan el filtro se arma con una condicion
        //si el campo esta vacio,pasa null que quita el filtro y muestra todas las filas
        //si hay texto usa RowFilter.regexFilter("(?i)" + Pattern.quote(texto)).
        //RowFilter.regexFilter(...) muestra solo las filas donde alguna columna coincide con la expresion regular al no inidcar columnas ,busca en todas
        //(?i) hace que la busqueda ignore mayusculas y minisculas
        //Pattern.quote(texto) hace que el texto se tome de forma literal. Sin esto, si el usuario escribe caracteres como ., ( o *, se interpretarían como parte de una expresión regular y podrían dar resultados incorrectos o lanzar un error.
        ordenador.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
        actualizarConteo();
    }


    //este metodo actualiza la etiqueta que muestra cuantos registros hay y cuantos se ven cuandi hay un filtro activo
    private void actualizarConteo() {
        //cuenta todas las filas del modelo de datos,es decir,todos los registros cargados eseten visibles o no
        int total = modelo.getRowCount();
        //cuenta las filas que muestra la tabla en ese momento
        int visibles = vista.getRowCount();
        //elige entre singular y plural segun el total
        //con 1 usa registro y con cualquier otro numero incluido 0 usa registros
        String palabra = total == 1 ? " registro" : " registros";
        //escribe el tecto en la etiqueta usando otra condicion
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
                return "Falta una tabla (Estado o Bitacora). Ejecuta el script usuarios_finales.sql.";
            default:
                return "Error de MySQL " + e.getErrorCode() + ": " + e.getMessage();
        }
    }
}


//en resumen dice :quiero una pantalla para la tabla estado que tiene un id y un nombre de hasta 20 caracteres
