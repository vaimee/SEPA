#How to publish on Docker HUB
# 1) docker login -u YOUR-USER-NAME.
#

FROM openjdk:13-alpine

COPY ./target/engine-1.0.0-SNAPSHOT.jar /engine.jar
#COPY ./target/jmx.properties /jmx.properties
#COPY ./target/jmxremote.access /jmxremote.access
#COPY ./target/jmxremote.password /jmxremote.password

#WORKDIR /

#RUN chmod 600 /jmxremote.password

EXPOSE 8000
EXPOSE 9000
EXPOSE 5555

#TODO: need to enable JMX!!!!!
ENTRYPOINT ["java","-jar","engine.jar"]
#ENTRYPOINT ["java","-Dcom.sun.management.jmxremote.port=5555","-Dcom.sun.management.jmxremote.authenticate=false","-Dcom.sun.management.jmxremote.ssl=false","-Dcom.sun.management.jmxremote.rmi.port=5555","-Djava.rmi.server.hostname=criteria.vaimee.it","-jar","engine.jar"]
