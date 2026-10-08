#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64

# Load and export environment variables from .env if it exists
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

~/gradle-8.2.1/bin/gradle build -x test -Dorg.gradle.java.home=/usr/lib/jvm/java-17-openjdk-arm64
rm -f build/libs/*-plain.jar
java -jar build/libs/*.jar

