# --- Build -------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Run ---------------------------------------------------------------------
# Official Playwright image: Java 21 + Chromium + all system libraries and fonts.
# Keep the tag in sync with <playwright.version> in pom.xml.
FROM mcr.microsoft.com/playwright/java:v1.56.0-noble
WORKDIR /app
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 \
    JAVA_OPTS="-Xmx512m"
COPY --from=build /src/target/document-utilities-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
