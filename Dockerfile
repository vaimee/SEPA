FROM openjdk:8-alpine

COPY . /usr/sepa/src

WORKDIR /usr/sepa/src

RUN apk update && apk add --no-cache maven

RUN mvn package -DskipTests=true

RUN mv ./engine/target/engine-0-SNAPSHOT.jar ../engine.jar

RUN rm -rf /usr/sepa/src

WORKDIR /usr/sepa

EXPOSE 8000
EXPOSE 9000

ENTRYPOINT ["java","-jar","engine.jar"]
