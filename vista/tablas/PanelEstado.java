//pantalla de la tabla Estado
//toda la pantalla,el CRUD y los mensajes los hereda de PanelBase,aqui solo se dicen los datos de la tabla Estado
//en resumen dice: quiero una pantalla para la tabla Estado que tiene un id y un nombre de hasta 20 caracteres

package vista.tablas;

public class PanelEstado extends PanelBase {

    public PanelEstado() {
        //super(...) llama al constructor de PanelBase y le manda:
        //nombre de la tabla,columna del id,columna del nombre,titulo del nombre en pantalla y el maximo del VARCHAR
        super("Estado", "ID_Estado", "Nombre_Estado", "Nombre del estado", 20);
    }
}