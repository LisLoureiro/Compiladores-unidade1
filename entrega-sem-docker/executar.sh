#!/usr/bin/env bash
# Executa o analisador sintático SLR(1) da linguagem Sigma.
# Exemplos:
#   ./executar.sh exemplo.txt
#   ./executar.sh testes/01_valido_basico.txt --tokens
#   ./executar.sh --tabela
java -Dfile.encoding=UTF-8 AnalisadorSintatico "$@"
