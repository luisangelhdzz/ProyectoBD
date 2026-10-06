package vista.tablas;

import modelo.Campo;
import vista.PanelTabla;

/** Sección de la tabla Patrocinador. */
public class PanelPatrocinador extends PanelTabla {

    public PanelPatrocinador() {
        super("Patrocinador", "patrocinador", false, "Patrocinadores",
                Campo.id("ID_Patrocinador", "ID"),
                Campo.texto("Nombre_Patrocinador", "Nombre del patrocinador", 20));
    }
}
