package vista.tablas;

//tabla padre: solo ID y nombre. Todo lo demas lo hace PanelTabla
public class PanelPosicion extends PanelTabla {
    public PanelPosicion() {
        super("Posicion");
        llaveAutomatica("ID_Posicion");
        texto("Nombre_Posicion", "Nombre de la posición", 20);
        armarPantalla();
    }
}
