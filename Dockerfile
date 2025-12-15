# syntax=docker/dockerfile:1.6

############################
# Build stage
############################
FROM maven:3.9.9-sapmachine-21 AS build

ARG REVISION=1.0.0-SNAPSHOT
WORKDIR /workspace

# 1) Copia solo i POM per massimizzare la cache Maven
COPY pom.xml .
COPY engine/pom.xml engine/pom.xml
# Se engine dipende da client-api (molto probabile), abilita anche questa riga:
COPY client-api/pom.xml client-api/pom.xml
COPY example-chat/pom.xml example-chat/pom.xml
COPY tool-dashboard/pom.xml tool-dashboard/pom.xml
COPY settings.xml /root/.m2/settings.xml

# 2) Pre-fetch dipendenze (cache .m2) usando settings.xml come secret
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    --mount=type=cache,target=/root/.m2 \
    export GITHUB_ACTOR="$(cat /run/secrets/github_actor)" && \
    export GITHUB_TOKEN="$(cat /run/secrets/github_token)" && \
    mvn -B -DskipTests -Drevision=${REVISION} -pl engine -am dependency:go-offline
# 3) Copia tutto il codice
COPY . .

# 4) Build del modulo engine (e dipendenze necessarie)
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    --mount=type=cache,target=/root/.m2 \
    export GITHUB_ACTOR="$(cat /run/secrets/github_actor)" && \
    export GITHUB_TOKEN="$(cat /run/secrets/github_token)" && \
    mvn -B -DskipTests -Drevision=${REVISION} -pl engine -am clean package


############################
# Runtime stage
############################
FROM sapmachine:21.0.6

ARG REVISION=1.0.0-SNAPSHOT

# Copia il JAR “giusto” (evita original-*)
# Se il nome non è esattamente engine-${REVISION}.jar, dimmelo e lo allineiamo.
COPY --from=build /workspace/engine/target/engine-${REVISION}.jar /engine.jar

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