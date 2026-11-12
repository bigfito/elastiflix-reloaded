# Imagen de Elastiflix-Movies Java para Cloud Run.
#
# Etapa 1: compila con el Maven Wrapper y corre ./mvnw verify (pruebas unitarias, ArchUnit, Checkstyle y PMD).
# Si algo falla, la imagen no se construye. Las pruebas de integración no corren aquí: necesitan Elasticsearch.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY config/ config/
COPY elasticsearch/ elasticsearch/
# Las pruebas unitarias validan los juicios y los mínimos del harness de relevancia; no pasan a la etapa 2.
COPY eval/ eval/
COPY src/ src/
RUN ./mvnw -B verify && cp target/elastiflix-reloaded-*.jar /workspace/app.jar

# Etapa 2: solo el JRE y el jar, con un usuario sin privilegios.
# Las credenciales de Elasticsearch llegan por variables de entorno (en Cloud Run, desde Secret Manager);
# la imagen no lleva .env.
FROM eclipse-temurin:25-jre
RUN groupadd --system elastiflix && useradd --system --gid elastiflix --home-dir /app elastiflix
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER elastiflix
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
