FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Dependencies resolve in their own layer, keyed only on the pom. A source-only change then reuses
# the cached layer instead of re-downloading the entire dependency tree on every build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# An application that never needs to write outside /tmp has no reason to run as uid 0.
RUN addgroup -S app && adduser -S -G app app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app

# Matches server.port=${PORT:8081}. Documentation only — the platform's injected PORT still wins.
EXPOSE 8081

# Fails fast and loudly if the prod profile's required secrets are missing, rather than starting
# with the development fallbacks baked into application.properties.
ENV SPRING_PROFILES_ACTIVE=prod

# Container-aware heap sizing: the JVM reads the cgroup limit rather than the host's total RAM.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
