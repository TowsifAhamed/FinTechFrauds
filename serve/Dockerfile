# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
RUN mvn -B -pl serve -am -DskipTests=true clean package

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends libgomp1 \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -r -u 10001 appuser
USER appuser

COPY --from=build /app/serve/target/*.jar /app/app.jar

ENV SERVER_PORT=8080 \
    REDIS_HOST=redis \
    REDIS_PORT=6379 \
    FINTECHFRAUDS_RATELIMITS_CAPACITY=100 \
    FINTECHFRAUDS_RATELIMITS_REFILLTOKENS=100 \
    FINTECHFRAUDS_RATELIMITS_REFILLPERIODSECONDS=60 \
    FINTECHFRAUDS_SECURITY_REQUESTTIMESKEWSECONDS=300

EXPOSE 8080

ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
