FROM openjdk:13-alpine

COPY ./target/engine-0-SNAPSHOT.jar /engine.jar
#COPY ./target/jmx.properties /jmx.properties
#COPY ./target/jmxremote.access /jmxremote.access
#COPY ./target/jmxremote.password /jmxremote.password

#WORKDIR /

#RUN chmod 600 /jmxremote.password

EXPOSE 8000
EXPOSE 9000
EXPOSE 5555

ENTRYPOINT ["java","-Dcom.sun.management.jmxremote.port=5555","-Dcom.sun.management.jmxremote.authenticate=false","-Dcom.sun.management.jmxremote.ssl=false","-Dcom.sun.management.jmxremote.rmi.port=5555","-Djava.rmi.server.hostname=sepa.monas.vaimee.it","-jar","engine.jar"]
