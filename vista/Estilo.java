package vista;
//esta clase es como la hoja de estilos de la aplicaciom
//guarda en un solo lugar los colores,las fuentes y la forma de creacr botones y el campo de texto con el mismo diseno
import javax.swing.*;
import javax.swing.border.CompoundBorder;//tipos de bordes para los componentes
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/** Colores, fuentes y componentes con el estilo de la aplicación. */
public final class Estilo {

    //pregunta a java en que sistema corre y revisa si el nombre.
    public static final boolean ES_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    //con el operador ternario elige la fuente y el tamano
    public static final String FUENTE = ES_MAC ? "Helvetica" : "Segoe UI";
    public static final int TAM = ES_MAC ? 13 : 12;

    public static final Color VERDE = new Color(0x1f4d2e);
    public static final Color VERDE_OSCURO = new Color(0x15361f);
    public static final Color CREMA = new Color(0xf6f1e4);
    public static final Color CREMA_2 = new Color(0xece4d0);
    public static final Color ARCILLA = new Color(0xb5532f);
    public static final Color ARCILLA_OSCURO = new Color(0x96401f);
    public static final Color ROJO = new Color(0x9b2c2c);
    public static final Color TEXTO = new Color(0x1d1d1b);
    public static final Color GRIS = new Color(0x6b6b63);
    public static final Color BLANCO = Color.WHITE;

    private Estilo() {
    }

       //crea una fuente con el tamano base mas extra
    public static Font fuente(int extra, boolean negrita) {
        return new Font(FUENTE, negrita ? Font.BOLD : Font.PLAIN, TAM + extra);
    }

    //crea una etiqueta
    public static JLabel etiqueta(String texto, int extra, boolean negrita, Color color) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(extra, negrita));
        l.setForeground(color);
        return l;
    }

    //crea un boton con estilo plano
    public static JButton boton(String texto, Color fondo) {
        JButton b = new JButton(texto);
        b.setFont(fuente(0, true));
        b.setBackground(fondo);
        b.setForeground(BLANCO);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        return b;
    }

    //aplica el estilo a un campo de entrada:fuente normal fondo blanco y texto oscuro
    public static void estiloCampo(JComponent c) {
        c.setFont(fuente(0, false));
        c.setBackground(BLANCO);
        c.setForeground(TEXTO);
        if (c instanceof JTextField) {
            c.setBorder(new CompoundBorder(new LineBorder(CREMA_2.darker(), 1), new EmptyBorder(6, 8, 6, 8)));
        }
    }
}
