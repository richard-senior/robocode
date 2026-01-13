#!/bin/bash

# Skynet Q-Learning Training Script
# Runs 50,000 headless battles for training

set -e

# Configuration
ROUNDS=50000
ROBOT_JAR="target/skynet-1.0-SNAPSHOT.jar"
LEARNING_DATA_DIR="learning_data"

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
    "sample.SittingDuck"
)

# Create learning data directory
mkdir -p "$LEARNING_DATA_DIR"

# Check if robot JAR exists
if [ ! -f "$ROBOT_JAR" ]; then
    echo "Building robot JAR..."
    source ~/.bashrc && mvn package
fi

# Copy robot to Robocode robots directory
echo "Copying robot to Robocode..."
cp "$ROBOT_JAR" "$ROBOCODE_DIR/robots/"

echo "Starting headless training for $ROUNDS rounds..."

for ((round=1; round<=ROUNDS; round++)); do
    # Random number of opponents (1-5)
    num_opponents=$((RANDOM % 5 + 1))
    
    # Select random opponents
    selected_opponents=""
    for ((i=0; i<num_opponents; i++)); do
        opponent_idx=$((RANDOM % ${#OPPONENTS[@]}))
        selected_opponents="$selected_opponents,${OPPONENTS[$opponent_idx]}"
    done
    
    # Remove leading comma
    selected_opponents="${selected_opponents:1}"
    
    # Run battle
    java -cp "$ROBOCODE_DIR/libs/*" robocode.Robocode \
        -battle \
        -nosound \
        -nodisplay \
        -robots "net.richardsenior.robocode.skynet.SkynetRobot,$selected_opponents" \
        -rounds 1 \
        -width 800 \
        -height 600 > /dev/null 2>&1
    
    # Progress indicator
    if ((round % 1000 == 0)); then
        echo "Completed $round/$ROUNDS rounds ($(( round * 100 / ROUNDS ))%)"
    fi
done

echo "Training completed! $ROUNDS rounds finished."
echo "Learning data stored in: $LEARNING_DATA_DIR"
