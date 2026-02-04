#!/bin/bash

ROBOCODE_DIR="/Users/richard/personal/robocode"
SKYNET_DIR="$ROBOCODE_DIR/skynet/robocode"
SUPERSAMPLE_SRC="$SKYNET_DIR/src/main/java/supersample"
BUILD_DIR="$SKYNET_DIR/target/supersample"
ROBOTS_DIR="$ROBOCODE_DIR/robots"

echo "Building SuperSample robots..."

# Create build directory
mkdir -p "$BUILD_DIR"

# Compile all supersample robots
javac -cp "$ROBOCODE_DIR/libs/robocode.jar" \
    -d "$BUILD_DIR" \
    "$SUPERSAMPLE_SRC"/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Create jar for each robot
cd "$BUILD_DIR"
for robot in SuperWalls SuperTrackFire SuperTracker SuperSpinBot SuperSittingDuck SuperRamFire SuperMercutio SuperCrazy SuperBoxBot SuperCorners; do
    echo "Packaging $robot..."
    
    # Create properties file
    cat > "supersample/${robot}.properties" <<EOF
#Robot Properties
robot.description=SuperSample robot
robot.webpage=
robocode.version=1.9.4.7
robot.java.source.included=false
robot.author.name=Sample
robot.classname=supersample.${robot}
robot.name=${robot}
robot.version=1.0
EOF
    
    # Jar all class files for this robot (main class and inner classes)
    jar cf "$ROBOTS_DIR/${robot}.jar" supersample/${robot}.properties $(find supersample -name "${robot}*.class")
done

echo "SuperSample robots deployed to $ROBOTS_DIR"
