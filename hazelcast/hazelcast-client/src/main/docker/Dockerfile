FROM openjdk:8-jre

# Add our jar to the container as "app.jar"
ADD *.jar /app.jar

ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar