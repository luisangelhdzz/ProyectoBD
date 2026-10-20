package vista.tablas;

import java.sql.SQLException;

//tabla hija de Partido,Equipo,Jugador y Posicion: quien juega cada partido,en que posicion y en que turno al bat
//su llave primaria es (equipo, jugador, partido): un jugador aparece una sola vez en la alineacion de cada partido
public class PanelAlineacion extends PanelBaseHija {
    public PanelAlineacion() {
        super("Alineacion");
        //el partido se ve como "1 - Sultanes vs Leones 2025-05-16"
        llaveMenu("ID_Partido", "Partido",
                "SELECT p.ID_Partido, CONCAT_WS(' ', l.Nombre_Equipo, 'vs', v.Nombre_Equipo, DATE(p.Fecha_Partido)) "
                        + "FROM Partido p "
                        + "JOIN Equipo l ON l.ID_Equipo = p.ID_Equipo_Local "
                        + "JOIN Equipo v ON v.ID_Equipo = p.ID_Equipo_Visitante "
                        + "ORDER BY p.Fecha_Partido");
        llaveMenu("ID_Equipo", "Equipo", "SELECT ID_Equipo, Nombre_Equipo FROM Equipo ORDER BY Nombre_Equipo");
        //el jugador se ve con su equipo entre parentesis,ej. "3 - Carlos Treviño (Sultanes)"
        llaveMenu("ID_Jugador", "Jugador",
                "SELECT j.ID_Jugador, CONCAT_WS(' ', j.Nombre_Jugador, j.Apellido_Jugador, CONCAT('(', e.Nombre_Equipo, ')')) "
                        + "FROM Jugador j JOIN Equipo e ON e.ID_Equipo = j.ID_Equipo "
                        + "ORDER BY j.Nombre_Jugador, j.Apellido_Jugador");
        menu("ID_Posicion", "Posición", "SELECT ID_Posicion, Nombre_Posicion FROM Posicion ORDER BY Nombre_Posicion");
        numero("Orden_Bateo", "Orden al bat", 1, 9);//el CHECK de la tabla pide del 1 al 9
        //mensajes para las reglas de la tabla que revisa MySQL
        avisoRegla("PRIMARY", "Ese jugador ya está en la alineación de ese partido.");
        avisoRegla("UQ_Alineacion_Orden", "Ese turno al bat ya lo tiene otro jugador del mismo equipo en ese partido.");
        avisoRegla("UQ_Alineacion_Posicion", "Esa posición ya la tiene otro jugador del mismo equipo en ese partido.");
        armarPantalla();
    }

    //reglas que MySQL no revisa pero que deben cumplirse para que los datos tengan sentido
    @Override
    protected String reglaExtra() throws SQLException {
        //el equipo tiene que ser el local o el visitante de ese partido
        if (!existe("SELECT 1 FROM Partido WHERE ID_Partido = ? AND ? IN (ID_Equipo_Local, ID_Equipo_Visitante)",
                idElegido("ID_Partido"), idElegido("ID_Equipo"))) {
            return "Ese equipo no juega en el partido elegido.";
        }
        //el jugador tiene que ser de ese equipo
        if (!existe("SELECT 1 FROM Jugador WHERE ID_Jugador = ? AND ID_Equipo = ?",
                idElegido("ID_Jugador"), idElegido("ID_Equipo"))) {
            return "Ese jugador no pertenece al equipo elegido.";
        }
        return null;
    }
}
