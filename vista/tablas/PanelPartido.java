package vista.tablas;

import modelo.Sesion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

//pantalla "Programar Partidos": tabla hija de Temporada,Estadio y Equipo (el equipo dos veces: local y visitante)
//cada partido tiene su fecha y hora,y las carreras de cada equipo
//hereda de PanelTabla,asi que ya trae el formulario,la tabla,el buscador,las validaciones,la bitacora
//y el aviso de llave foranea al eliminar (un partido con alineaciones o carreras no se puede borrar)
//para ver los juegos de un equipo se escribe su nombre en el buscador de arriba
public class PanelPartido extends PanelTabla {
    public PanelPartido() {
        super("Partido");
        llaveAutomatica("ID_Partido");
        menu("ID_Temporada", "Temporada", "SELECT ID_Temporada, Anio_Temporada FROM Temporada ORDER BY Anio_Temporada DESC");
        menu("ID_Estadio", "Estadio", "SELECT ID_Estadio, Nombre_Estadio FROM Estadio ORDER BY Nombre_Estadio");
        menu("ID_Equipo_Local", "Equipo local", "SELECT ID_Equipo, Nombre_Equipo FROM Equipo ORDER BY Nombre_Equipo");
        menu("ID_Equipo_Visitante", "Equipo visitante", "SELECT ID_Equipo, Nombre_Equipo FROM Equipo ORDER BY Nombre_Equipo");
        fechaHora("Fecha_Partido", "Fecha del juego");
        numero("Carreras_Local", "Carreras local", 0, 99);
        numero("Carreras_Visitante", "Carreras visitante", 0, 99);
        armarPantalla();
        agregarEncabezado();
    }

    //el local y el visitante tienen que ser equipos distintos
    @Override
    protected String reglaExtra() throws SQLException {
        if (idElegido("ID_Equipo_Local").equals(idElegido("ID_Equipo_Visitante"))) {
            return "El equipo local y el visitante no pueden ser el mismo.";
        }
        return null;
    }

    //arriba de todo: el titulo,la fecha de hoy y quien atiende (el usuario que inicio sesion)
    //armarPantalla() ya puso el buscador en el norte,asi que se saca,se mete debajo del titulo y todo vuelve al norte
    private void agregarEncabezado() {
        BorderLayout distribucion = (BorderLayout) getLayout();
        Component buscador = distribucion.getLayoutComponent(BorderLayout.NORTH);
        remove(buscador);

        JLabel titulo = new JLabel("Programar Partidos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        JLabel hoy = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        JPanel fila = new JPanel(new BorderLayout());
        fila.add(titulo, BorderLayout.WEST);
        fila.add(hoy, BorderLayout.EAST);

        JPanel datos = new JPanel(new GridLayout(2, 1, 0, 2));
        datos.add(fila);
        datos.add(new JLabel("Le atiende: " + Sesion.getNombre()));

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setBorder(new EmptyBorder(0, 0, 4, 0));
        norte.add(datos, BorderLayout.NORTH);
        norte.add(buscador, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);
    }
}
