#!/usr/bin/env bash
# Compila o analisador léxico/sintático da linguagem Sigma.
# Requer um JDK instalado (comando javac).
set -e
javac -encoding UTF-8 -d . ./*.java
echo "Compilação concluída com sucesso."
