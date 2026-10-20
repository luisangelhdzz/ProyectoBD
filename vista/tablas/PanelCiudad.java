package vista.tablas;

//tabla hija de Estado: cada ciudad pertenece a un estado
//el estado se elige con un menu que muestra los estados que hay en la tabla Estado
public class PanelCiudad extends PanelBaseHija {
    public PanelCiudad() {
        super("Ciudad");
        llaveAutomatica("ID_Ciudad");
        texto("Nombre_Ciudad", "Nombre de la ciudad", 20);
        menu("ID_Estado", "Estado", "SELECT ID_Estado, Nombre_Estado FROM Estado ORDER BY Nombre_Estado");
        armarPantalla();
    }
}
