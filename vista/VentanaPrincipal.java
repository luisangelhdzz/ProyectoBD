package vista;

import conexion.Conexion;
import modelo.Sesion;
import vista.tablas.PanelPosicion;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Ventana de la aplicación: muestra el login o la pantalla principal con las tablas. */
public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal() {
        // Llama al constructor de JFrame y le pone el título a la ventana
        super("Liga de Baseball Mexicana");
        // Al darle a la X solo se destruye (dispose) la ventana, no se cierra el programa de golpe
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Agrega un "oyente" que reacciona a eventos de la ventana (clase anónima que extiende WindowAdapter)
        addWindowListener(new WindowAdapter() {
            // Sobrescribe el método de WindowAdapter para decidir qué hacer cuando la ventana ya se cerró
            @Override
            public void windowClosed(WindowEvent e) {
                // Cierra la conexión a la base de datos para no dejarla abierta
                Conexion.cerrar();
                // Termina el programa por completo (0 = salida sin error)
                System.exit(0);
            }
        });
        // Muestra la pantalla de login al arrancar la aplicación
        mostrarLogin();
    }

    // ------------------------------------------------------------------
    // PANTALLAS
    // ------------------------------------------------------------------

    // Método privado (solo se usa dentro de esta clase) que muestra la pantalla de login
    private void mostrarLogin() {
        // Crea el PanelLogin y le pasa this::mostrarPrincipal, el método que se ejecutará cuando el login sea correcto.
        // Luego cambiarContenido pone ese panel en la ventana con tamaño 420 de ancho x 340 de alto
        cambiarContenido(new PanelLogin(this::mostrarPrincipal), 420, 340);
    }

    // Método privado que arma y muestra la pantalla principal (se llama después de un login correcto)
    private void mostrarPrincipal() {
        // Crea la barra superior; FlowLayout.RIGHT alinea sus componentes a la derecha, con 14 px de separación horizontal y 6 px vertical
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 6));
        // Agrega una etiqueta con el nombre y el login del usuario que inició sesión (datos guardados en Sesion)
        barra.add(new JLabel("Usuario: " + Sesion.getNombre() + " (" + Sesion.getLogin() + ")"));
        // Crea el botón para cerrar sesión
        JButton salir = new JButton("Cerrar sesión");
        // Al hacer clic en el botón se ejecuta cerrarSesion() (lambda que recibe el evento e)
        salir.addActionListener(e -> cerrarSesion());
        // Agrega el botón a la barra, después de la etiqueta
        barra.add(salir);

        // ------------------------------------------------------------------
        // AQUÍ SE REGISTRAN LAS TABLAS: cada tabla es una pestaña
        // Para agregar otra tabla: crea su clase en vista/tablas y agrégala aquí.
        // ------------------------------------------------------------------
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Posiciones", new PanelPosicion());
        // Siguientes tablas, por ejemplo:
        // pestanas.addTab("Ciudades", new PanelCiudad());
        // pestanas.addTab("Equipos", new PanelEquipo());
        // pestanas.addTab("Jugadores", new PanelJugador());

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(barra, BorderLayout.NORTH);
        raiz.add(pestanas, BorderLayout.CENTER);
        cambiarContenido(raiz, 860, 540);
    }

    private void cerrarSesion() {
        Sesion.cerrar();
        Conexion.cerrar();
        mostrarLogin();
    }

    private void cambiarContenido(JComponent nuevo, int ancho, int alto) {
        setContentPane(nuevo);
        setSize(ancho, alto);
        setLocationRelativeTo(null);
        revalidate();
        repaint();
    }
}
