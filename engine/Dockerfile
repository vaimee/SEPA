FROM openjdk

#COPY . /usr/sepa/src

#WORKDIR /usr/sepa/src

#RUN apk update && apk add --no-cache maven

#RUN mvn package -DskipTests=true

#RUN mv ./engine/target/engine-0-SNAPSHOT.jar ../engine.jar

#RUN rm -rf /usr/sepa/src

COPY ./target/engine-0-SNAPSHOT.jar /engine.jar

WORKDIR /

EXPOSE 8000
EXPOSE 9000

ENTRYPOINT ["java","-jar","engine.jar"]
