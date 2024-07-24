FROM openjdk:17

ARG JAR_FILE=app.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=local", "app.jar"]
