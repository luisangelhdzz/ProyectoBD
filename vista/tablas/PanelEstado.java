package vista.tablas;

//tabla padre: solo ID y nombre. Todo lo demas lo hace PanelTabla
public class PanelEstado extends PanelTabla {
    public PanelEstado() {
        super("Estado");
        llaveAutomatica("ID_Estado");
        texto("Nombre_Estado", "Nombre del estado", 20);
        armarPantalla();
    }
}
