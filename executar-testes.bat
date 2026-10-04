@echo off
rem ============================================================
rem  Roda todos os arquivos de teste da pasta "testes".
rem ============================================================
chcp 65001 >nul
setlocal enabledelayedexpansion
for %%F in (testes\*.txt) do (
    echo ============================================================
    echo Arquivo: %%F
    echo ============================================================
    java -Dfile.encoding=UTF-8 AnalisadorSintatico "%%F"
    echo.
)
endlocal
pause
