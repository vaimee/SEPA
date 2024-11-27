#How to publish on Docker HUB
# 1) docker build -t vaimeedock/sepa:latest -f Dockerfile .
# 2) docker login -u YOUR-USER-NAME.
# Build command on Apple M1: docker buildx build --platform linux/amd64 --push -t vaimeedock/sepa .
# MULTIPLE PUSH
# docker build -t vaimeedock/sepa:v0.15.0 -t vaimeedock/sepa:latest . 
# docker push vaimeedock/sepa --all-tag

FROM maven:latest AS build
COPY . .

RUN mvn clean package

FROM eclipse-temurin:latest

COPY --from=build ./run.sh /run.sh
COPY --from=build ./engine/target/engine-1.0.0-SNAPSHOT.jar /engine.jar
COPY --from=build ./engine/src/main/resources/jmxremote.password /jmxremote.password
COPY --from=build ./engine/src/main/resources/jmxremote.access /jmxremote.access
COPY --from=build ./engine/src/main/resources/jmx.properties /jmx.properties
COPY --from=build ./engine/src/main/resources/endpoint.jpar /endpoint.jpar
# COPY ALL ENDPOINTS TO ALLOW CMD LINE CUSTOMIZATION
COPY --from=build ./engine/src/main/resources/endpoints /endpoints

RUN chmod 600 /jmxremote.password
RUN chmod 777 /run.sh

ENV JMX_HOST=0.0.0.0
ENV JMX_PORT=7099

EXPOSE 8000
EXPOSE 9000
EXPOSE ${JMX_PORT}

ENTRYPOINT ["/run.sh"]
