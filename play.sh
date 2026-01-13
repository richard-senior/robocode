#!/bin/bash

# Skynet Q-Learning Visual Play Script
# Runs battles with GUI for visual monitoring

set -e

# Configuration
ROBOT_JAR="target/skynet-1.0-SNAPSHOT.jar"

# Try to find Robocode installation
ROBOCODE_DIRS=(
    "/Users/richard/personal/robocode"
    "/usr/local/robocode"
    "/opt/robocode"
    "$HOME/robocode"
    "/Applications/Robocode.app/Contents/Resources/Java"
)

ROBOCODE_DIR=""
for dir in "${ROBOCODE_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        ROBOCODE_DIR="$dir"
        break
    fi
done

if [ -z "$ROBOCODE_DIR" ]; then
    echo "Error: Robocode installation not found!"
    echo "Please install Robocode or set ROBOCODE_DIR environment variable"
    echo "Download from: https://robocode.sourceforge.io/"
    exit 1
fi

echo "Using Robocode installation: $ROBOCODE_DIR"

# Standard opponents to fight against
OPPONENTS=(
    "sample.SpinBot"
    "sample.Tracker"
    "sample.Walls"
    "sample.RamFire"
    "sample.Fire"
    "sample.Crazy"
)

# Check if robot JAR exists
if [ ! -f "$ROBOT_JAR" ]; then
    echo "Building robot JAR..."
    source ~/.bashrc && mvn package
fi

# Copy robot to Robocode robots directory
echo "Copying robot to Robocode..."
cp "$ROBOT_JAR" "$ROBOCODE_DIR/robots/"

echo "Starting visual battles..."

while true; do
    # Random number of opponents (1-4, leaving room for our robot)
    num_opponents=$((RANDOM % 4 + 1))
    
    # Select random opponents
    selected_opponents=""
    for ((i=0; i<num_opponents; i++)); do
        opponent_idx=$((RANDOM % ${#OPPONENTS[@]}))
        selected_opponents="$selected_opponents,${OPPONENTS[$opponent_idx]}"
    done
    
    # Remove leading comma
    selected_opponents="${selected_opponents:1}"
    
    echo "Fighting against: $selected_opponents"
    
    # Run battle with GUI
    java -cp "$ROBOCODE_DIR/libs/*" robocode.Robocode \
        -battle \
        -robots "net.richardsenior.robocode.skynet.robot.SkynetRobot,$selected_opponents" \
        -rounds 10 \
        -width 800 \
        -height 600
    
    echo "Battle completed. Press Ctrl+C to stop or wait for next battle..."
    sleep 3
done
