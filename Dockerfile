# Imagem do analisador sintático SLR(1) da linguagem Sigma.
# O código é compatível com Java 8; --release 8 garante que os .class gerados
# também rodem em um JRE 8, mesmo compilando com o JDK do container.
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY *.java ./
RUN javac --release 8 -encoding UTF-8 *.java
COPY testes ./testes
COPY exemplo.txt ./
ENTRYPOINT ["java", "-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8", "AnalisadorSintatico"]
