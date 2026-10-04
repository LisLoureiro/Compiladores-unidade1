# Imagem do analisador sintático SLR(1) da linguagem Sigma.
# O código é compatível com Java 8, mas compilamos com o JDK 21 do container.
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY *.java ./
RUN javac -encoding UTF-8 *.java
COPY testes ./testes
COPY exemplo.txt ./
ENTRYPOINT ["java", "-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8", "AnalisadorSintatico"]
