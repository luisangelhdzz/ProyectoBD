//este codigo crea la pantalla para manejar la tabla estado de la base de datos,esta clase solo le dice que tabla es y que columnas tiene


package vista.tablas;

import modelo.Campo;
import vista.PanelTabla;//traen dos clases de otras partes del proyecto para poder usarlas aqui

//crea la clase panel estado que hereda de panelTabla,recibe botones y consultas sin tener que reescribirlo
public class PanelEstado extends PanelTabla {

    public PanelEstado() {
        //llama al constructor de panleTabla y le pasa a la configuracion
        super("Estado", "estado", false, "Estados",
                Campo.id("ID_Estado", "ID"),
                Campo.texto("Nombre_Estado", "Nombre del estado", 20));
    }
}


//en resumen dice :quiero una pantalla para la tabla estado que tiene un id y un nombre de hasta 20 caracteres