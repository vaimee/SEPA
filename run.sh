#!/bin/bash

#java -Djava.rmi.server.hostname=${JMX_HOST} -Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT} -Dcom.sun.management.jmxremote.port=${JMX_PORT} -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote -jar engine.jar
java ${JAVA_OPTS} -jar engine.jar