package net.richardsenior.robocode.skynet.base.impl;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Enemy;
import net.richardsenior.robocode.skynet.base.Obstacle;
import net.richardsenior.robocode.skynet.base.Wave;

/**
 * Implementation of Wave that creates arc sectors representing dangerous areas
 * where bullets are likely to be based on all known enemy positions at firing time.
 * That is, at firing time all enemy bearings and their max possible deviation from that bearing
 * are calculated and arc fronts are created from that information
 */
public class WaveImpl implements Wave {
    private static final int ADVANCE_PER_STALE_TICK = 2; // additional pixels per tick of staleness (safety margin)
    private long createdTime;
    private Battlefield battlefield;
    private Point2D centre;
    private int radius;
    private double bulletSpeed;
    private int scanStaleness;
    private java.util.List<WaveFrontImpl>sectors;
    private java.lang.ref.WeakReference<Enemy> firingEnemy;
    
    @Override
    public Point2D getCentre() {return centre;}    
    @Override
    public int getRadius() {return radius;}
    @Override
    public Battlefield getBattlefield() {return battlefield;}
    @Override
    public double getBulletSpeed() {return bulletSpeed;}
    @Override  
    public Enemy getEnemy() {
        if (this.firingEnemy == null) {return null;}
        return this.firingEnemy.get();
    }
    public long getCreatedTime() {return this.createdTime;}
    /**
     * Creates a wave radiating out at the given bullet speed
     * The wave has 'rays' that point towards all other opponents on the battlefield
     * The rays have an angle which should mean the ray encompases the most likely extremes
     * Of likely opponent movement. These areas covered by these rays are to be avoided
     * @param enemy The enemy that is firing the bullet
     * @param bulletSpeed The speed at which the bullet is fired
     * @param staleness How many ticks old the scan data was when the bullet was detected
     */
    public WaveImpl(Enemy enemy, double bulletSpeed, int staleness) {
        if (enemy == null) {return;}
        
        // Check if enemy position is set
        Point2D enemyPos = enemy.getPosition();
        if (enemyPos == null) {return;}
        
        // Bullet was fired when we detected the energy drop, but we detect it one tick later
        this.createdTime = enemy.getBattlefield().getSelf().getTime() - 1;
        this.battlefield = enemy.getBattlefield();
        this.centre = enemyPos;
        // Calculate wave advance: bullet has traveled (bulletSpeed * staleness) during the stale period
        // Plus safety margin
        int waveAdvance = (int)(bulletSpeed * staleness) + (staleness * ADVANCE_PER_STALE_TICK);
        this.radius = (int)(bulletSpeed + waveAdvance);
        this.bulletSpeed = bulletSpeed;
        this.scanStaleness = staleness;
        this.scanStaleness = staleness;
        this.sectors = new ArrayList<WaveFrontImpl>();
        this.firingEnemy = new java.lang.ref.WeakReference<Enemy>(enemy);
        
        // Calculate angle pairs for all potential targets
        java.util.List<AnglePair> anglePairs = new ArrayList<>();
        for (Obstacle e : battlefield) {
            if (!(e instanceof Enemy)) {continue;}
            Enemy en = (Enemy)e;
            if (en.getId().equals(enemy.getId())) {continue;}
            
            Point2D targetPos = en.getPosition();
            if (targetPos == null) {continue;} // Skip if position not set
            double bearingToTarget = Math.atan2(targetPos.getX() - centre.getX(), targetPos.getY() - centre.getY());
            double distance = centre.distance(targetPos);
            double timeToIntercept = distance / bulletSpeed;
            double maxPerpendicularDistance = 8.0 * timeToIntercept; // MAX_PERPENDICULAR_VELOCITY
            double maxAngularDisplacement = Math.atan(maxPerpendicularDistance / distance);
            
            anglePairs.add(new AnglePair(
                bearingToTarget - maxAngularDisplacement,
                bearingToTarget + maxAngularDisplacement,
                en
            ));
        }
        
        // Merge overlapping angle pairs
        java.util.List<AnglePair> merged = mergeAnglePairs(anglePairs);
        
        // Create WaveFrontImpl for each merged sector
        for (AnglePair pair : merged) {
            this.sectors.add(new WaveFrontImpl(pair.startAngle, pair.endAngle, this));
        }
    }
    
    private java.util.List<AnglePair> mergeAnglePairs(java.util.List<AnglePair> pairs) {
        if (pairs.isEmpty()) return pairs;
        
        // Normalize all angles to [-π, π]
        for (AnglePair pair : pairs) {
            pair.startAngle = normalizeAngle(pair.startAngle);
            pair.endAngle = normalizeAngle(pair.endAngle);
            // Ensure endAngle > startAngle
            if (pair.endAngle < pair.startAngle) {
                pair.endAngle += 2 * Math.PI;
            }
        }
        
        // Sort by start angle
        pairs.sort((a, b) -> Double.compare(a.startAngle, b.startAngle));
        
        java.util.List<AnglePair> merged = new ArrayList<>();
        AnglePair current = pairs.get(0);
        
        for (int i = 1; i < pairs.size(); i++) {
            AnglePair next = pairs.get(i);
            
            // Check if current and next overlap
            if (next.startAngle <= current.endAngle) {
                // Merge: extend current to cover both
                current = new AnglePair(
                    current.startAngle,
                    Math.max(current.endAngle, next.endAngle),
                    null
                );
            } else {
                // No overlap, save current and move to next
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        
        return merged;
    }
    
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
    
    private static class AnglePair {
        double startAngle;
        double endAngle;
        Enemy enemy;
        
        AnglePair(double startAngle, double endAngle, Enemy enemy) {
            this.startAngle = startAngle;
            this.endAngle = endAngle;
            this.enemy = enemy;
        }
    }
    @Override
    public Point2D getPosition() {return centre;}
    
    /**
     * Returns the combined outline of all obstacles (sectors) in this wave
     * As they appear right now (this tick)
     */
    @Override
    public Area getOutline(int ticks) {
       // iterate wave fronts, add them and return them
       Area waveArea = new Area();
       for (Obstacle sector : sectors) {
           if (sector != null) {
               waveArea.add(sector.getOutline(ticks));
           }
       }
       return waveArea;
    }

    @Override
    public void update() {
        // Calculate distance traveled by bullet (standard wave surfing calculation)
        // Bullets move before we detect them, so they've already traveled bulletSpeed distance
        long ticks = battlefield.getSelf().getTime() - createdTime;
        double distanceTraveled = ticks * bulletSpeed;
        // Add staleness-based advance: bullet traveled during stale period plus safety margin
        int waveAdvance = (int)(bulletSpeed * scanStaleness) + (scanStaleness * ADVANCE_PER_STALE_TICK);
        this.radius = (int)(distanceTraveled + waveAdvance);
        // Update all sectors, although they probably don't do anything in their update methods
        for (WaveFrontImpl sector : sectors) {sector.update();}
    }
    
    /** irrelevant for waves */
    @Override
    public long lastSeen() {return 1000;}

    /**
     * Inner class representing an arc sector obstacle on the wave.
     * That is, for each enemy that MAY have been targetted we create an arc
     * of the wave circle which represents the likely location of any bullets which
     * would have been fired at that enemy (including ourselves)
     */
    public class WaveFrontImpl implements Obstacle {
        private static final int BASE_DEPTH = 5; // base depth in pixels
        private static final int DEPTH_PER_STALE_TICK = 15; // additional depth per tick of staleness
        private static final int MAX_DEPTH = 60; // maximum depth in pixels
        private Wave wave;
        private double startAngle;
        private double endAngle;
        
        /**
         * Create a wave front sector with pre-calculated angles
         * @param startAngle The starting angle (counter-clockwise limit)
         * @param endAngle The ending angle (clockwise limit)
         * @param wave The wave this sector belongs to
         */
        public WaveFrontImpl(double startAngle, double endAngle, Wave wave) {
            this.wave = wave;
            this.startAngle = startAngle;
            this.endAngle = endAngle;
        }
        
        /** irrelevant for waves sectors */
        @Override
        public long lastSeen() {return 1000;}

        @Override
        public Area getOutline(int ticks) {
            double outerRadius = this.wave.getRadius() + (ticks * this.wave.getBulletSpeed());
            double depth = Math.min(BASE_DEPTH + (scanStaleness * DEPTH_PER_STALE_TICK), MAX_DEPTH);
            double innerRadius = Math.max(outerRadius - depth, 1);
            
            // Create a proper closed annular sector using Path2D
            java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
            
            // Start at inner arc start point
            double innerStartX = centre.getX() + innerRadius * Math.sin(startAngle);
            double innerStartY = centre.getY() + innerRadius * Math.cos(startAngle);
            path.moveTo(innerStartX, innerStartY);
            
            // Draw outer radial line to outer arc start
            double outerStartX = centre.getX() + outerRadius * Math.sin(startAngle);
            double outerStartY = centre.getY() + outerRadius * Math.cos(startAngle);
            path.lineTo(outerStartX, outerStartY);
            
            // Draw outer arc
            double sweepAngle = endAngle - startAngle;
            while (sweepAngle > Math.PI) sweepAngle -= 2 * Math.PI;
            while (sweepAngle < -Math.PI) sweepAngle += 2 * Math.PI;
            
            double java2DStartAngle = Math.toDegrees(startAngle - Math.PI/2);
            double sweepDegrees = Math.toDegrees(sweepAngle);
            
            java.awt.geom.Arc2D.Double outerArc = new java.awt.geom.Arc2D.Double(
                centre.getX() - outerRadius, centre.getY() - outerRadius,
                outerRadius * 2, outerRadius * 2,
                java2DStartAngle, sweepDegrees, java.awt.geom.Arc2D.OPEN
            );
            path.append(outerArc, true);
            
            // Draw inner radial line from outer arc end to inner arc end
            double innerEndX = centre.getX() + innerRadius * Math.sin(endAngle);
            double innerEndY = centre.getY() + innerRadius * Math.cos(endAngle);
            path.lineTo(innerEndX, innerEndY);
            
            // Draw inner arc back to start (reverse direction)
            java.awt.geom.Arc2D.Double innerArc = new java.awt.geom.Arc2D.Double(
                centre.getX() - innerRadius, centre.getY() - innerRadius,
                innerRadius * 2, innerRadius * 2,
                java2DStartAngle + sweepDegrees, -sweepDegrees, java.awt.geom.Arc2D.OPEN
            );
            path.append(innerArc, true);
            
            // Close the path
            path.closePath();
            return new Area(path);
        }

        @Override
        public void update() {}

        @Override
        public Point2D getPosition() {return centre;}

        @Override
        public Battlefield getBattlefield() {return battlefield;}
    }
}
