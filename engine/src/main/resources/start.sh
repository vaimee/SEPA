#!/bin/bash
java -Dcom.sun.management.config.file=./jmx.properties -Dlog4j.configurationFile=./log4j2.xml -Dorg.slf4j.simpleLogger.defaultLogLevel=off \
	-jar engine-0.9.11.jar
