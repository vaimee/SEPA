#How to publish on Docker HUB
# 1) docker build -t vaimeedock/sepa:latest -f Dockerfile .
# 2) docker login -u YOUR-USER-NAME.
# Build command on Apple M1: docker buildx build --platform linux/amd64 --push -t vaimeedock/engine .
FROM maven:3.6-jdk-13 as BUILD
COPY . .

RUN mvn clean package

FROM openjdk:13-jdk-alpine

COPY --from=BUILD ./engine/target/engine-1.0.0-SNAPSHOT.jar /engine.jar
COPY --from=BUILD ./engine/src/main/resources/jmxremote.password /jmxremote.password
COPY --from=BUILD ./engine/src/main/resources/jmxremote.access /jmxremote.access
COPY --from=BUILD ./engine/src/main/resources/jmx.properties /jmx.properties
COPY --from=BUILD ./engine/src/main/resources/endpoint.jpar /endpoint.jpar


RUN chmod 600 /jmxremote.password

EXPOSE 8000
EXPOSE 9000
EXPOSE 7091

ENV  JMX_HOSTNAME=0.0.0.0

ENTRYPOINT java -Djava.rmi.server.hostname=${JMX_HOSTNAME} -Dcom.sun.management.config.file=jmx.properties -jar engine.jar
