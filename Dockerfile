#How to publish on Docker HUB
# 1) docker build -t vaimeedock/sepa:latest -f Dockerfile .
# 2) docker login -u YOUR-USER-NAME.
# Build command on Apple M1: docker buildx build --platform linux/amd64 --push -t vaimeedock/sepa .
# MULTIPLE PUSH
# docker build -t vaimeedock/sepa:v0.15.0 -t vaimeedock/sepa:latest . 
# docker push vaimeedock/sepa --all-tag

#FROM maven:3.6-jdk-11 AS build
FROM maven:3.9.9-sapmachine-21 AS build
COPY . .

RUN mvn -DskipTests clean package

#FROM openjdk:11.0-jre
FROM sapmachine:21.0.6

COPY --from=build ./engine/target/engine-1.0.0-SNAPSHOT.jar /engine-1.0.0-SNAPSHOT.jar
COPY --from=build ./engine/src/main/resources/run.sh /run.sh
COPY --from=build ./engine/src/main/resources/jmxremote.password /jmxremote.password
COPY --from=build ./engine/src/main/resources/jmxremote.access /jmxremote.access
COPY --from=build ./engine/src/main/resources/jmx.properties /jmx.properties
COPY --from=build ./engine/src/main/resources/log4j2.xml /log4j2.xml
# COPY ALL ENDPOINTS TO ALLOW CMD LINE CUSTOMIZATION
COPY --from=build ./engine/src/main/resources/endpoints /endpoints

RUN chmod 600 /jmxremote.password
RUN chmod 777 /run.sh

# MUST BE SET WITH THE HOST NAME (e.g. vaimee.com , vaimee.org, ...)
ENV JMX_HOSTNAME=0.0.0.0
ENV JMX_PORT=7090

EXPOSE ${JMX_PORT}
EXPOSE 8000
EXPOSE 9000

ENTRYPOINT ["/run.sh"]
