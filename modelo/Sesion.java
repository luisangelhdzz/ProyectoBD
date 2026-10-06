//sesion es la clase que recuerda quien inicio sesion mientras la app este abierta
//sirve oara validar el login y la contrasena cobtra mi tabla usuario
//y cada que un usuario modiffica algo deja su id en la modificacion


package modelo;

import conexion.Conexion;

import java.sql.PreparedStatement;//una consulta sql con huecos ? que se llenan despues
import java.sql.ResultSet;//los renglones que dvuelve un select
import java.sql.SQLException;


public final class Sesion {

    private static Integer idUsuario;
    private static String nombre;
    private static String login;

    private Sesion() {
    }

    /** Revisa usuario y contraseña en la tabla Usuario. Devuelve true si son correctos. */
    public static boolean iniciar(String usuario, String contrasena) throws SQLException {
        //la consulta busca un usuario cuyo login y contrasena coincidan
        //en la tabla las contrasenas no se guardan tal cual si no convertidas en sha.....
        String sql = "SELECT ID_Usuario, Nombre_Usuario, Login_Usuario FROM Usuario "
                + "WHERE Login_Usuario = ? AND Contrasena_Usuario = SHA2(?, 256)";
        try (PreparedStatement ps = Conexion.obtener().prepareStatement(sql)) {
            ps.setString(1, usuario);//llena el hueco con usuario
            ps.setString(2, contrasena);//llena el hueco con contrasena
            //guarda el resultado en un ResultSet,que es como una tabla en memoria con los renglones que devolvio el select
            try (ResultSet resultado = ps.executeQuery()) {
                if (resultado.next()) {//intenta pasar al primer renglon,si existe devuelve true osea que encontro un usuario con ese login y contra,
                    //si no existe devuelve false,osea que no encontro ningun usuario con ese login y contra
                    idUsuario = resultado.getInt("ID_Usuario");
                    nombre = resultado.getString("Nombre_Usuario");
                    login = resultado.getString("Login_Usuario");
                    return true;//si no entro al if el login fallo
                }
            }
        }
        return false;
    }

    public static void cerrar() {
        idUsuario = null;
        nombre = null;
        login = null;
    }

    public static Integer getIdUsuario() {
        return idUsuario;
    }

    public static String getNombre() {
        return nombre;
    }

    public static String getLogin() {
        return login;
    }
}
