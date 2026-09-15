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

EXPOSE 8080
ENV JAVA_OPTS=""
USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
