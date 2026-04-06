FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/*.jar app.jar
RUN addgroup --system app && adduser --system --ingroup app app \
    && chown -R app:app /app
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
