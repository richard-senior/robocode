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
- **Scanner**: Manages radar scanning strategies (1v1 pencil beam, melee wobble/full scan)
- **Mover**: Handles movement using grid-based potential field navigation

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
- Tracks oldest scan time across all enemies

#### `/src/main/java/net/richardsenior/robocode/skynet/base/impl/ScannerImpl.java`
- Implements adaptive radar scanning strategies
- **1v1 Mode**: Pencil beam lock on single enemy (2x bearing for narrow sweep)
- **Melee Mode**: 
  - Full 360° scan when data is stale (>10 ticks old)
  - Wobble scan between min/max enemy bearings when data is fresh
  - Falls back to full scan if enemies span >180° (faster than wobbling)
- Alternates scan direction to average scan times across all targets

#### `/src/main/java/net/richardsenior/robocode/skynet/base/impl/EnemyImpl.java`
- Tracks enemy robot positions and energy
- Detects bullet firing via energy drops (0.1-3.0 range)
- Creates WaveImpl objects when enemies fire, passing scan staleness
- Stores scan history as Deque (last 5 scans) for movement prediction
- Uses PolynomialPredictor for movement prediction

#### `/src/main/java/net/richardsenior/robocode/skynet/base/impl/WaveImpl.java`
- Represents expanding bullet waves radiating from firing enemy
- Contains multiple WaveFrontImpl sectors (one per potential target)
- Self-removes when wave passes robot position
- **Wave Advance**: Compensates for detection delay
  - Base advance: `bulletSpeed × staleness` (distance traveled during stale period)
  - Safety margin: `staleness × 2px`
  - Ensures wave front encompasses actual bullet position
- Draws as semi-transparent orange sectors for debugging

#### `/src/main/java/net/richardsenior/robocode/skynet/base/impl/WaveImpl.WaveFrontImpl.java`
- Inner class representing individual danger sectors within a wave
- Creates annular sector (banana-shaped) obstacle for each potential target
- **Sector Width**: Based on maximum perpendicular velocity
  - Calculates time to intercept: `distance / bulletSpeed`
  - Max perpendicular travel: `MAX_PERPENDICULAR_VELOCITY × time` (default 6.0 px/tick)
  - Angular displacement: `atan(perpendicular / distance)`
  - Sector spans: `bearing ± maxAngularDisplacement`
- **Sector Depth**: Accounts for scan staleness uncertainty
  - Base depth: 10px
  - Additional depth: `DEPTH_PER_STALE_TICK × staleness` (default 15px/tick)
  - Max depth: 100px
  - Older scans = deeper sectors to account for position uncertainty
- Rendered as closed Path2D with inner arc, outer arc, and radial edges
- Filled with semi-transparent orange, outlined in solid orange

#### `/src/main/java/net/richardsenior/robocode/skynet/base/impl/MoverImpl.java`
- Grid-based potential field navigation (30px cells)
- Calculates gravity from three sources:
  - Enemies: Point source with inverse square law
  - Waves: Area source (checks if cell center inside wave outline)
  - Walls: Gradient within 50px margin
- Movement speed scales with local gravity (high danger = move fast)
- Direction follows steepest descent gradient (away from danger)

## Current Status

### Working Features
- ✅ Adaptive radar scanning (1v1 pencil beam, melee wobble/full scan)
- ✅ Enemy detection and tracking with scan history
- ✅ Bullet firing detection via energy drops
- ✅ Wave creation with staleness compensation
- ✅ Accurate wave front sectors based on physics
- ✅ Thread-safe obstacle management
- ✅ Visual debugging with semi-transparent danger zones
- ✅ Automatic cleanup of dead robots and old waves
- ✅ Grid-based potential field movement system

### Known Issues
- 🔧 Collision damage (0.6 energy) can trigger false wave detection
- 🔧 Energy gains from bullet hits can affect wave detection accuracy
- 🔧 Movement system not yet integrated into main robot loop

### Recent Fixes
- Fixed ConcurrentModificationException by using ConcurrentHashMap
- Fixed null pointer exceptions in EnemyImpl.updateSelf()
- Added explicit robot death handling
- Improved wave lifecycle management
- Fixed radar scanning to properly handle left/right turns
- Changed getScanHistory() to return Deque for direct access to getLast()
- Fixed wave front rendering to show closed banana shapes without rays to center
- Implemented staleness-based wave advance and sector depth

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
3. Wave sectors appear as semi-transparent orange banana shapes
4. Enemies appear as red circles

## Key Algorithms

### Radar Scanning Strategy
```java
// 1v1: Lock onto single enemy
if (battlefield.getSelf().getOthers() < 2) {
    double radarTurn = heading - radarHeading + enemyBearing;
    setTurnRadarRight(normalRelativeAngleDegrees(radarTurn) * 2);
}

// Melee: Adaptive scanning
long stalest = currentTime - oldestScanTime;
if (stalest > 10) {
    fullScan(); // 360° alternating direction
} else {
    wobble(); // Between min/max enemy bearings
}
```

### Enemy Firing Detection
```java
double energyDrop = lastEnemyEnergy - currentEnergy;
if (energyDrop > 0.09 && energyDrop <= 3.0) {
    double bulletVelocity = 20.0 - (3.0 * energyDrop);
    int staleness = currentTime - lastScanTime;
    Wave wave = new WaveImpl(this, bulletVelocity, staleness);
}
```

### Wave Front Sector Calculation
```java
// Calculate maximum angular displacement based on physics
double timeToIntercept = distance / bulletSpeed;
double maxPerpendicularDistance = MAX_PERPENDICULAR_VELOCITY * timeToIntercept;
double maxAngularDisplacement = Math.atan(maxPerpendicularDistance / distance);

// Sector spans bearing ± displacement
startAngle = bearingToTarget - maxAngularDisplacement;
endAngle = bearingToTarget + maxAngularDisplacement;

// Depth accounts for scan staleness
depth = BASE_DEPTH + (staleness * DEPTH_PER_STALE_TICK);
```

### Wave Advance Compensation
```java
// Wave must be ahead of bullet to account for detection delay
int waveAdvance = (int)(bulletSpeed * staleness) + (staleness * ADVANCE_PER_STALE_TICK);
radius = (int)(distanceTraveled + waveAdvance);
```

### Grid-Based Movement
```java
// Calculate gravity at each grid cell
for (Obstacle obstacle : battlefield) {
    if (obstacle instanceof Wave) {
        if (wave.getOutline().contains(cellCenter)) {
            gravityGrid[x][y] += WAVE_GRAVITY;
        }
    }
}

// Move along steepest descent gradient
double speed = Math.min(8.0, Math.max(1.0, localGravity * 0.5));
```

## Development Notes

### Threading
- Robocode runs events on separate threads from main robot execution
- All collections must be thread-safe (using ConcurrentHashMap)
- Avoid modifying collections during iteration

### Coordinate System
- Robocode uses standard Cartesian coordinates (0,0 at bottom-left)
- Angles in radians, with 0 pointing north (clockwise positive)
- Battlefield typically 800x600 pixels

### Performance
- Grid-based movement uses 30px cells for real-time performance
- Wave front sectors use center-point sampling instead of area intersection
- Scan history limited to last 5 scans per enemy
- Manual cleanup of dead robots and expired waves

### Tunable Constants

**ScannerImpl:**
- Stale scan threshold: 10 ticks (triggers full scan)
- Wobble scan: Falls back to full scan if enemies span >180°

**WaveImpl:**
- `ADVANCE_PER_STALE_TICK = 2` - Safety margin for wave advance

**WaveFrontImpl:**
- `MAX_PERPENDICULAR_VELOCITY = 6.0` - Max enemy lateral movement (px/tick)
- `BASE_DEPTH = 10` - Minimum sector depth (px)
- `DEPTH_PER_STALE_TICK = 15` - Additional depth per tick of staleness
- `MAX_DEPTH = 100` - Maximum sector depth cap

**MoverImpl:**
- `GRID_CELL_SIZE = 30` - Grid resolution (px)
- `ENEMY_GRAVITY = 10.0` - Enemy repulsion strength
- `WAVE_GRAVITY = 20.0` - Wave danger level
- `WALL_GRAVITY = 15.0` - Wall avoidance strength
- `WALL_MARGIN = 50` - Distance from wall to start avoiding (px)

## Next Steps
1. Integrate MoverImpl into main robot loop
2. Implement targeting system with lead calculation
3. Add gun cooling rate tracking
4. Tune gravity constants for optimal movement
5. Address false wave detection from collisions and bullet hits
6. Add statistical tracking for enemy firing patterns

## Git Repository
- Remote: https://github.com/richard-senior/robocode.git
- Branch: main
- Local config uses personal GitHub credentials for this repo only

## Dependencies
- Java 8+ (tested with Java 21)
- Maven 3.6+
- Robocode 1.10.0+
- No external dependencies beyond Robocode API
