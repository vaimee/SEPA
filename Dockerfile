# syntax=docker/dockerfile:1.6

############################
# Build stage
############################
FROM maven:3.9.11-eclipse-temurin-25 AS build

ARG REVISION=1.0.0-SNAPSHOT
WORKDIR /workspace

COPY pom.xml .
COPY engine/pom.xml engine/pom.xml
COPY client-api/pom.xml client-api/pom.xml
COPY example-chat/pom.xml example-chat/pom.xml
COPY tool-dashboard/pom.xml tool-dashboard/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests -Dgpg.skip=true -Drevision=${REVISION} -pl engine -am dependency:go-offline

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests -Dgpg.skip=true -Drevision=${REVISION} -pl engine -am clean package


############################
# Runtime stage
############################
FROM eclipse-temurin:25-jre

ARG REVISION=1.0.0-SNAPSHOT

COPY --from=build /workspace/engine/target/sepa-engine-${REVISION}.jar /engine.jar

COPY --from=build /workspace/engine/src/main/resources/run.sh /run.sh
COPY --from=build /workspace/engine/src/main/resources/jmxremote.password /jmxremote.password
COPY --from=build /workspace/engine/src/main/resources/jmxremote.access /jmxremote.access
COPY --from=build /workspace/engine/src/main/resources/jmx.properties /jmx.properties
COPY --from=build /workspace/engine/src/main/resources/log4j2.xml /log4j2.xml
COPY --from=build /workspace/engine/src/main/resources/endpoints /endpoints

RUN chmod 600 /jmxremote.password && chmod 755 /run.sh

ENV JMX_HOSTNAME=0.0.0.0
ENV JMX_PORT=7090

EXPOSE 7090
EXPOSE 8000
EXPOSE 9000

ENV SIS_DATA=/var/lib/sis
RUN mkdir -p /var/lib/sis

ENTRYPOINT ["/run.sh"]
