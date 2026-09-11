# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies separately from source for faster rebuilds
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user
RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=build /build/target/smart-tech-backend-*.jar app.jar

# Persisted upload directory (matches file.upload.dir / storage.local.base-directory)
RUN mkdir -p /app/uploads && chown -R spring:spring /app
VOLUME ["/app/uploads"]

USER spring
EXPOSE 9090

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
