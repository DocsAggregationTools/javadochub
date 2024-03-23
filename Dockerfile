FROM eclipse-temurin:8-jdk-alpine
VOLUME /tmp
COPY target/javadochub.jar javadochub.jar
COPY src/main/resources/application.yml application.yml
#COPY bin/start.sh start.sh
#COPY bin/stop.sh stop.sh
ENTRYPOINT ["java", "-jar", "-Dspring.config.location=application.yml", "/javadochub.jar"]
