#How to publish on Docker HUB
# 1) docker build -t vaimeedock/sepa:latest -f Dockerfile .
# 2) docker login -u YOUR-USER-NAME.
# Build command on Apple M1: docker buildx build --platform linux/amd64 --push -t vaimeedock/engine .

FROM openjdk
#FROM bellsoft/liberica-runtime-container:jdk-all-17-glibc

COPY ./target/engine-1.0.0-SNAPSHOT.jar /engine.jar
COPY ./src/main/resources/jmxremote.password /jmxremote.password
COPY ./src/main/resources/jmxremote.access /jmxremote.access
COPY ./src/main/resources/jmx.properties /jmx.properties

RUN chmod 600 /jmxremote.password

EXPOSE 8000
EXPOSE 9000
EXPOSE 7091

ENTRYPOINT ["java","-Djava.rmi.server.hostname=dld.arces.unibo.it","-Dcom.sun.management.config.file=jmx.properties","-jar","engine.jar"]
