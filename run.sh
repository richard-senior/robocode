#!/bin/bash

# Skynet View Script - Compile, copy, and launch Robocode GUI

ROBOCODE_DIR="/Users/richard/personal/robocode"
SKYNET_DIR="$ROBOCODE_DIR/skynet/robocode"

cd "$ROBOCODE_DIR" || { echo "Robocode directory not found!"; exit 1; }
echo "Compiling Skynet robot..."
cd "$SKYNET_DIR" || { echo "Skynet robot directory not found!"; exit 1; }
source ~/.bashrc && mvn package

echo "Copying robot to Robocode..."
cd $ROBOCODE_DIR/robots
cp $SKYNET_DIR/target/skynet-1.0-SNAPSHOT.jar .

echo "Starting Robocode GUI..."
cd $ROBOCODE_DIR

JAVA_OPTS="-Xmx1024M -XX:+IgnoreUnrecognizedVMOptions"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/sun.net.www.protocol.jar=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang.reflect=ALL-UNNAMED" 
JAVA_OPTS="$JAVA_OPTS --add-opens java.desktop/sun.awt=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.net=ALL-UNNAMED"

java $JAVA_OPTS -cp libs/robocode.jar robocode.Robocode
