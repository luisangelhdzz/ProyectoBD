package vista;

import conexion.Conexion;
import modelo.Sesion;
import vista.tablas.PanelEstado;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Ventana de la aplicación: muestra el login o la pantalla principal con las tablas. */
public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal() {
        super("Liga de Baseball Mexicana");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                Conexion.cerrar();
                System.exit(0);
            }
        });
        mostrarLogin();
    }

    // ------------------------------------------------------------------
    // PANTALLAS
    // ------------------------------------------------------------------

    private void mostrarLogin() {
        cambiarContenido(new PanelLogin(this::mostrarPrincipal), 420, 340);
    }

    private void mostrarPrincipal() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 6));
        barra.add(new JLabel("Usuario: " + Sesion.getNombre() + " (" + Sesion.getLogin() + ")"));
        JButton salir = new JButton("Cerrar sesión");
        salir.addActionListener(e -> cerrarSesion());
        barra.add(salir);

        // ------------------------------------------------------------------
        // AQUÍ SE REGISTRAN LAS TABLAS: cada tabla es una pestaña
        // Para agregar otra tabla: crea su clase en vista/tablas y agrégala aquí.
        // ------------------------------------------------------------------
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Estados", new PanelEstado());

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
