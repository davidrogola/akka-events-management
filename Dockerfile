
FROM openjdk:8-jdk-alpine
# ----
MAINTAINER David Ogola <davidrogola@gmail.com>

ARG JAR_FILE
 #Add the application's jar to the container
ADD ${JAR_FILE} akka-events-booking-java.jar
# Run the jar file
ENTRYPOINT ["java", "-jar", "akka-events-booking-java.jar"]
