FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
RUN ./mvnw -B --no-transfer-progress verify

FROM eclipse-temurin:25-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system fixhub \
    && useradd --system --gid fixhub --no-create-home fixhub

COPY --from=build --chown=fixhub:fixhub \
    /workspace/target/backend-0.0.1-SNAPSHOT.jar /app/backend-0.0.1-SNAPSHOT.jar

USER fixhub

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/backend-0.0.1-SNAPSHOT.jar"]
