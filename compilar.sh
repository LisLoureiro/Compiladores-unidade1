#!/usr/bin/env bash
# Compila o analisador léxico/sintático da linguagem Sigma.
# Requer um JDK instalado (comando javac).
set -e
# Usa --release 8 quando o JDK suporta, para manter o bytecode
# compatível com Java 8 (JDK 9+).
if javac --help 2>&1 | grep -q -- "--release"; then
    javac --release 8 -encoding UTF-8 -d . ./*.java
else
    javac -encoding UTF-8 -d . ./*.java
fi
echo "Compilação concluída com sucesso."
