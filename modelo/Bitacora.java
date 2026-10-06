//bitacora guarda un renglon por cada movimiento (alta, cambio o baja) que hace un usuario final
//asi aunque un registro se elimine queda guardado quien lo borro y cuando

package modelo;

import conexion.Conexion;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class Bitacora {

    private Bitacora() {
    }

    //tabla = en que tabla se hizo el movimiento (ej. "Estado")
    //accion = "INSERTAR", "ACTUALIZAR" o "ELIMINAR"
    //detalle = que registro fue (ej. "ID 5 - Coahuila")
    //la fecha la pone MySQL sola y el usuario se toma de la Sesion
    public static void registrar(String tabla, String accion, String detalle) throws SQLException {
        String sql = "INSERT INTO Bitacora (ID_Usuario, Tabla, Accion, Detalle) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            ps.setInt(1, Sesion.getIdUsuario());
            ps.setString(2, tabla);
            ps.setString(3, accion);
            ps.setString(4, detalle);
            ps.executeUpdate();
        }
    }
}
