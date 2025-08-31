FROM gradle:8.9-jdk17-alpine AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache yt-dlp ffmpeg python3
COPY --from=build /workspace/build/libs/*.jar /app/
RUN rm -f /app/*-plain.jar && \
    mv /app/*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
