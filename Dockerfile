FROM loyaltyone/docker-slim-java-node:jre-8-node-8

MAINTAINER LoyaltyOne

COPY bootstrap /usr/local/bin/

ENTRYPOINT ["/usr/local/bin/env-decrypt", "/usr/local/bin/bootstrap"]

#FROM openjdk:8-jdk-alpine
# ----
#MAINTAINER David Ogola <davidrogola@gmail.com>
# Install Maven

# Add a volume pointing to /tmp
#VOLUME /tmp

# Make port 8080 available to the world outside this container
#EXPOSE 8080

# The application's jar file
#ARG JAR_FILE

# Add the application's jar to the container
#ADD ${JAR_FILE} akka-events-booking-java.jar

# Run the jar file
#ENTRYPOINT ["java","-jar","/akka-events-booking-java.jar"]
