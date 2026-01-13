#!/bin/bash
# Deploy Skynet Robot

set -e

echo "Building Skynet robot..."
mvn clean package -q

echo "Deploying to Robocode..."
cp target/skynet-1.0-SNAPSHOT.jar ../../robots/

echo "Skynet robot deployed successfully!"
echo "JAR location: /Users/richard/personal/robocode/robots/skynet-1.0-SNAPSHOT.jar"
