#!/usr/bin/env bash
echo "============================================================"
echo " Executando todos os testes em testes/"
echo "============================================================"
mkdir -p saida
for f in testes/*.txt; do
    nome=$(basename "$f" .txt)
    echo
    echo "------------------------------------------------------------"
    echo "Arquivo: $f"
    echo "------------------------------------------------------------"
    java -Dfile.encoding=UTF-8 AnalisadorLexicoAFD "$f" "saida/$nome.txt"
    if [ $? -eq 0 ]; then
        echo "[resultado] OK"
    else
        echo "[resultado] ERRO LEXICO detectado"
    fi
done
echo
echo "Fim dos testes."
