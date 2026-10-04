#!/usr/bin/env bash
set -e
echo "Compilando o analisador lexico (AFND)..."
javac -encoding UTF-8 -d . AnalisadorLexicoAFND.java
echo "Compilacao concluida com sucesso."
