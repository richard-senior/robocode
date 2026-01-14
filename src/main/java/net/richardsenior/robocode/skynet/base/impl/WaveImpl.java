package net.richardsenior.robocode.skynet.base.impl;

import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Enemy;
import net.richardsenior.robocode.skynet.base.Obstacle;
import net.richardsenior.robocode.skynet.base.Wave;
import net.richardsenior.robocode.skynet.base.Predictor;
import net.richardsenior.robocode.skynet.base.EventWrapper;
import robocode.ScannedRobotEvent;

/**
 * Implementation of Wave that creates arc sectors representing dangerous areas
 * where bullets are likely to be based on all known enemy positions at firing time
 * and predicted positions when the wave reaches them.
 * That is, a list of sectors centred on the firing enemy at the time of firing and reaching towards
 * the positions of all enemies
 */
public class WaveImpl implements Wave {
    private long createdTime;
    private Battlefield battlefield;
    private Point2D centre;
    private int radius;
    private double bulletSpeed;
    private java.util.List<WaveSectorImpl>sectors;
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
     */
    public WaveImpl(Enemy enemy, double bulletSpeed) {
        if (enemy == null) {return;}
        // Bullet was fired when we detected the energy drop, but we detect it one tick later
        this.createdTime = enemy.getBattlefield().getSelf().getTime() - 1;
        this.battlefield = enemy.getBattlefield();
        this.centre = enemy.getPosition();
        this.radius = (int)(bulletSpeed + 18); // Bullet starts at robot edge (18px) plus one tick of travel
        this.bulletSpeed = bulletSpeed;
        this.sectors = new ArrayList<WaveSectorImpl>();
        this.firingEnemy = new java.lang.ref.WeakReference<Enemy>(enemy);
        
        int enemyCount = 0;
        for (Obstacle e : battlefield) {
            if (!(e instanceof Enemy)) {continue;}
            Enemy en = (Enemy)e;
            if (en.getId().equals(enemy.getId())) {continue;}
            WaveSectorImpl sector = new WaveSectorImpl(en, this);
            this.sectors.add(sector);
            enemyCount++;
        }
        
        System.out.println("WAVE: " + enemy.getId() + " fired at speed " + bulletSpeed + " targeting " + enemyCount + " enemies at " + centre.getX() + "," + centre.getY() + " fireTime=" + createdTime + " currentTime=" + battlefield.getSelf().getTime());
    }
    @Override
    public Point2D getPosition() {return centre;}
    
    /**
     * Returns the combined outline of all obstacles (sectors) in this wave
     */
    @Override
    public Area getOutline() {
        Area combinedArea = new Area();
        for (Obstacle obstacle : sectors) {
            if (obstacle != null && obstacle.getOutline() != null) {
                combinedArea.add(obstacle.getOutline());
            }
        }
        return combinedArea;
    }

    @Override
    public void update() {
        // Calculate distance traveled by bullet (standard wave surfing calculation)
        // Bullets move before we detect them, so they've already traveled bulletSpeed distance
        long ticks = battlefield.getSelf().getTime() - createdTime;
        double distanceTraveled = ticks * bulletSpeed;
        this.radius = (int)distanceTraveled;
        
        System.out.println("WAVE UPDATE: ticks=" + ticks + " distanceTraveled=" + distanceTraveled + " radius=" + radius + " bulletSpeed=" + bulletSpeed);
        
        // Update all sectors
        for (WaveSectorImpl sector : sectors) {
            sector.update();
        }
        
        // get the distance from the centre to ourselves as it stands right now
        Point2D.Double selfpos = new Point2D.Double(battlefield.getSelf().getX(), battlefield.getSelf().getY());
        double distance = centre.distance(selfpos);
        
        // if the wave has passed us then remove the wave from battlefield (add robot radius for collision)
        if (distance < this.radius + 18) {
            this.getBattlefield().remove(this);
        }
    }
    
    /** irrelevant for waves */
    @Override
    public long lastSeen() {return 1000;}

    /**
     * Draws the outline of this obstacle for debugging purposes
     * @param g the Graphics2D context from onPaint method
     */
    public void drawOutline(java.awt.Graphics2D g) {
        // Always draw the wave circle first
        g.setColor(java.awt.Color.ORANGE);
        g.drawOval((int)(centre.getX() - radius), (int)(centre.getY() - radius), 
                   radius * 2, radius * 2);
        
        // Then draw any sectors
        for (Obstacle sector : sectors) {
            if (sector != null) {
                sector.drawOutline(g);
            }
        }
    }

    /**
     * Inner class representing an arc sector obstacle on the wave.
     */
    public class WaveSectorImpl implements Obstacle {
        private Wave wave;
        private double startAngle;
        private double endAngle;
        private Area outline;
        private java.lang.ref.WeakReference<Enemy>enemy;
        /**
         * Creates a circle sector shaped obstacle representing a ray
         * emanating from the firing enemy towards the given enemy
         * who's area represents the most likely location of the bullet
         * @param enemy An enemy which is potentially being fired AT
         * @param wave The wave created by the enemy that is doing the firing. Wave contains the enemy that is firing.
         */
        public WaveSectorImpl(Enemy enemy, Wave wave) {
            this.wave = wave;
            this.enemy = new java.lang.ref.WeakReference<Enemy>(enemy);
            
            Enemy source = wave.getEnemy();
            Enemy target = enemy;
            Point2D centre = wave.getCentre();
            Point2D targetPos = target.getPosition();
            
            // Calculate current angle to target (Robocode convention: 0 = North)
            this.startAngle = Math.atan2(targetPos.getX() - centre.getX(), targetPos.getY() - centre.getY());
            
            // Try to get predicted position, fallback to current position if prediction fails
            Point2D.Double interceptPos = new Point2D.Double(targetPos.getX(), targetPos.getY());
            try {
                if (target.getScanHistory().size() > 0) {
                    Predictor p = target.getScanHistory().peek().getPredictor();
                    if (p != null) {
                        Point2D.Double predicted = p.getIntercept(wave.getBulletSpeed(), source.getPosition());
                        if (predicted != null && !Double.isNaN(predicted.x) && !Double.isNaN(predicted.y)) {
                            interceptPos = predicted;
                        }
                    }
                }
            } catch (Exception e) {
                // Prediction failed, use current position
            }
            
            // Calculate end angle (Robocode convention: 0 = North)
            this.endAngle = Math.atan2(interceptPos.x - centre.getX(), interceptPos.y - centre.getY());
            
            // Ensure we have a minimum sector width for stationary targets
            double angleDiff = Math.abs(this.endAngle - this.startAngle);
            if (angleDiff < 0.1) { // Less than ~6 degrees
                this.endAngle = this.startAngle + 0.1; // Add minimum 6-degree sector
            }
        }
        
        @Override
        public Area getOutline() {
            if (this.outline == null) {
                update(); // Initialize outline if not set
            }
            return this.outline;
        }

        /** irrelevant for waves sectors */
        @Override
        public long lastSeen() {return 1000;}

        @Override
        public void update() {
            double radius = this.wave.getRadius();
            double sweepAngle = endAngle - startAngle;
            
            // Normalize sweep angle to [-π, π]
            while (sweepAngle > Math.PI) sweepAngle -= 2 * Math.PI;
            while (sweepAngle < -Math.PI) sweepAngle += 2 * Math.PI;
            
            // Convert from Robocode angles (0 = North) to Java 2D angles (0 = East)
            double java2DStartAngle = Math.toDegrees(startAngle - Math.PI/2);
            
            Area ret = new Area(new java.awt.geom.Arc2D.Double(
                centre.getX() - radius, centre.getY() - radius,
                radius * 2, radius * 2,
                java2DStartAngle, 
                Math.toDegrees(sweepAngle), java.awt.geom.Arc2D.PIE
            ));
            this.outline = ret;
        }

        @Override
        public Point2D getPosition() {
            return centre;
        }

        @Override
        public Battlefield getBattlefield() {return battlefield;}

        @Override
        public void drawOutline(Graphics2D g) {
            Area outline = getOutline();
            if (outline != null) {
                g.setColor(java.awt.Color.ORANGE);
                g.draw(outline);
            }
        }
    }
}
