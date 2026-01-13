#!/bin/bash

# Launch Robocode with Skynet vs Walls battle

cd /Users/richard/personal/robocode

echo "Starting Robocode with Skynet vs Walls battle..."

# Launch Robocode with a battle configuration
java -Xmx512M -cp libs/robocode.jar -DNOSECURITY=true robocode.Robocode \
    -battle \
    -robots "net.richardsenior.robocode.skynet.SkynetRobot,sample.Walls" \
    -rounds 10 \
    -width 800 \
    -height 600
