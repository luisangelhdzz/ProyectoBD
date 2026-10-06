//esta clase es la puerta de entrada a la base de datos
package conexion;//la clase vive en el paquete conexion(la carpeta conexion/)

import java.sql.Connection;//representa una conexion abierta con MySql
import java.sql.DriverManager;//la"fabrica que crea esa conexion a partir de una url,usuario y contrasena
import java.sql.SQLException;//el erro que lanza si algo fallo

//final significa que nadie puede heredar de ella
public final class Conexion {

    //mySql corre en local en mi maquina
    public static final String HOST = "localhost";
    public static final int PUERTO = 3306;
    //esquema con el que se conecta
    public static final String BASE_DATOS = "BD_LIGA_BASEBALL";
    //usuario de mysql que usa la aplicacion no es el usuario que inicia sesion e tu programa
    public static final String USUARIO_BD = "APP_LIGA";
    public static final String CLAVE_BD = "Liga2026";
    //url de conexion a la base de datos, con parametros para que no de error al conectarse
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + BASE_DATOS
            + "?useSSL=false&allowPublicKeyRetrieval=true&yearIsDateType=false";
    //conexion guardada en memoria, para que no se abra y cierre cada vez que se necesite
    private static Connection conexion;
    //constructor privado,nadie puede hacer new Conexion(),se usa solo con los metodos estaticos
    private Conexion() {
    }

    //si nunca se ah abierto o sea esta null o ya se cerro se abre una nueva con DriveManager
    //si ya esta abierta devuelve la misma
    public static Connection obtener() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(URL, USUARIO_BD, CLAVE_BD);
        }
        return conexion;
    }

    //cierra la conexion si esta abierta ignora cualquier error al cerrar y la deja en null,se llama al salir de la aplicacion
    public static void cerrar() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException ignored) {
        }
        conexion = null;
    }
}

//para que funcione necesita el driver MYSQL CONNECTOR mysql-connector-j-x.x.x.jar) agregado a las librerías del proyecto; si falta, verás "No suitable driver found".
//
