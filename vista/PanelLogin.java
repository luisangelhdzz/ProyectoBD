//indica que esta clase vive en el paquete (carpeta) "vista", donde estan todas las pantallas
package vista;
//esta clase es la pantalla de inciio de sesion de la app,arma el formualario,revisa los datos conra mySql y si son correctos avisa para pasar a la siguiente pantalla

//importa la clase Conexion (paquete conexion) para poder cerrar la conexion si falla y leer el nombre de la base y del usuario de MySQL
import conexion.Conexion;
//importa la clase Sesion (paquete modelo) que es la que de verdad consulta la tabla Usuario y recuerda quien entro
import modelo.Sesion;

//importa TODAS las clases de Swing (JPanel, JLabel, JTextField, JPasswordField, JButton, SwingUtilities...)
import javax.swing.*;
//EmptyBorder es un borde invisible que solo sirve para dejar espacio (margen) alrededor de un componente
import javax.swing.border.EmptyBorder;
//importa las clases de AWT: BorderLayout, GridBagLayout, GridBagConstraints, Insets, Dimension, Color...
import java.awt.*;
//SQLException es el error que lanza Java cuando algo falla al hablar con la base de datos
import java.sql.SQLException;

/** Pantalla de inicio de sesión de los usuarios finales (tabla Usuario). */
//"extends JPanel" significa que PanelLogin ES un panel de Swing,o sea un rectangulo donde se pueden poner componentes
//la VentanaPrincipal lo crea y lo pone como contenido de la ventana
public class PanelLogin extends JPanel {

    //alEntrar es una "accion guardada" (Runnable = un bloque de codigo sin parametros que se ejecuta con run())
    //la VentanaPrincipal nos la pasa (this::mostrarPrincipal) y nosotros la ejecutamos cuando el login es correcto
    //asi el login no necesita saber que pantalla sigue,solo avisa "ya entro"
    //final = una vez asignada en el constructor ya no se puede cambiar
    private final Runnable alEntrar;
    //caja de texto donde se escribe el usuario; el 18 es el ancho aproximado en columnas (caracteres)
    private final JTextField txtUsuario = new JTextField(18);
    //caja para la contrasena; es igual que JTextField pero muestra puntitos en lugar de las letras
    private final JPasswordField txtClave = new JPasswordField(18);
    //etiqueta roja donde se muestran los errores
    //empieza con " " (un espacio) y no con "" para que la etiqueta ya ocupe su altura y el formulario no "brinque" cuando aparece un error
    private final JLabel lblError = new JLabel(" ");

    //constructor: se ejecuta al hacer new PanelLogin(...) y arma toda la pantalla
    public PanelLogin(Runnable alEntrar) {
        //guarda la accion recibida en el atributo de la clase (this.alEntrar) para usarla despues en entrar()
        this.alEntrar = alEntrar;
        //el panel acomoda etiquetas, cajas y boton con GridBagLayout (una cuadricula flexible)
        setLayout(new GridBagLayout());
        //margen interno: 20 arriba, 50 izquierda, 20 abajo, 50 derecha (en pixeles)
        setBorder(new EmptyBorder(20, 50, 20, 50));
        //GridBagConstraints son las "reglas" de como colocar cada componente en el GridBagLayout
        //se crea una sola vez y se reutiliza cambiandole valores antes de cada add
        GridBagConstraints g = new GridBagConstraints();
        //gridx = 0: todo va en la columna 0,o sea una sola columna
        //como no se fija gridy, cada add nuevo se pone en el siguiente renglon (uno debajo del otro)
        g.gridx = 0;
        //HORIZONTAL = cada componente se estira a lo ancho para llenar toda su celda (cajas y boton del mismo ancho)
        g.fill = GridBagConstraints.HORIZONTAL;
        //weightx = 1: la columna se queda con todo el espacio horizontal sobrante
        g.weightx = 1;

        //Insets(arriba, izquierda, abajo, derecha) = margen externo del componente que se agrega
        //aqui deja 14 pixeles abajo del titulo
        g.insets = new Insets(0, 0, 14, 0);
        //titulo centrado y en negrita de 16 puntos
        JLabel titulo = new JLabel("LIGA DE BASEBALL MEXICANA", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        add(titulo, g);

        //4 pixeles abajo de la etiqueta "Usuario" para que quede pegadita a su caja
        g.insets = new Insets(0, 0, 4, 0);
        add(new JLabel("Usuario"), g);
        //12 pixeles abajo de la caja de usuario para separarla de la parte de contrasena
        g.insets = new Insets(0, 0, 12, 0);
        //agrega la caja de usuario al formulario
        add(txtUsuario, g);

        //otra vez 4 pixeles debajo de la etiqueta
        g.insets = new Insets(0, 0, 4, 0);
        //etiqueta "Contrasena"
        add(new JLabel("Contraseña"), g);
        //8 pixeles abajo de la caja de contrasena (antes del mensaje de error)
        g.insets = new Insets(0, 0, 8, 0);
        //agrega la caja de contrasena
        add(txtClave, g);

        //crea el boton "Entrar"
        JButton btnEntrar = new JButton("Entrar");
        //cuando se hace clic en el boton se llama al metodo entrar()
        //"e -> entrar()" es una lambda: e es el evento del clic (no se usa) y entrar() es lo que se ejecuta
        btnEntrar.addActionListener(e -> entrar());
        //4 pixeles arriba del boton para separarlo un poco
        g.insets = new Insets(4, 0, 0, 0);
        //agrega el boton debajo de las cajas
        add(btnEntrar, g);

        //agrega la etiqueta de error (por ahora solo un espacio en blanco) al final,con 8 pixeles arriba
        //se queda con todo el espacio que sobra hacia abajo (weighty = 1) y el texto empieza arriba (TOP)
        //asi un mensaje largo tiene lugar para partirse en varios renglones sin que se corte
        lblError.setForeground(Color.RED);
        lblError.setVerticalAlignment(SwingConstants.TOP);
        g.insets = new Insets(8, 0, 0, 0);
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        add(lblError, g);

        //en un JTextField el ActionListener se dispara al presionar ENTER
        //si das ENTER en la caja de usuario, el cursor salta a la caja de contrasena
        txtUsuario.addActionListener(e -> txtClave.requestFocusInWindow());
        //si das ENTER en la caja de contrasena, intenta iniciar sesion (igual que dar clic en "Entrar")
        txtClave.addActionListener(e -> entrar());
    }

    //@Override indica que estamos reescribiendo un metodo que ya existe en JPanel
    //addNotify() lo llama Swing automaticamente cuando el panel se agrega a una ventana visible
    @Override
    public void addNotify() {
        //primero hace lo que normalmente hace JPanel (obligatorio, si no el panel no se muestra bien)
        super.addNotify();
        //invokeLater = "hazlo un momentito despues", cuando Swing termine de dibujar la ventana
        //ahi pone el cursor en la caja de usuario para que el usuario pueda escribir de inmediato
        //txtUsuario::requestFocusInWindow es una referencia a metodo,equivale a () -> txtUsuario.requestFocusInWindow()
        SwingUtilities.invokeLater(txtUsuario::requestFocusInWindow);
    }

    //metodo que se ejecuta al dar clic en "Entrar" o ENTER en la contrasena; valida los datos e intenta iniciar sesion
    private void entrar() {
        //lee el texto de la caja de usuario y trim() le quita espacios al inicio y al final
        String usuario = txtUsuario.getText().trim();
        //getPassword() devuelve un arreglo de char (por seguridad no da un String),aqui lo convertimos a String
        //a la contrasena NO se le hace trim porque un espacio podria ser parte de la contrasena
        String clave = new String(txtClave.getPassword());

        //si alguna de las dos cajas esta vacia ni siquiera vamos a la base de datos
        if (usuario.isEmpty() || clave.isEmpty()) {
            //muestra el aviso en rojo
            lblError.setText("Escribe tu usuario y tu contraseña.");
            //return corta el metodo aqui,no sigue con lo de abajo
            return;
        }

        //try: aqui va el codigo que puede fallar al hablar con MySQL
        try {
            //Sesion.iniciar hace el SELECT en la tabla Usuario comparando la contrasena con SHA2
            //devuelve true si encontro al usuario con esa contrasena y false si no
            if (Sesion.iniciar(usuario, clave)) {
                //login correcto: limpia el mensaje de error (deja el espacio para conservar la altura)
                lblError.setText(" ");
                //ejecuta la accion que nos paso la VentanaPrincipal: cambia a la pantalla principal
                alEntrar.run();
            } else {
                //login incorrecto: avisa al usuario
                lblError.setText("Usuario o contraseña incorrectos.");
                //borra la contrasena escrita para que la vuelva a escribir
                txtClave.setText("");
                //pone el cursor de nuevo en la caja de contrasena
                txtClave.requestFocusInWindow();
            }
        //catch: si hubo un error de base de datos (MySQL apagado, base inexistente, etc.) cae aqui en vez de tronar el programa
        } catch (SQLException e) {
            //cierra la conexion que pudo quedar danada,asi el siguiente intento abre una nueva desde cero
            Conexion.cerrar();
            //muestra el mensaje de error traducido a algo entendible
            //se envuelve en <html>...</html> porque un JLabel con HTML hace salto de linea automatico si el texto es largo
            lblError.setText("<html>" + mensajeError(e) + "</html>");
        }
    }

    //recibe la excepcion de MySQL y regresa un mensaje en espanol facil de entender segun el tipo de error
    private String mensajeError(SQLException e) {
        //obtiene el mensaje original del error; si viene null lo cambia por "" para que msg.contains() no lance NullPointerException
        //esto es un operador ternario: condicion ? valorSiTrue : valorSiFalse
        String msg = e.getMessage() == null ? "" : e.getMessage();
        //"No suitable driver" significa que Java no encontro el conector de MySQL (el archivo mysql-connector-j-...jar)
        if (msg.contains("No suitable driver")) {
            return "No se encontró el conector de MySQL (.jar).";
        }
        //getErrorCode() da el numero de error que manda MySQL; segun el numero se escoge el mensaje
        switch (e.getErrorCode()) {
            //1045 = Access denied: el usuario o la clave de MySQL de la aplicacion (APP_LIGA) son incorrectos o no existe ese usuario
            case 1045:
                return "La aplicación no pudo entrar a MySQL con el usuario " + Conexion.USUARIO_BD
                        + ". Ejecuta el script usuarios_finales.sql.";
            //1049 = Unknown database: la base BD_LIGA_BASEBALL no existe
            case 1049:
                return "No existe la base " + Conexion.BASE_DATOS + ".";
            //1146 = Table doesn't exist: la base existe pero le falta la tabla Usuario
            case 1146:
                return "No existe la tabla Usuario. Ejecuta el script usuarios_finales.sql.";
            //0 = no hubo respuesta de MySQL (normalmente el servidor esta apagado o no se pudo conectar al puerto)
            case 0:
                return "No se pudo conectar a MySQL. ¿Está encendido?";
            //cualquier otro error: muestra el numero y el mensaje original de MySQL
            default:
                return "Error de MySQL " + e.getErrorCode() + ": " + msg;
        }
    }
}
