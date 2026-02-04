package net.richardsenior.robocode.skynet.base.impl;

import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Mover;
import net.richardsenior.robocode.skynet.base.Wave;
import net.richardsenior.robocode.skynet.base.Enemy;

public class MoverImpl implements Mover {
    private static final double MOVE_DISTANCE = 8.0; // Max robot velocity per tick
    private static final int NUM_SAMPLES = 16; // Sample points around each circle
    private static final int NUM_CIRCLES = 4; // Number of concentric circles
    private static final double MIN_RADIUS = 40.0; // Minimum circle radius (safely around robot)
    private static final double MAX_RADIUS_DIVISOR = 5.0; // Divisor for max radius (diagonal / divisor)
    private static final double WALL_EXCLUSION_ZONE = 28.0; // Margin from walls
    
    private Battlefield battlefield;
    private Point2D.Double pos;
    private Point2D.Double targetPos; // Store for painting
    private int moveDirection = 1; // 1 = forward, -1 = backward
    private boolean[][] safePoints; // [circle][sample] - safe across ALL time slices
    private double[] circleRadii; // Calculated radii for each circle
    private boolean hitWall = false; // Flag for wall collision
    private int wallEscapeTicks = 0; // Ticks remaining in wall escape mode

    public MoverImpl(Battlefield battlefield) {
        this.battlefield = battlefield;
        this.safePoints = new boolean[NUM_CIRCLES][NUM_SAMPLES];
        this.circleRadii = new double[NUM_CIRCLES];
        calculateCircleRadii();
    }
    
    private void calculateCircleRadii() {
        // Calculate battlefield diagonal
        double width = battlefield.getSelf().getBattleFieldWidth();
        double height = battlefield.getSelf().getBattleFieldHeight();
        double diagonal = Math.sqrt(width * width + height * height);
        
        // Outer circle is diagonal / MAX_RADIUS_DIVISOR
        double maxRadius = diagonal / MAX_RADIUS_DIVISOR;
        
        // Distribute circles evenly from MIN_RADIUS to maxRadius
        for (int i = 0; i < NUM_CIRCLES; i++) {
            circleRadii[i] = MIN_RADIUS + (i * (maxRadius - MIN_RADIUS) / (NUM_CIRCLES - 1));
        }
    }

    @Override
    public void update() {
        this.pos = new Point2D.Double(battlefield.getSelf().getX(), battlefield.getSelf().getY());
    }

    @Override
    public void doMove() {
        // If in wall escape mode, skip normal movement logic
        if (wallEscapeTicks > 0) {
            wallEscapeTicks--;
            return;
        }
        
        // Get battlefield area ONCE for current time only
        Area currentArea = battlefield.getBattlefieldArea(0);
        
        // Clear previous safe points
        for (int i = 0; i < NUM_CIRCLES; i++) {
            for (int j = 0; j < NUM_SAMPLES; j++) {
                safePoints[i][j] = true; // Assume safe initially
            }
        }
        
        // Check each circle's points against CURRENT battlefield area AND active waves
        for (int circleIdx = 0; circleIdx < NUM_CIRCLES; circleIdx++) {
            double radius = circleRadii[circleIdx];
            
            for (int sampleIdx = 0; sampleIdx < NUM_SAMPLES; sampleIdx++) {
                double bearing = (sampleIdx * 2 * Math.PI) / NUM_SAMPLES;
                Point2D.Double testPoint = projectPosition(pos, bearing, radius);
                
                // Check if this point is in the safe area NOW
                if (!currentArea.contains(testPoint.x, testPoint.y)) {
                    safePoints[circleIdx][sampleIdx] = false;
                    continue; // Already unsafe, skip wave check
                }
                
                // Check if this point is inside any active wave's danger zone
                for (net.richardsenior.robocode.skynet.base.Enemy enemy : battlefield.getEnemies()) {
                    for (net.richardsenior.robocode.skynet.base.Wave wave : enemy.getWaves()) {
                        // Get wave outline (annular segment)
                        Area waveArea = wave.getOutline(0);
                        if (waveArea != null && waveArea.contains(testPoint.x, testPoint.y)) {
                            safePoints[circleIdx][sampleIdx] = false;
                            break; // Point is in wave danger zone
                        }
                    }
                    if (!safePoints[circleIdx][sampleIdx]) break; // Already marked unsafe
                }
            }
        }
        
        // Check if ALL sample points that are inside the battlefield are safe
        double fieldWidth = battlefield.getSelf().getBattleFieldWidth();
        double fieldHeight = battlefield.getSelf().getBattleFieldHeight();
        boolean allSafe = true;
        
        for (int i = 0; i < NUM_CIRCLES; i++) {
            for (int j = 0; j < NUM_SAMPLES; j++) {
                double bearing = (j * 2 * Math.PI) / NUM_SAMPLES;
                Point2D.Double testPoint = projectPosition(pos, bearing, circleRadii[i]);
                
                // Only check points that are inside the battlefield bounds
                if (testPoint.x >= WALL_EXCLUSION_ZONE && testPoint.x <= fieldWidth - WALL_EXCLUSION_ZONE &&
                    testPoint.y >= WALL_EXCLUSION_ZONE && testPoint.y <= fieldHeight - WALL_EXCLUSION_ZONE) {
                    
                    if (!safePoints[i][j]) {
                        allSafe = false;
                        break;
                    }
                }
            }
            if (!allSafe) break;
        }
        
        // If all points inside battlefield are safe, stop moving
        if (allSafe) {
            battlefield.getSelf().setTurnRightRadians(0);
            battlefield.getSelf().setAhead(0);
            return;
        }
        
        // Find best path through the safe points
        double currentHeading = battlefield.getSelf().getHeadingRadians();
        double targetBearing = findBestPath(currentHeading);
        
        // Set heading and move
        double turn = robocode.util.Utils.normalRelativeAngle(targetBearing - currentHeading);
        
        // Prefer reversing over sharp turns - check if going backward is better
        double backwardHeading = robocode.util.Utils.normalAbsoluteAngle(currentHeading + Math.PI);
        double backwardTurn = robocode.util.Utils.normalRelativeAngle(targetBearing - backwardHeading);
        
        // If backward turn is significantly smaller, reverse instead
        if (Math.abs(backwardTurn) < Math.abs(turn) * 0.7) {
            moveDirection = -1;
            turn = backwardTurn;
        } else if (Math.abs(turn) > Math.PI / 2 + 0.3) {
            // Only flip if turn is very large (with hysteresis)
            moveDirection *= -1;
            turn = robocode.util.Utils.normalRelativeAngle(turn + Math.PI);
        }
        
        // Adjust speed based on turn angle - slow down for sharp turns
        double turnDegrees = Math.abs(Math.toDegrees(turn));
        double speed;
        
        // Check if we're in emergency escape mode (no safe inner circle points)
        boolean emergencyEscape = true;
        for (int i = 0; i < NUM_SAMPLES; i++) {
            if (safePoints[0][i]) {
                emergencyEscape = false;
                break;
            }
        }
        
        if (emergencyEscape) {
            // Emergency: Full speed escape regardless of turn angle
            speed = 100 * moveDirection;
        } else if (turnDegrees > 60) {
            // Sharp turn - slow way down to turn faster
            speed = 20 * moveDirection;
        } else if (turnDegrees > 30) {
            // Moderate turn - reduce speed
            speed = 50 * moveDirection;
        } else {
            // Gentle turn - full speed
            speed = 100 * moveDirection;
        }
        
        // Store target position for painting
        targetPos = projectPosition(pos, targetBearing, circleRadii[NUM_CIRCLES - 1]);
        
        battlefield.getSelf().setTurnRightRadians(turn);
        battlefield.getSelf().setAhead(speed);
    }
    
    @Override
    public void onHitWall() {
        hitWall = true;
        wallEscapeTicks = 20; // Escape for 20 ticks
        // Reverse direction immediately
        moveDirection *= -1;
        // Turn perpendicular to current heading to get away from wall
        double currentHeading = battlefield.getSelf().getHeadingRadians();
        double fieldWidth = battlefield.getSelf().getBattleFieldWidth();
        double fieldHeight = battlefield.getSelf().getBattleFieldHeight();
        
        // Calculate bearing toward center
        double centerX = fieldWidth / 2;
        double centerY = fieldHeight / 2;
        double bearingToCenter = Math.atan2(centerX - pos.x, centerY - pos.y);
        
        // Turn toward center and move
        double turn = robocode.util.Utils.normalRelativeAngle(bearingToCenter - currentHeading);
        battlefield.getSelf().setTurnRightRadians(turn);
        battlefield.getSelf().setAhead(100);
    }
    
    private double findBestPath(double currentHeading) {
        // Check if we have any safe points on the innermost circle
        boolean hasInnerSafe = false;
        for (int i = 0; i < NUM_SAMPLES; i++) {
            if (safePoints[0][i]) {
                hasInnerSafe = true;
                break;
            }
        }
        
        // Emergency: No safe points on inner circle - escape from walls
        if (!hasInnerSafe) {
            return calculateWallEscape();
        }
        
        // Calculate bearing toward Lagrange point (least densely populated spot)
        java.awt.geom.Point2D.Double lagrangePoint = battlefield.getLagrangePoint();
        double targetBearing;
        
        if (lagrangePoint != null) {
            targetBearing = Math.atan2(lagrangePoint.x - pos.x, lagrangePoint.y - pos.y);
        } else {
            // Fallback: head toward center if no Lagrange point
            double fieldWidth = battlefield.getSelf().getBattleFieldWidth();
            double fieldHeight = battlefield.getSelf().getBattleFieldHeight();
            targetBearing = Math.atan2(fieldWidth/2 - pos.x, fieldHeight/2 - pos.y);
        }
        
        // Use current move direction to bias toward continuing in same direction
        double preferredHeading = moveDirection > 0 ? currentHeading : 
            robocode.util.Utils.normalAbsoluteAngle(currentHeading + Math.PI);
        
        // Get active waves for perpendicular movement calculation
        java.util.List<Wave> activeWaves = new java.util.ArrayList<>();
        for (Enemy enemy : battlefield.getEnemies()) {
            activeWaves.addAll(enemy.getWaves());
        }
        
        // Distance-based urgency weights (inner circles = more urgent)
        double[] circleWeights = {4.0, 2.0, 1.0, 0.5};
        
        // For each circle, find the safe bearing with best score
        double[] pathBearings = new double[NUM_CIRCLES];
        boolean pathFound = true;
        
        for (int circleIdx = 0; circleIdx < NUM_CIRCLES; circleIdx++) {
            double bestBearing = preferredHeading;
            double bestScore = Double.MAX_VALUE;
            boolean foundSafe = false;
            
            for (int sampleIdx = 0; sampleIdx < NUM_SAMPLES; sampleIdx++) {
                if (safePoints[circleIdx][sampleIdx]) {
                    double bearing = (sampleIdx * 2 * Math.PI) / NUM_SAMPLES;
                    
                    double score = 0;
                    
                    // 1. Prefer continuing current direction (weight: 1.0)
                    double diffFromPreferred = Math.abs(robocode.util.Utils.normalRelativeAngle(bearing - preferredHeading));
                    score += diffFromPreferred;
                    
                    // 2. Prefer moving toward Lagrange point (weight: 0.3)
                    double diffFromTarget = Math.abs(robocode.util.Utils.normalRelativeAngle(bearing - targetBearing));
                    score += 0.3 * diffFromTarget;
                    
                    // 3. Prefer perpendicular movement to waves (weight: varies by distance)
                    for (Wave wave : activeWaves) {
                        Point2D waveCentre = wave.getCentre();
                        if (waveCentre == null) continue;
                        Point2D.Double waveCenter = (Point2D.Double) waveCentre;
                        
                        // Calculate wave distance and urgency
                        double waveDistance = pos.distance(waveCenter) - wave.getRadius();
                        if (waveDistance < 0) waveDistance = 0; // Wave has passed
                        
                        // Urgency increases as wave gets closer
                        double urgency = circleWeights[circleIdx] / (waveDistance + 50);
                        
                        // Bearing from wave center to our position
                        double waveBearing = Math.atan2(pos.x - waveCenter.x, pos.y - waveCenter.y);
                        
                        // How perpendicular is this bearing to the wave?
                        double angleToWave = Math.abs(robocode.util.Utils.normalRelativeAngle(bearing - waveBearing));
                        
                        // Score: 0 = perpendicular (good), PI/2 = radial (bad)
                        double perpendicularScore = Math.abs(angleToWave - Math.PI/2);
                        
                        score += urgency * perpendicularScore;
                    }
                    
                    if (score < bestScore) {
                        bestScore = score;
                        bestBearing = bearing;
                        foundSafe = true;
                    }
                }
            }
            
            if (!foundSafe) {
                pathFound = false;
                break;
            }
            
            pathBearings[circleIdx] = bestBearing;
        }
        
        if (!pathFound) {
            // No clear path - use wall escape
            return calculateWallEscape();
        }
        
        // Average the bearings to get smooth path
        double sumX = 0, sumY = 0;
        for (double bearing : pathBearings) {
            sumX += Math.sin(bearing);
            sumY += Math.cos(bearing);
        }
        
        return Math.atan2(sumX, sumY);
    }
    
    private double calculateWallEscape() {
        // Calculate bearing away from nearest walls
        double fieldWidth = battlefield.getSelf().getBattleFieldWidth();
        double fieldHeight = battlefield.getSelf().getBattleFieldHeight();
        
        double distToLeft = pos.x;
        double distToRight = fieldWidth - pos.x;
        double distToBottom = pos.y;
        double distToTop = fieldHeight - pos.y;
        
        // Find which wall(s) we're closest to
        double minDist = Math.min(Math.min(distToLeft, distToRight), Math.min(distToBottom, distToTop));
        
        // Calculate escape bearing toward center, away from walls
        double centerX = fieldWidth / 2;
        double centerY = fieldHeight / 2;
        
        // If we're in a corner, head toward opposite corner
        if (minDist < 100) {
            double escapeX = centerX;
            double escapeY = centerY;
            
            // Bias away from nearest walls
            if (distToLeft < 100) escapeX = fieldWidth * 0.75;
            if (distToRight < 100) escapeX = fieldWidth * 0.25;
            if (distToBottom < 100) escapeY = fieldHeight * 0.75;
            if (distToTop < 100) escapeY = fieldHeight * 0.25;
            
            return Math.atan2(escapeX - pos.x, escapeY - pos.y);
        }
        
        // Default: head toward center
        return Math.atan2(centerX - pos.x, centerY - pos.y);
    }
    
    private Point2D.Double projectPosition(Point2D.Double from, double heading, double distance) {
        return new Point2D.Double(
            from.x + distance * Math.sin(heading),
            from.y + distance * Math.cos(heading)
        );
    }

    @Override
    public void doPaint(Graphics2D g) {
        if (pos == null) return;
        
        // Draw concentric circles
        for (int i = 0; i < NUM_CIRCLES; i++) {
            double radius = circleRadii[i];
            
            // Draw circle outline
            g.setColor(new java.awt.Color(100, 100, 255, 100));
            g.drawOval((int)(pos.x - radius), (int)(pos.y - radius), (int)(radius * 2), (int)(radius * 2));
            
            // Draw all sample points using the actual safePoints array
            for (int j = 0; j < NUM_SAMPLES; j++) {
                double bearing = (j * 2 * Math.PI) / NUM_SAMPLES;
                Point2D.Double point = projectPosition(pos, bearing, radius);
                
                // Use the actual safePoints array that includes wave checks
                boolean safe = safePoints[i][j];
                
                if (safe) {
                    g.setColor(java.awt.Color.GREEN);
                    g.fillOval((int)point.x - 3, (int)point.y - 3, 6, 6);
                } else {
                    g.setColor(java.awt.Color.RED);
                    g.fillOval((int)point.x - 3, (int)point.y - 3, 6, 6);
                }
            }
        }
        
        // Draw our current position
        g.setColor(java.awt.Color.BLUE);
        g.fillOval((int)pos.x - 8, (int)pos.y - 8, 16, 16);
        
        // Draw the chosen path
        if (targetPos != null) {
            g.setColor(java.awt.Color.YELLOW);
            g.drawLine((int)pos.x, (int)pos.y, (int)targetPos.x, (int)targetPos.y);
            
            g.setColor(java.awt.Color.BLACK);
            g.fillOval((int)(targetPos.x - 5), (int)(targetPos.y - 5), 10, 10);
        }
        
        // Draw labels
        g.setColor(java.awt.Color.WHITE);
        g.drawString("Green = safe NOW, Red = blocked NOW", 10, 30);
        g.drawString("Radii: " + (int)circleRadii[0] + ", " + (int)circleRadii[1] + ", " + 
            (int)circleRadii[2] + ", " + (int)circleRadii[3], 10, 45);
    }
}
