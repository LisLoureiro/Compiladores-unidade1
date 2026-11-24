@echo off
rem ============================================================
rem  Executa o analisador sintatico SLR(1) da linguagem Sigma.
rem  Exemplos:
rem     executar.bat exemplo.txt
rem     executar.bat testes\01_valido_basico.txt --tokens
rem     executar.bat --tabela
rem  (configure o console para UTF-8 para exibir os acentos)
rem ============================================================
chcp 65001 >nul
java -Dfile.encoding=UTF-8 AnalisadorSintatico %*
pause
