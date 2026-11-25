//bitacora guarda un renglon por cada movimiento (alta, cambio o baja) que hace un usuario final
//asi aunque un registro se elimine queda guardado quien lo borro y cuando

package modelo;

import conexion.BaseDatos;

import java.sql.SQLException;

public final class Bitacora {

    private Bitacora() {
    }

    //tabla = en que tabla se hizo el movimiento (ej. "Estado")
    //accion = "INSERTAR", "ACTUALIZAR" o "ELIMINAR"
    //detalle = que registro fue (ej. "ID 5 - Coahuila")
    //la fecha la pone MySQL sola y el usuario se toma de la Sesion
    public static void registrar(String tabla, String accion, String detalle) throws SQLException {
        BaseDatos.ejecutar("INSERT INTO Bitacora (ID_Usuario, Tabla, Accion, Detalle) VALUES (?, ?, ?, ?)",
                Sesion.getIdUsuario(), tabla, accion, detalle);
    }
}
