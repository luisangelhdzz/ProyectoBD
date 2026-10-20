#!/bin/sh
# Compila y abre la aplicación. Uso:  sh ejecutar.sh
cd "$(dirname "$0")"

CONECTOR=mysql-connector-j-26.7.0.jar
# Busca el conector en Lib/ o en la raíz del proyecto
if [ -f "Lib/$CONECTOR" ]; then
    CONECTOR="Lib/$CONECTOR"
fi

if [ ! -f "$CONECTOR" ]; then
    echo "No encuentro $CONECTOR (ni en Lib/ ni en la raíz)."
    exit 1
fi

# En Windows (Git Bash) el separador del classpath es ';', en Linux y macOS es ':'
SEP=":"
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
esac

javac -encoding UTF-8 -cp "$CONECTOR" -d out \
    Main.java conexion/*.java modelo/*.java vista/*.java vista/tablas/*.java \
  && java -cp "out${SEP}$CONECTOR" Main