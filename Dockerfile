#How to publish on Docker HUB
# 1) docker build -t vaimeedock/sepa:latest -f Dockerfile .
# 2) docker login -u YOUR-USER-NAME.
# Build command on Apple M1: docker buildx build --platform linux/amd64 --push -t vaimeedock/engine .
# MULTIPLE PUSH
# docker build -t vaimeedock/sepa:v0.15.0 -t vaimeedock/sepa:latest . 
# docker push vaimeedock/sepa --all-tag

FROM maven:3.6-jdk-11 as BUILD
COPY . .

ENV  JMX_HOSTNAME=0.0.0.0
ENV  JMX_PORT=7090

RUN mvn clean package

FROM openjdk:11.0-jre

COPY --from=BUILD ./engine/target/engine-1.0.0-SNAPSHOT.jar /engine.jar
COPY --from=BUILD ./engine/src/main/resources/jmxremote.password /jmxremote.password
COPY --from=BUILD ./engine/src/main/resources/jmxremote.access /jmxremote.access
COPY --from=BUILD ./engine/src/main/resources/jmx.properties /jmx.properties
COPY --from=BUILD ./engine/src/main/resources/endpoint.jpar /endpoint.jpar
# COPY ALL ENDPOINTS TO ALLOW CMD LINE CUSTOMIZATION
COPY --from=BUILD ./engine/src/main/resources/endpoints /endpoints

RUN chmod 600 /jmxremote.password

EXPOSE 8000
EXPOSE 9000
EXPOSE ${JMX_PORT}

ENTRYPOINT java -Djava.rmi.server.hostname=${JMX_HOSTNAME} -Dcom.sun.management.jmxremote.port=${JMX_PORT} -Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT} -Dcom.sun.management.config.file=jmx.properties -jar engine.jar
