//pinta las celdas de UNA columna de la tabla segun su valor
//se escribe una sola vez y se reutiliza en todas las tablas: cada tabla solo dice la columna,el color y la regla
//
//  ej. en el constructor de una tabla:  colorear("Estatus", new Color(198, 239, 206), "Activo");
//      colorear("Estatus", new Color(255, 199, 206), v -> v.equals("Baja"));

package vista.tablas;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ColorColumna extends DefaultTableCellRenderer {

    //las reglas: si el valor cumple la condicion,la celda se pinta de ese color
    //se revisan en orden y gana la primera que se cumple
    private final Map<Predicate<String>, Color> reglas = new LinkedHashMap<>();

    public ColorColumna agregar(Predicate<String> cuando, Color color) {
        reglas.put(cuando, color);
        return this;
    }

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                   boolean enfoque, int fila, int columna) {
        Component celda = super.getTableCellRendererComponent(tabla, valor, seleccionada, enfoque, fila, columna);
        //la fila seleccionada conserva el color de seleccion de la tabla para que se note cual esta elegida
        if (!seleccionada) {
            celda.setBackground(tabla.getBackground());
            String texto = valor == null ? "" : valor.toString();
            for (Map.Entry<Predicate<String>, Color> regla : reglas.entrySet()) {
                if (regla.getKey().test(texto)) {
                    celda.setBackground(regla.getValue());
                    break;
                }
            }
        }
        return celda;
    }
}
