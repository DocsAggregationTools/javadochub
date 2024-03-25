FROM eclipse-temurin:8-jdk-alpine
VOLUME /tmp
COPY target/javadochub.jar javadochub.jar
COPY target/classes/application-prod.yml application.yml
#COPY bin/start.sh start.sh
#COPY bin/stop.sh stop.sh
ENTRYPOINT ["java", "-jar", "/javadochub.jar"]
