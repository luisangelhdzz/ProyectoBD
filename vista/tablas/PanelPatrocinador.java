package vista.tablas;

//tabla padre: solo ID y nombre. Todo lo demas lo hace PanelTabla
public class PanelPatrocinador extends PanelTabla {
    public PanelPatrocinador() {
        super("Patrocinador");
        llaveAutomatica("ID_Patrocinador");
        texto("Nombre_Patrocinador", "Nombre del patrocinador", 20);
        armarPantalla();
    }
}
