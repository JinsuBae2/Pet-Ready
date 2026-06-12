#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export MARIADB_USER=pet_ready_user
export MARIADB_PASSWORD=2019
~/gradle-8.2.1/bin/gradle build -x test -Dorg.gradle.java.home=/usr/lib/jvm/java-17-openjdk-arm64
rm -f build/libs/*-plain.jar
java -jar build/libs/*.jar
