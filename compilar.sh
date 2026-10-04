#!/usr/bin/env bash
set -e
echo "Compilando o analisador lexico (AFND)..."
javac -encoding UTF-8 -d . TipoToken.java Token.java AnalisadorLexicoAFND.java
echo "Compilacao concluida com sucesso."
