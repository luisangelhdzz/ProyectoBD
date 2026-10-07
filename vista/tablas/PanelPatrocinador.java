package vista.tablas;

public class PanelPatrocinador extends PanelBase {
        public PanelPatrocinador() {
        //super(...) llama al constructor de PanelBase y le manda:
        //nombre de la tabla,columna del id,columna del nombre,titulo del nombre en pantalla y el maximo del VARCHAR
        super("Patrocinador", "ID_Patrocinador", "Nombre_Patrocinador","Nombre del patrocinador", 20);
    }
    
}
