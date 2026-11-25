//dependencias busca si un registro esta ligado por llave foranea con registros de otras tablas
//sirve para avisar ANTES de eliminar: todas las llaves foraneas son ON DELETE RESTRICT,
//asi que MySQL no deja borrar un registro padre que todavia tiene hijos (error 1451)
//ej. no se puede borrar el estado Coahuila si todavia hay ciudades de Coahuila

package modelo;


import conexion.BaseDatos;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Dependencias {

    private Dependencias() {
    }

    //tabla y columna = la llave del registro que se quiere borrar,ej. "Estado","ID_Estado"
    //valor = su valor,ej. 1
    //regresa un renglon por cada tabla que tiene registros ligados,ej. "- Ciudad: 2 registros"
    //o null si no hay ninguno y si se puede eliminar
    public static String buscar(String tabla, String columna, Object valor) throws SQLException {
        //1. que tablas tienen una llave foranea que apunta a esta tabla y columna
        //information_schema es la base donde MySQL guarda la descripcion de todas las bases: tablas,columnas,llaves...
        //KEY_COLUMN_USAGE tiene un renglon por cada columna de cada llave,y en las foraneas dice a que tabla apuntan
        //se traen todas las llaves foraneas de la base y se escogen aqui con equalsIgnoreCase,
        //porque en Mac MySQL guarda algunos nombres en minusculas (ej. "estado" en vez de "Estado")
        List<Object[]> llaves = BaseDatos.consultar("SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME "
                + "FROM information_schema.KEY_COLUMN_USAGE "
                + "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL");

        //2. en cada tabla hija cuenta cuantos registros tienen ese valor en su llave foranea
        //se suman por tabla porque una tabla puede apuntar dos veces a la misma (ej. Partido: equipo local y visitante)
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (Object[] llave : llaves) {
            if (llave[2].toString().equalsIgnoreCase(tabla) && llave[3].toString().equalsIgnoreCase(columna)) {
                List<Object[]> total = BaseDatos.consultar(
                        "SELECT COUNT(*) FROM `" + llave[0] + "` WHERE `" + llave[1] + "` = ?", valor);
                int cuantos = ((Number) total.get(0)[0]).intValue();
                if (cuantos > 0) {
                    String nombre = conMayuscula(llave[0].toString());
                    conteo.put(nombre, conteo.getOrDefault(nombre, 0) + cuantos);
                }
            }
        }
        if (conteo.isEmpty()) {
            return null;
        }

        //3. arma el texto para el aviso
        StringBuilder texto = new StringBuilder();
        for (Map.Entry<String, Integer> e : conteo.entrySet()) {
            if (texto.length() > 0) {
                texto.append("\n");
            }
            texto.append("- ").append(e.getKey()).append(": ").append(e.getValue())
                    .append(e.getValue() == 1 ? " registro" : " registros");
        }
        return texto.toString();
    }

    //"ciudad" -> "Ciudad",por si MySQL regreso el nombre en minusculas
    private static String conMayuscula(String nombre) {
        return nombre.isEmpty() ? nombre : Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
    }
}
