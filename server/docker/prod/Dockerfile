# Stage 1: build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: run
FROM eclipse-temurin:21-jre
RUN useradd -m appuser
WORKDIR /home/appuser
COPY --chown=appuser:appuser --from=build /app/target/server.jar server.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","server.jar"]
