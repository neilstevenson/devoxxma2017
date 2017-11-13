FROM openjdk:8-jre

# Add our jar to the container as "app.jar"
ADD *.jar /app.jar

EXPOSE 5701

ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar