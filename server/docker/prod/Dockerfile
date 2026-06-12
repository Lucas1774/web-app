FROM eclipse-temurin:25-jre
RUN useradd -m appuser
WORKDIR /home/appuser
COPY --chown=appuser:appuser server.jar server.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "server.jar"]
