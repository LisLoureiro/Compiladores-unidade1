@echo off
rem ============================================================
rem  Roda todos os arquivos de teste da pasta "testes" e confere
rem  o veredito esperado, deduzido do nome do arquivo.
rem  Os trechos usados na comparacao sao ASCII para nao dependerem
rem  da codificacao do console.
rem ============================================================
chcp 65001 >nul
setlocal enabledelayedexpansion

set /a total=0
set /a ok=0
set /a falhas=0
set "saida=%TEMP%\sigma_saida_teste.txt"

for %%F in (testes\*.txt) do (
    set "nome=%%~nxF"
    set "esperado=desconhecido"
    echo !nome! | findstr /i /c:"valido" >nul && set "esperado=valido"
    echo !nome! | findstr /i /c:"erro_lexico" >nul && set "esperado=lexico"
    echo !nome! | findstr /i /c:"erro_sintatico" >nul && set "esperado=sintatico"
    echo !nome! | findstr /i /c:"erro_fim" >nul && set "esperado=sintatico"

    java -Dfile.encoding=UTF-8 AnalisadorSintatico "%%F" > "!saida!" 2>&1

    set "obtido=desconhecido"
    findstr /c:"sintaticamente V" "!saida!" >nul && set "obtido=valido"
    findstr /c:"interrompida" "!saida!" >nul && set "obtido=lexico"
    findstr /c:"sintaticamente INV" "!saida!" >nul && set "obtido=sintatico"

    set /a total+=1
    if "!esperado!"=="!obtido!" (
        set /a ok+=1
        echo OK    !nome! ^(!obtido!^)
    ) else (
        set /a falhas+=1
        echo FALHA !nome! ^(esperado: !esperado!, obtido: !obtido!^)
        type "!saida!"
    )
)

echo ------------------------------------------------------------
echo Resultado: !ok!/!total! testes OK

del "!saida!" >nul 2>nul
pause
if not "!falhas!"=="0" (
    endlocal
    exit /b 1
)
endlocal
