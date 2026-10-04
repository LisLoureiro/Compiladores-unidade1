@echo off
chcp 65001 >nul
echo Compilando o analisador lexico (AFND)...
javac -encoding UTF-8 -d . TipoToken.java Token.java AnalisadorLexicoAFND.java
if errorlevel 1 (
    echo.
    echo Falha na compilacao. Verifique se o JDK esta instalado (javac no PATH).
    exit /b 1
)
echo Compilacao concluida com sucesso.
