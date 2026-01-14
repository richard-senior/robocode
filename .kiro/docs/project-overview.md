# Skynet Robocode Project

## Overview
This is a Robocode robot implementation called "Skynet" that implements wave surfing and enemy tracking capabilities. The project is built in Java using Maven and follows object-oriented design patterns for battlefield simulation.

## Project Structure

### Core Architecture
- **SkynetRobot**: Main robot class extending AdvancedRobot
- **Battlefield**: Singleton managing all obstacles (enemies, waves) on the battlefield
- **Obstacle Interface**: Base interface for all battlefield objects
- **Enemy**: Represents opponent robots with position tracking and prediction
- **Wave**: Represents bullet waves for dodging/surfing

### Key Classes

#### `/src/main/java/net/richardsenior/robocode/skynet/robot/SkynetRobot.java`
- Main robot implementation
- Delegates all battlefield management to BattlefieldImpl
- Has onPaint() method for visual debugging (requires Paint button enabled in robot console)

#### `/src/main/java/net/richardsenior/robocode/skynet/base/impl/BattlefieldImpl.java`
- Singleton battlefield manager
- Uses ConcurrentHashMap for thread-safe obstacle collection
- Handles all Robocode events (ScannedRobot, RobotDeath, etc.)
- Automatically removes dead robots and old waves

#### `/src/main/java/net/richardsenior/robocode/skynet/base/impl/EnemyImpl.java`
- Tracks enemy robot positions and energy
- Detects bullet firing via energy drops (0.1-3.0 range)
- Creates WaveImpl objects when enemies fire
- Stores scan history for movement prediction

#### `/src/main/java/net/richardsenior/robocode/skynet/base/impl/WaveImpl.java`
- Represents expanding bullet waves
- Contains WaveSectorImpl objects for danger zones
- Self-removes when wave passes robot position
- Draws as orange circles for debugging

## Current Status

### Working Features
- ✅ Basic robot movement and radar scanning
- ✅ Enemy detection and tracking
- ✅ Bullet firing detection via energy drops
- ✅ Wave creation and expansion
- ✅ Thread-safe obstacle management
- ✅ Visual debugging (enemies=red circles, waves=orange circles)
- ✅ Automatic cleanup of dead robots and old waves

### Known Issues
- 🔧 Wave painting may not be visible (requires Paint button in robot console)
- 🔧 Polynomial prediction for stationary robots needs refinement
- 🔧 WaveSectorImpl angle calculations need validation

### Recent Fixes
- Fixed ConcurrentModificationException by using ConcurrentHashMap
- Fixed null pointer exceptions in EnemyImpl.updateSelf()
- Added explicit robot death handling
- Improved wave lifecycle management

## Build & Run

### Build Commands
```bash
cd /Users/richard/personal/robocode/skynet/robocode
mvn clean package
cp target/skynet-1.0-SNAPSHOT.jar /Users/richard/personal/robocode/robots/
```

### Launch Scripts
- `./skynet-gui.sh` - Builds and launches Robocode with painting enabled
- `./deploy.sh` - Just builds and copies JAR to robots directory

### Debugging
1. Enable painting: Right-click robot → Robot Console → Paint button
2. Check console output for debug messages
3. Look for "BattlefieldImpl.onPaint called" messages

## Key Algorithms

### Enemy Firing Detection
```java
double energyDrop = lastEnemyEnergy - currentEnergy;
if (energyDrop > 0.0 && energyDrop <= 3.0) {
    // Enemy fired - create wave
    double bulletVelocity = 20.0 - (3.0 * energyDrop);
    Wave wave = new WaveImpl(this, bulletVelocity);
}
```

### Wave Expansion
```java
long ticks = (currentTime - createdTime) + 1;
radius = (int)(ticks * bulletSpeed);
```

### Position Calculation
```java
double absoluteBearing = robotHeading + scannedBearing;
double x = robotX + distance * Math.sin(absoluteBearing);
double y = robotY + distance * Math.cos(absoluteBearing);
```

## Development Notes

### Threading
- Robocode runs events on separate threads from main robot execution
- All collections must be thread-safe (using ConcurrentHashMap)
- Avoid modifying collections during iteration

### Coordinate System
- Robocode uses standard Cartesian coordinates (0,0 at bottom-left)
- Angles in radians, with 0 pointing north
- Battlefield typically 800x600 pixels

### Performance
- WeakHashMap replaced with ConcurrentHashMap for thread safety
- Manual cleanup of dead robots and expired waves
- Efficient obstacle iteration with snapshots when needed

## Next Steps
1. Implement proper wave surfing movement algorithm
2. Add polynomial prediction for enemy movement
3. Implement targeting system
4. Add gun cooling rate tracking
5. Optimize wave sector calculations for better accuracy

## Git Repository
- Remote: https://github.com/richard-senior/robocode.git
- Branch: main
- Local config uses personal GitHub credentials for this repo only

## Dependencies
- Java 8+ (tested with Java 21)
- Maven 3.6+
- Robocode 1.10.0+
- No external dependencies beyond Robocode API
