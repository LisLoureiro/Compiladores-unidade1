@echo off
rem ============================================================
rem  Compila o analisador lexico/sintatico (linguagem Sigma).
rem  Requer um JDK instalado (comando javac). No Linux/macOS use
rem  o script compilar.sh.
rem ============================================================
javac -encoding UTF-8 -d . *.java
if errorlevel 1 (
    echo.
    echo Falha na compilacao.
) else (
    echo.
    echo Compilacao concluida com sucesso.
)
