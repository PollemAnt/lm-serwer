
FROM gradle:8.5-jdk17 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle shadowJar --no-daemon


FROM eclipse-temurin:17-jre
COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
