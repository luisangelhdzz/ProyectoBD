package vista.tablas;

import modelo.Campo;
import vista.PanelTabla;

/** Sección de la tabla Posicion. */
public class PanelPosicion extends PanelTabla {

    public PanelPosicion() {
        super("Posicion", "posición", true, "Posiciones",
                Campo.id("ID_Posicion", "ID"),
                Campo.texto("Nombre_Posicion", "Nombre de la posición", 20));
    }
}
