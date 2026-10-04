#!/usr/bin/env bash
echo "============================================================"
echo " Executando todos os testes em testes/"
echo "============================================================"
for f in testes/*.txt; do
    echo
    echo "------------------------------------------------------------"
    echo "Arquivo: $f"
    echo "------------------------------------------------------------"
    java -Dfile.encoding=UTF-8 AnalisadorLexicoAFND "$f"
    if [ $? -eq 0 ]; then
        echo "[resultado] OK"
    else
        echo "[resultado] ERRO LEXICO detectado"
    fi
done
echo
echo "Fim dos testes."
