package vista.tablas;

import java.time.LocalDate;

//tabla hija de Equipo y de Patrocinador: que patrocinador tiene cada equipo y de cuando a cuando
//su llave primaria es la pareja (equipo, patrocinador): un equipo tiene un solo contrato con cada patrocinador
public class PanelContrato extends PanelBaseHija {
    public PanelContrato() {
        super("Contrato");
        llaveMenu("ID_Equipo", "Equipo", "SELECT ID_Equipo, Nombre_Equipo FROM Equipo ORDER BY Nombre_Equipo");
        llaveMenu("ID_Patrocinador", "Patrocinador",
                "SELECT ID_Patrocinador, Nombre_Patrocinador FROM Patrocinador ORDER BY Nombre_Patrocinador");
        fecha("Inicio_Contrato", "Inicio del contrato");
        fecha("Fin_Contrato", "Fin del contrato");
        avisoRegla("PRIMARY", "Ese equipo ya tiene un contrato con ese patrocinador.");
        armarPantalla();
    }

    //el contrato no puede terminar antes de empezar
    @Override
    protected String reglaExtra() {
        LocalDate inicio = fechaEscrita("Inicio_Contrato");
        LocalDate fin = fechaEscrita("Fin_Contrato");
        if (fin.isBefore(inicio)) {
            return "La fecha de fin no puede ser antes de la fecha de inicio.";
        }
        return null;
    }
}
