#!/bin/bash

# Quick Robocode installer for macOS

echo "Installing Robocode..."

# Create installation directory
INSTALL_DIR="/Users/richard/personal/robocode"
mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"

# Download Robocode
echo "Downloading Robocode..."
curl -L -o robocode-setup.jar "https://sourceforge.net/projects/robocode/files/robocode/1.9.4.7/robocode-1.9.4.7-setup.jar/download"

if [ -f "robocode-setup.jar" ] && [ -s "robocode-setup.jar" ]; then
    echo "Running installer..."
    java -jar robocode-setup.jar
    echo "Robocode installed to: $INSTALL_DIR"
    echo "You can now run: ./learn.sh or ./play.sh"
else
    echo "Download failed. Please install manually:"
    echo "1. Go to: https://robocode.sourceforge.io/"
    echo "2. Download and run the installer"
    echo "3. Install to: $INSTALL_DIR"
fi
