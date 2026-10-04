#!/usr/bin/env bash
# Roda todos os arquivos de teste da pasta "testes".
for f in testes/*.txt; do
    echo "============================================================"
    echo "Arquivo: $f"
    echo "============================================================"
    java -Dfile.encoding=UTF-8 AnalisadorSintatico "$f"
    echo
done
