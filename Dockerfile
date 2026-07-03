# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven \
    && mvn -B -DskipTests package \
    && mv target/saltmarsh-*.jar target/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S saltmarsh && adduser -S saltmarsh -G saltmarsh
COPY --from=build /workspace/target/app.jar /app/app.jar
USER saltmarsh
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
