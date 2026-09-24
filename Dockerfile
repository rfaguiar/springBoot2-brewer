# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

# Build directly to avoid go-offline failing on legacy transitive metadata.
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Patch critical CVEs in base runtime packages.
RUN apk update && apk upgrade --no-cache openssl expat

# Run as non-root for better container security.
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/target/*.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# OTel Java Agent (auto-instrumentation), version pinned + checksum-verified for reproducible builds.
ADD --chown=spring:spring --checksum=sha256:bbf83c151b6400709e2f225bdd07a04f839d9d13b8b93464241333fd25d3e3ba \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.31.1/opentelemetry-javaagent.jar \
    /app/opentelemetry-javaagent.jar

EXPOSE 8080
ENV JAVA_OPTS=""
USER spring

ENTRYPOINT ["/app/docker-entrypoint.sh"]
