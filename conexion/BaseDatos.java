//el ayudante de la base de datos: aqui se escribe UNA sola vez el try/catch, el PreparedStatement y el llenado de los ?
//asi el resto del programa solo escribe el SQL y los valores,sin repetir todo lo demas
//
//  consultar("SELECT ID_Estado, Nombre_Estado FROM Estado WHERE ID_Estado > ?", 3)  -> lista de renglones
//  existe("SELECT 1 FROM Jugador WHERE ID_Jugador = ?", 7)                          -> true o false
//  ejecutar("DELETE FROM Estado WHERE ID_Estado = ?", 5)                            -> renglones afectados
//
//los metodos lanzan SQLException: quien los llama decide como avisar el error (cada pantalla lo traduce a un mensaje)

package conexion;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BaseDatos {

    private BaseDatos() {
    }

    //ejecuta un SELECT y regresa todos sus renglones,cada renglon es un arreglo con los valores de sus columnas
    //los ? se llenan con los valores en el mismo orden
    public static List<Object[]> consultar(String sql, Object... valores) throws SQLException {
        try (PreparedStatement ps = preparar(sql, valores); ResultSet resultado = ps.executeQuery()) {
            int columnas = resultado.getMetaData().getColumnCount();
            List<Object[]> renglones = new ArrayList<>();
            while (resultado.next()) {
                Object[] fila = new Object[columnas];
                for (int i = 0; i < columnas; i++) {
                    fila[i] = resultado.getObject(i + 1);//null si la celda esta vacia
                }
                renglones.add(fila);
            }
            return renglones;
        }
    }

    //true si el SELECT regresa al menos un renglon
    public static boolean existe(String sql, Object... valores) throws SQLException {
        try (PreparedStatement ps = preparar(sql, valores); ResultSet resultado = ps.executeQuery()) {
            return resultado.next();
        }
    }

    //ejecuta un INSERT, UPDATE o DELETE y regresa cuantos renglones cambio
    public static int ejecutar(String sql, Object... valores) throws SQLException {
        try (PreparedStatement ps = preparar(sql, valores)) {
            return ps.executeUpdate();
        }
    }

    //prepara la consulta y llena los huecos ? con los valores
    private static PreparedStatement preparar(String sql, Object[] valores) throws SQLException {
        PreparedStatement ps = Conexion.obtener().prepareStatement(sql);
        try {
            for (int i = 0; i < valores.length; i++) {
                ps.setObject(i + 1, valores[i]);
            }
            return ps;
        } catch (SQLException e) {
            ps.close();
            throw e;
        }
    }
}
