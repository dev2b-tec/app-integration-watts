# ─── Stage 1: build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY app/pom.xml ./
RUN mvn dependency:go-offline -q

COPY app/src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: runner ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN addgroup --system --gid 1001 spring && \
    adduser  --system --uid 1001 spring

COPY --from=builder /app/target/*.jar app.jar

USER spring
EXPOSE 8012

ENTRYPOINT ["java", "-jar", "app.jar"]
