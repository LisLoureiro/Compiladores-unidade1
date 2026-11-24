#!/usr/bin/env bash
# Roda todos os arquivos de teste da pasta "testes" e confere o veredito.
# O esperado é deduzido do nome do arquivo: *valido*, *erro_lexico*
# ou *erro_sintatico*/*erro_fim*. Os trechos usados para detectar o obtido
# são ASCII para não dependerem da codificação do terminal.
total=0
ok=0
falhas=0

for f in testes/*.txt; do
    [ -e "$f" ] || continue
    nome=$(basename "$f")

    esperado=desconhecido
    case "$nome" in
        *valido*)                    esperado=valido ;;
        *erro_lexico*)               esperado=lexico ;;
        *erro_sintatico*|*erro_fim*) esperado=sintatico ;;
    esac

    saida=$(java -Dfile.encoding=UTF-8 AnalisadorSintatico "$f" 2>&1)

    obtido=desconhecido
    case "$saida" in
        *"sintaticamente V"*)   obtido=valido ;;
        *"interrompida"*)       obtido=lexico ;;
        *"sintaticamente INV"*) obtido=sintatico ;;
    esac

    total=$((total + 1))
    if [ "$esperado" = "$obtido" ]; then
        ok=$((ok + 1))
        printf 'OK    %-45s (%s)\n' "$nome" "$obtido"
    else
        falhas=$((falhas + 1))
        printf 'FALHA %-45s (esperado: %s, obtido: %s)\n' "$nome" "$esperado" "$obtido"
        printf '%s\n' "$saida" | tail -n 5
    fi
done

echo "------------------------------------------------------------"
echo "Resultado: $ok/$total testes OK"

if [ "$total" -eq 0 ]; then
    echo "Nenhum arquivo de teste encontrado em testes/."
    exit 1
fi

[ "$falhas" -eq 0 ]
