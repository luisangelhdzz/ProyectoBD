//ayudas para las llaves primarias de las tablas
//siguienteLibre: el ID que se asigna solo cuando no escribes uno. Es el mas chico libre a partir del 1
//  solo 1 -> 2 | 1 y 4 -> 2 | 2 y 3 -> 1 | 1, 2 y 3 -> 4
//esPadre: true si otra tabla tiene una llave foranea que apunta a esta. Una tabla padre no debe cambiar su ID

package modelo;

import conexion.BaseDatos;

import java.sql.SQLException;
import java.util.List;

public final class Ids {

    private Ids() {
    }

    //recorre los IDs de menor a mayor y avanza mientras encuentre el numero que sigue sin saltos
    //en cuanto aparece un hueco (un ID mayor que el esperado) ese es el libre
    public static int siguienteLibre(String tabla, String columna) throws SQLException {
        List<Object[]> renglones = BaseDatos.consultar("SELECT " + columna + " FROM " + tabla + " ORDER BY " + columna);
        int esperado = 1;
        for (Object[] renglon : renglones) {
            int id = ((Number) renglon[0]).intValue();
            if (id == esperado) {
                esperado++;
            } else if (id > esperado) {
                break;
            }
        }
        return esperado;
    }

    //si no se puede consultar se regresa false: la tabla se trata como hija y el ID sigue editable
    public static boolean esPadre(String tabla) {
        try {
            return BaseDatos.existe("SELECT 1 FROM information_schema.KEY_COLUMN_USAGE "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL "
                    + "AND LOWER(REFERENCED_TABLE_NAME) = LOWER(?)", tabla);
        } catch (SQLException e) {
            return false;
        }
    }
}
