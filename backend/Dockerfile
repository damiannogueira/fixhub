FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
RUN ./mvnw -B --no-transfer-progress verify

FROM eclipse-temurin:25-jre-jammy AS runtime

WORKDIR /app

RUN test -x "$JAVA_HOME/bin/keytool" \
    && "$JAVA_HOME/bin/keytool" -help >/dev/null 2>&1 \
    && groupadd --system fixhub \
    && (getent group 1000 >/dev/null || groupadd --gid 1000 render-secrets) \
    && useradd --system --gid fixhub --groups 1000 --no-create-home fixhub

COPY --from=build --chown=fixhub:fixhub \
    /workspace/target/backend-0.0.1-SNAPSHOT.jar /app/backend-0.0.1-SNAPSHOT.jar

COPY --chown=fixhub:fixhub docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 0555 /app/docker-entrypoint.sh

USER fixhub

EXPOSE 10000

ENTRYPOINT ["/app/docker-entrypoint.sh"]
