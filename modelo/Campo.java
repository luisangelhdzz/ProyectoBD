//esta clase campo es una descripcion de una columna de una tabla,no guarda datos,solo guarda infrormacion sobre la columna

package modelo;//vive en la carpeta modelo/ junto con sesion y las demas clases del modelo

public class Campo {

    //un enum es una lista cerrada de valores posibles,cada campo es de uno de estos cinco tipos y la pantalla decide con eso que control mostrar
    public enum Tipo { ENTERO, DECIMAL, TEXTO, FECHA, FORANEA }

    private final String columna;//nombre real en MYSQL:"NOMBRE_ESTADO"
    private final String etiqueta;//lo que ve el usuario:"NOMBRE DEL ESTADO"
    private final Tipo tipo;//ENTERO ,TEXTO,ETC

    private boolean llave;//ES PARTE DE LA LLAVE FORANEA?
    private boolean autoIncremento;//MYSQL GENERA EL NUMERO SOLO
    private boolean obligatorio = true;//POR DEFECTO TODO CAMPO ES OBLIGATORIO
    private int longitudMaxima;//PARA TEXTOS : EL 20 DE VARCHAR(20)
    private String ayuda = "";//TEXTO DE AYUDA BAJO EL CAMPO

    // Solo para llaves foráneas
    private String tablaReferencia;//TABLE PADRE ,EJEMPLO ESTADO
    private String columnaMostrar;//QUE COLUMNA MOSTRAR EN LA LISTA EJ."nOMBRE_ESTADO"


    //es private asi que nadie puede hacer new Campo() desde fuers
    private Campo(String columna, String etiqueta, Tipo tipo) {
        this.columna = columna;
        this.etiqueta = etiqueta;
        this.tipo = tipo;
    }


//es el campo id de una tabla
    public static Campo id(String columna, String etiqueta) {
        Campo c = new Campo(columna, etiqueta, Tipo.ENTERO);
        c.llave = true;
        c.autoIncremento = true;
        c.obligatorio = false;
        c.ayuda = "Déjalo vacío para que se asigne solo.";
        return c;
    }


    //es el campo nombre de una tabla
    //devuelve un objeto campo,recibe 3 datos
    //el nombre de la columna enMYSQL
    //el texto para la pantalla
    //maximo de cracateres
    //En resumen: crea un campo de tipo texto, le pone su límite de caracteres y un mensaje de ayuda que dice ese límite, y lo devuelve listo para usar.
    public static Campo texto(String columna, String etiqueta, int longitudMaxima) {
        //crea un objeto campo y lo guarda en la variable c,usa el constructor privado pasandole la columna y el tipo fijo texto
        Campo c = new Campo(columna, etiqueta, Tipo.TEXTO);
        c.longitudMaxima = longitudMaxima;//copia el 20 que recibio el metodo al atributo del objeto
        //en este punto c tiene columna="NOMBRE_ESTADO",etiqueta="NOMBRE DEL ESTADO",tipo=TEXTO,longitudMaxima=20,ayuda=""
        //arma el texto de ayuda pegando tres partes "maximo " + "20" + " caracteres." y lo guarda en el atributo ayuda del objeto c
        c.ayuda = "Máximo " + longitudMaxima + " caracteres.";
        return c;//devuelve el campo ya configurado a quien llamo el metodo
    }


    //campo para una tabla que pida enteros
    //metodo publico y estatico que recibe solo datos: el nombre de la columna y la etiqueta,no pide longitud
    public static Campo entero(String columna, String etiqueta) {
        //crea el campo con tipo entero y lo devuelbve directamente,todo en una linea no guarda el objeto en una variable c porque no hay nada que configurarle
        return new Campo(columna, etiqueta, Tipo.ENTERO);
    }


    //camopo para una tabla que pida decimales
    public static Campo decimal(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.DECIMAL);
    }


    //campo para una tabla que pida fechas,recibe el nombre de la columna y la etiqueta,devuelve un objeto campo
    //crea el campo con tipo fecha y lo guarda en c
    public static Campo fecha(String columna, String etiqueta) {
        Campo c = new Campo(columna, etiqueta, Tipo.FECHA);
        c.ayuda = "Formato AAAA-MM-DD.";//le pone un texto de ayuda fijo que indica como escribir la fecha
        return c;//devuelve el campo configurado
    }



    //con esto la app hace por dentro SELECT ID_Estado, Nombre_Estado FROM Estado;
    //metodo publico y estatico que recibe cuatro datos
    //la columna en la tabla hija
    //el texto en pantalla
    //la tabla padre donde salen las opciones
    //que coluna de la tabla padre le ensena al usuario
    public static Campo foranea(String columna, String etiqueta, String tablaReferencia, String columnaMostrar) {
        //crea el campo con tipo foranea y lo guarda en c porque aun falta configurarlo,es el que
        //le avisa al formulario que debe mostrar una lista desplegable con los datos de la tabla padre, y que columna mostrarle al usuario
        Campo c = new Campo(columna, etiqueta, Tipo.FORANEA);
        //guarda n el objeto el nombre de la tabla padre,con esto la app sabe de que tabla sacar las opciones
        c.tablaReferencia = tablaReferencia;
        //guarda que columna mostrar,asi en la lista aparecen nombres legibles en ligar de numeros
        c.columnaMostrar = columnaMostrar;
        return c;//devuelve el campo listo
    }


    /** El campo puede quedar vacío (se guarda NULL). */
    public Campo opcional() {
        this.obligatorio = false;
        return this;
    }

    /** Forma parte de la llave primaria (para llaves compuestas como Contrato). */
    public Campo comoLlave() {
        this.llave = true;
        return this;
    }

    public Campo conAyuda(String ayuda) {
        this.ayuda = ayuda;
        return this;
    }

//devuelve nombre en MYSQL
    public String getColumna() {
        return columna;
    }
//devuelve en texto pantalla
    public String getEtiqueta() {
        return etiqueta;
    }
//devuelve tipo de campo
    public Tipo getTipo() {
        return tipo;
    }
//devuelve true si es llave primaria
    public boolean esLlave() {
        return llave;
    }
//lo numera mysql?
    public boolean esAutoIncremento() {
        return autoIncremento;
    }
//no puedee quedar vacio
    public boolean esObligatorio() {
        return obligatorio;
    }
//maximo de caracteres
    public int getLongitudMaxima() {
        return longitudMaxima;
    }
//mensaje de ayuda
    public String getAyuda() {
        return ayuda;
    }
//tabla padre(foraneas)
    public String getTablaReferencia() {
        return tablaReferencia;
    }
//que mostrar en la lista
    public String getColumnaMostrar() {
        return columnaMostrar;
    }
}
