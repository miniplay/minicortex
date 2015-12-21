#!/usr/bin/env bash

# Check if java is installed
if ! which java >/dev/null; then
    echo "Java (/usr/bin/java) NOT FOUND!"
    exit 1;
fi

# Check if maven is installed
if ! which mvn >/dev/null; then
    echo "Maven (/usr/bin/mvn) NOT FOUND!"
    exit 1;
fi

echo "Running Maven compilation..."
mvn clean compile assembly:single

echo "Running Minicortex command..."
java -jar target/minicortex.jar $1