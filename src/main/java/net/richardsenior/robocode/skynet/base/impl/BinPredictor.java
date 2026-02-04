package net.richardsenior.robocode.skynet.base.impl;

import net.richardsenior.robocode.skynet.base.Predictor;
import net.richardsenior.robocode.skynet.base.Wave;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Bin-based statistical targeting predictor using GuessFactor methodology.
 * Learns enemy movement patterns by recording where they move relative to fired bullets.
 * Uses power-of-2 bin count for fast modulo operations.
 */
public class BinPredictor implements Predictor {
    private static final int BIN_COUNT = 32;  // Power of 2 for fast bitwise operations
    private static final int BIN_MASK = BIN_COUNT - 1;  // For fast modulo: x & BIN_MASK
    
    /**
     * Helper class to store targeting information for a fired wave
     */
    private static class TargetingWave {
        final double firingBearing;
        final double maxEscapeAngle;
        final Point2D.Double sourceLocation;
        
        TargetingWave(double firingBearing, double maxEscapeAngle, Point2D.Double sourceLocation) {
            this.firingBearing = firingBearing;
            this.maxEscapeAngle = maxEscapeAngle;
            this.sourceLocation = new Point2D.Double(sourceLocation.x, sourceLocation.y);
        }
    }
    
    private int[] bins;
    private int maxBin;
    private int maxValue;
    private Map<Wave, TargetingWave> firedWaves;  // Track waves we've fired
    
    public BinPredictor() {
        this.bins = new int[BIN_COUNT];
        this.maxBin = BIN_COUNT / 2;  // Start with center bin
        this.maxValue = 0;
        this.firedWaves = new HashMap<>();
    }
    
    @Override
    public double predictBearingOffset(Point2D.Double ourPosition, Point2D.Double enemyPosition,
                                      double bulletVelocity, double directBearing) {
        // Find the bin with most hits
        int bestBin = maxBin;
        
        // Convert bin to GuessFactor (-1 to +1)
        double guessFactor = (bestBin / (double)BIN_COUNT) * 2.0 - 1.0;
        
        // Calculate maximum escape angle
        double distance = ourPosition.distance(enemyPosition);
        double maxEscapeAngle = calculateMaxEscapeAngle(distance, bulletVelocity);
        
        // Convert GuessFactor to bearing offset
        return guessFactor * maxEscapeAngle;
    }
    
    @Override
    public void recordShot(Wave wave) {
        // Calculate firing bearing and max escape angle from wave
        Point2D centre = wave.getCentre();
        Point2D.Double sourceLocation = new Point2D.Double(centre.getX(), centre.getY());
        double bulletVelocity = wave.getBulletSpeed();
        
        // We need enemy position at firing time - get from wave's enemy
        if (wave.getEnemy() == null) return;
        Point2D.Double enemyPosition = wave.getEnemy().getPosition();
        
        double firingBearing = Math.atan2(
            enemyPosition.x - sourceLocation.x,
            enemyPosition.y - sourceLocation.y
        );
        
        double distance = sourceLocation.distance(enemyPosition);
        double maxEscapeAngle = calculateMaxEscapeAngle(distance, bulletVelocity);
        
        // Store the targeting information
        firedWaves.put(wave, new TargetingWave(firingBearing, maxEscapeAngle, sourceLocation));
    }
    
    @Override
    public void recordResult(Wave wave, Point2D.Double actualPosition) {
        TargetingWave targetingWave = firedWaves.get(wave);
        if (targetingWave == null) return;
        
        firedWaves.remove(wave);
        
        // Calculate actual bearing to enemy
        double actualBearing = Math.atan2(
            actualPosition.x - targetingWave.sourceLocation.x,
            actualPosition.y - targetingWave.sourceLocation.y
        );
        
        // Calculate bearing offset
        double bearingOffset = normalizeAngle(actualBearing - targetingWave.firingBearing);
        
        // Calculate GuessFactor
        if (targetingWave.maxEscapeAngle == 0) return;  // Avoid division by zero
        
        double guessFactor = bearingOffset / targetingWave.maxEscapeAngle;
        
        // Clamp to [-1, 1]
        guessFactor = Math.max(-1.0, Math.min(1.0, guessFactor));
        
        // Convert to bin index
        int bin = (int)((guessFactor + 1.0) / 2.0 * BIN_COUNT);
        bin = bin & BIN_MASK;  // Fast modulo using bitwise AND
        
        // Update bin count
        bins[bin]++;
        
        // Update max tracking
        if (bins[bin] > maxValue) {
            maxValue = bins[bin];
            maxBin = bin;
        }
    }
    
    // Geometric prediction methods - not used by bin predictor
    @Override
    public Point2D.Double predict(int ticks) {
        // Bin predictor doesn't do geometric prediction
        return null;
    }
    
    @Override
    public Point2D.Double getIntercept(double bulletVelocity, Point2D.Double firingPosition) {
        // Bin predictor doesn't do geometric intercept calculation
        return null;
    }
    
    // Helper methods
    private double calculateMaxEscapeAngle(double distance, double bulletVelocity) {
        // Maximum robot speed is 8 pixels/tick
        double maxRobotVelocity = 8.0;
        
        // Time for bullet to reach enemy
        double timeToImpact = distance / bulletVelocity;
        
        // Maximum distance enemy can travel perpendicular to bullet path
        double maxEscapeDistance = maxRobotVelocity * timeToImpact;
        
        // Maximum escape angle (in radians)
        return Math.atan2(maxEscapeDistance, distance);
    }
    
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
    
    // Debug/utility methods
    public int[] getBins() {
        return bins.clone();
    }
    
    public int getTotalShots() {
        int total = 0;
        for (int count : bins) {
            total += count;
        }
        return total;
    }
}
