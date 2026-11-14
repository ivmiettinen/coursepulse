FROM eclipse-temurin:26.0.2.1_1-jdk-noble AS build
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN sh ./gradlew --no-daemon bootJar

FROM eclipse-temurin:26.0.2.1_1-jre-noble AS runtime
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --uid 10001 --user-group --no-create-home coursepulse
COPY --from=build --chown=10001:10001 /app/build/libs/coursepulse-*.jar ./app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
