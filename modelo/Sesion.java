//sesion es la clase que recuerda quien inicio sesion mientras la app este abierta
//sirve oara validar el login y la contrasena cobtra mi tabla usuario
//y cada que un usuario modiffica algo deja su id en la modificacion


package modelo;

import conexion.BaseDatos;

import java.sql.SQLException;
import java.util.List;


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
        List<Object[]> encontrados = BaseDatos.consultar(
                "SELECT ID_Usuario, Nombre_Usuario, Login_Usuario FROM Usuario "
                        + "WHERE Login_Usuario = ? AND Contrasena_Usuario = SHA2(?, 256)", usuario, contrasena);
        if (encontrados.isEmpty()) {
            return false;//no hay ningun usuario con ese login y contrasena
        }
        Object[] renglon = encontrados.get(0);
        idUsuario = ((Number) renglon[0]).intValue();
        nombre = (String) renglon[1];
        login = (String) renglon[2];
        return true;
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
