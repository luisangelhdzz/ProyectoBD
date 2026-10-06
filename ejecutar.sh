#!/bin/sh
# Compila y abre la aplicación. Uso:  sh ejecutar.sh
cd "$(dirname "$0")"
CONECTOR=mysql-connector-j-26.7.0.jar

javac -encoding UTF-8 -cp "$CONECTOR" -d out \
    Main.java conexion/*.java modelo/*.java vista/*.java vista/tablas/*.java \
  && java -cp "out:$CONECTOR" Main
