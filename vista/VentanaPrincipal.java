package vista;

import conexion.Conexion;
import modelo.Sesion;
import vista.tablas.PanelEstado;
import vista.tablas.PanelPatrocinador;
import vista.tablas.PanelPosicion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ventana de la aplicación: muestra el login o la pantalla principal con el menú. */
public class VentanaPrincipal extends JFrame {

    /** Secciones del menú agrupadas por título (CATÁLOGOS, EQUIPOS, ...). */
    private final Map<String, List<PanelTabla>> grupos = new LinkedHashMap<>();

    private final List<JButton> botonesMenu = new ArrayList<>();
    private CardLayout tarjetas;
    private JPanel contenido;

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
    // AQUÍ SE REGISTRAN LAS TABLAS DEL MENÚ
    // Para agregar otra tabla: crea su clase en vista/tablas y agrégala
    // aquí con el grupo donde debe aparecer.
    // ------------------------------------------------------------------
    private void registrarSecciones() {
        grupos.clear();

        agregar("CATÁLOGOS", new PanelEstado());
        agregar("CATÁLOGOS", new PanelPosicion());
        agregar("CATÁLOGOS", new PanelPatrocinador());

        // Siguientes tablas, por ejemplo:
        // agregar("CATÁLOGOS", new PanelCiudad());
        // agregar("EQUIPOS", new PanelEquipo());
        // agregar("EQUIPOS", new PanelJugador());
    }

    private void agregar(String grupo, PanelTabla panel) {
        grupos.computeIfAbsent(grupo, g -> new ArrayList<>()).add(panel);
    }

    // ------------------------------------------------------------------
    // PANTALLAS
    // ------------------------------------------------------------------

    private void mostrarLogin() {
        setMinimumSize(null);
        cambiarContenido(new PanelLogin(this::mostrarPrincipal), 440, 440);
    }

    private void mostrarPrincipal() {
        registrarSecciones();

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(construirBarraSuperior(), BorderLayout.NORTH);

        tarjetas = new CardLayout();
        contenido = new JPanel(tarjetas);
        contenido.add(construirBienvenida(), "inicio");
        for (List<PanelTabla> secciones : grupos.values()) {
            for (PanelTabla p : secciones) {
                contenido.add(p, p.getTitulo());
            }
        }
        raiz.add(construirMenu(), BorderLayout.WEST);
        raiz.add(contenido, BorderLayout.CENTER);

        cambiarContenido(raiz, 1150, 660);
        setMinimumSize(new Dimension(1000, 600));
    }

    private JComponent construirBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Estilo.VERDE);
        barra.setBorder(new EmptyBorder(10, 20, 10, 16));
        barra.add(Estilo.etiqueta("LIGA DE BASEBALL MEXICANA", 3, true, Estilo.BLANCO), BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        derecha.setOpaque(false);
        derecha.add(Estilo.etiqueta("Usuario: " + Sesion.getNombre() + " (" + Sesion.getLogin() + ")",
                0, false, Estilo.BLANCO));
        JButton salir = Estilo.boton("Cerrar sesión", Estilo.ARCILLA);
        salir.addActionListener(e -> cerrarSesion());
        derecha.add(salir);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JComponent construirMenu() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Estilo.VERDE_OSCURO);
        menu.setBorder(new EmptyBorder(16, 0, 16, 0));
        menu.setPreferredSize(new Dimension(200, 0));

        botonesMenu.clear();
        for (Map.Entry<String, List<PanelTabla>> grupo : grupos.entrySet()) {
            JLabel titulo = Estilo.etiqueta(grupo.getKey(), -2, true, new Color(0xb9c9bd));
            titulo.setBorder(new EmptyBorder(10, 20, 6, 0));
            titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
            menu.add(titulo);

            for (PanelTabla panel : grupo.getValue()) {
                JButton b = Estilo.boton(panel.getTitulo(), Estilo.VERDE_OSCURO);
                b.setHorizontalAlignment(SwingConstants.LEFT);
                b.setFont(Estilo.fuente(0, false));
                b.setBorder(new EmptyBorder(9, 20, 9, 20));
                b.setAlignmentX(Component.LEFT_ALIGNMENT);
                b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
                b.addActionListener(e -> abrir(panel, b));
                botonesMenu.add(b);
                menu.add(b);
            }
        }
        return menu;
    }

    private JComponent construirBienvenida() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Estilo.CREMA);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.insets = new Insets(0, 0, 10, 0);
        p.add(Estilo.etiqueta("Bienvenido, " + Sesion.getNombre(), 12, true, Estilo.VERDE), g);
        p.add(Estilo.etiqueta("Elige una tabla en el menú de la izquierda.", 2, false, Estilo.TEXTO), g);
        p.add(Estilo.etiqueta("Cada registro que agregues o modifiques quedará a tu nombre.", 0, false, Estilo.GRIS), g);
        return p;
    }

    private void abrir(PanelTabla panel, JButton boton) {
        for (JButton b : botonesMenu) {
            b.setBackground(b == boton ? Estilo.ARCILLA : Estilo.VERDE_OSCURO);
            b.setFont(Estilo.fuente(0, b == boton));
        }
        panel.alMostrar();
        tarjetas.show(contenido, panel.getTitulo());
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
