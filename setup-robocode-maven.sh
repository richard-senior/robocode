#!/bin/bash

# Robocode Maven Setup - Downloads latest Robocode from Maven Central

ROBOCODE_DIR="/Users/richard/personal/robocode"
VERSION="1.10.1"

echo "Setting up Robocode $VERSION from Maven Central..."

cd "$ROBOCODE_DIR"
mkdir -p {libs,robots,battles,config}

# Download core Robocode JARs
echo "Downloading Robocode JARs..."
cd libs

curl -L -o robocode.api.jar "https://repo1.maven.org/maven2/net/sf/robocode/robocode.api/$VERSION/robocode.api-$VERSION.jar"
curl -L -o robocode.core.jar "https://repo1.maven.org/maven2/net/sf/robocode/robocode.core/$VERSION/robocode.core-$VERSION.jar"
curl -L -o robocode.battle.jar "https://repo1.maven.org/maven2/net/sf/robocode/robocode.battle/$VERSION/robocode.battle-$VERSION.jar"
curl -L -o robocode.host.jar "https://repo1.maven.org/maven2/net/sf/robocode/robocode.host/$VERSION/robocode.host-$VERSION.jar"
curl -L -o robocode.repository.jar "https://repo1.maven.org/maven2/net/sf/robocode/robocode.repository/$VERSION/robocode.repository-$VERSION.jar"
curl -L -o robocode.sound.jar "https://repo1.maven.org/maven2/net/sf/robocode/robocode.sound/$VERSION/robocode.sound-$VERSION.jar"
curl -L -o robocode.ui.jar "https://repo1.maven.org/maven2/net/sf/robocode/robocode.ui/$VERSION/robocode.ui-$VERSION.jar"

# Create sample robots directory
cd ../robots
mkdir -p sample

echo "Robocode $VERSION setup complete!"
echo "Location: $ROBOCODE_DIR"
echo ""
echo "To run Robocode:"
echo "java -cp \"$ROBOCODE_DIR/libs/*\" robocode.Robocode"
