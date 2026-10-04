@echo off
chcp 65001 >nul
echo ============================================================
echo  Executando todos os testes de C:\...\analisador-lexico-1unidade\testes
echo ============================================================
for %%f in (testes\*.txt) do (
    echo.
    echo ------------------------------------------------------------
    echo Arquivo: %%f
    echo ------------------------------------------------------------
    java -Dfile.encoding=UTF-8 AnalisadorLexicoAFND "%%f"
    if errorlevel 1 (echo [resultado] ERRO LEXICO detectado) else (echo [resultado] OK)
)
echo.
echo Fim dos testes.
