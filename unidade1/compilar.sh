#!/usr/bin/env bash
set -e
echo "Compilando o analisador lexico (AFD)..."
javac -encoding UTF-8 -d . AnalisadorLexicoAFD.java
echo "Compilacao concluida com sucesso."
