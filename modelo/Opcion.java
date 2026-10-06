package modelo;

import java.util.Objects;//clase con utiilidades para comparara valores que pueden ser null sin que truene el programa

//opcion es cada renglon de la lista despegable de una llave foranea,
//junta en un solo objeto el numero que se guarda en mysql y el texto que ve el usuario
public class Opcion {
    //sirve para que la lista no arranque con algo ya elegido por accidente
    public static final Opcion NINGUNA = new Opcion(null, "-- Selecciona --");

    private final Integer id;//el numero que se manda a mysql
    private final String texto;//lo que se muestra en la lista despegable,ej."NOMBRE DEL ESTADO"

    public Opcion(Integer id, String texto) {
        this.id = id;
        this.texto = texto;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return texto;
    }

    //cuando dos opciones son la misma
    //por defec to java considera iguales dos objeto si sson exactamente el mismo objeto en memoria
    //aqui cambia la regla ,dos opciones son iguales i tienen el mismo id sin importar el texto
    @Override
    public boolean equals(Object o) {
        //lo que me pasaron es un opcion?,si no.son disitintas
        //convierte o a opcion para poder leer su id
        //compara los dos id sin tronar si alguno es null
        return o instanceof Opcion && Objects.equals(id, ((Opcion) o).id);
    }//sirve para editar un registro


//si redefines equals debes redefinir hashCode para que dos objetos iguales tengan el mismo hashcode
    //sirve para que un HashSet o HashMap pueda encontrar un objeto en su interior
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

//hashmap=un diciionario ,guarda parecjas clave,datos
//hasset=guarda elementos sueltos sin duplicados y sin orden fijo


//flujo de trabajo;
//en la base de datos,por ejemplo tenemos la tabla Estado con su id y nombre
//para llenar la lista,la app lee la tabla con un select
//por cada renglon de la tabla crea un objeto opcion que guarda juntos el numero y el nombre,primero va ninguna que no tiene numero
//mete esas opciones a la listadespegable,para saber que texto esccribir la lista llamas a toString() que devuelve solo le nombre
//el usuario solo ve nombres,nunca numeros
//al guardar, la app le pregunta a la opcion elegida su numero con getId()