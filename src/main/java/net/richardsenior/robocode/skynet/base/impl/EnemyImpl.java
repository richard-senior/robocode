package net.richardsenior.robocode.skynet.base.impl;

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import net.richardsenior.robocode.skynet.base.Wave;
import net.richardsenior.robocode.skynet.base.Predictor;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Enemy;
import net.richardsenior.robocode.skynet.base.Gun;
import robocode.*;


/**
 * Basic Implementation of Enemy interface.
 * An Enemy is an 'Obstacle' with other properties allowing
 * targetting and avoidance strategies etc.
 * @author Richard Senior
 */
public class EnemyImpl implements Enemy {
    private static final double MAX_ROBOT_SPEED = 8.0; // pixels per tick
    private static final double MAX_RADAR_TURN_RATE = 45.0; // degrees per tick
    private static final double FULL_ROTATION_TICKS = 360.0 / MAX_RADAR_TURN_RATE; // 8 ticks
    private static final double OUTLINE_RADIUS = MAX_ROBOT_SPEED * FULL_ROTATION_TICKS + 18.0; // 64 + 18 = 82 pixels
    private static final double OUTLINE_DIAMETER = OUTLINE_RADIUS * 2;
    private static final double GUN_COOLING_RATE = 0.1; // per tick
    
    private String id;
    private Deque<EventWrapperImpl>scanHistory;
    private Point2D.Double position;
    private Point2D.Double polarPosition;
    private Battlefield battlefield;
    private double energy = 100;
    private List<Wave> waves;
    private Gun gun;
    private double lastBulletPower = 0;
    private long lastBulletFireTime = 0;
    private double wallHitDamage = 0;
    
    @Override
    public Point2D.Double getPosition() {
        if (position == null) {throw new NullPointerException("Position not yet set for Enemy");}
        return this.position;
    }
    @Override
    public Point2D.Double getPolarPosition() {
        if (this.polarPosition == null) {throw new NullPointerException("Polar Position not yet set for Enemy");}
        return this.polarPosition;
    }    
    @Override
    public String getId() {return this.id;}
    @Override
    public Deque<EventWrapperImpl> getScanHistory() {return this.scanHistory;}
    @Override
    public Battlefield getBattlefield() {return battlefield;}
    @Override
    public List<Wave> getWaves() {return waves;}
    @Override
    public Gun getGun() {return gun;}
    @Override
    public Predictor getPredictor() {
        if (scanHistory.isEmpty()) return null;
        return scanHistory.getLast().getPredictor();
    }

    /** Constructor */
    public EnemyImpl(String id, Battlefield battlefield) {  
        this.id = id;      
        this.scanHistory = new LinkedList<EventWrapperImpl>();
        this.battlefield = battlefield;
        this.waves = new ArrayList<>();
        this.gun = new GuessGunImpl(battlefield, this);
    }

    @Override
    public Point2D.Double predictPosition(int ticks) {
        if (scanHistory.size() < 3) {return this.position;}
        Point2D.Double predicted = ((java.util.LinkedList<EventWrapperImpl>)scanHistory).getLast().getPredictor().predict(ticks);
        return predicted != null ? predicted : this.position;
    }

    @Override
    public long lastSeen() {
        if (this.scanHistory.isEmpty()) {return Long.MAX_VALUE;}
        return this.battlefield.getSelf().getTime() - ((java.util.LinkedList<EventWrapperImpl>)this.scanHistory).getLast().getTime();
    }

    @Override
    public void update() {
        // Update all waves and remove expired ones
        Point2D.Double selfPos = new Point2D.Double(battlefield.getSelf().getX(), battlefield.getSelf().getY());
        waves.removeIf(wave -> {
            wave.update();
            double distance = wave.getCentre().distance(selfPos);
            return distance < wave.getRadius() + 18; // wave passed us (18 = robot radius)
        });
    }

    @Override
    public void update(robocode.Event event) {
        if (event == null) {return;}
        switch (event.getClass().getSimpleName()) {
            case "ScannedRobotEvent":
                ScannedRobotEvent sre = (ScannedRobotEvent) event;                
                // sanity check
                if (!this.getId().equals(sre.getName())) {
                    throw new IllegalArgumentException("ScannedRobotEvent name does not match Enemy ID");
                }
                // wrap this event
                EventWrapperImpl s = new EventWrapperImpl(sre);                                
                // now recalculate position, velocity, heading etc. 
                AdvancedRobot self = this.getBattlefield().getSelf();
                double absoluteBearing = self.getHeadingRadians() + sre.getBearingRadians();
                this.polarPosition = new Point2D.Double(sre.getDistance(), absoluteBearing);                
                double enemyX = self.getX() + sre.getDistance() * Math.sin(absoluteBearing);
                double enemyY = self.getY() + sre.getDistance() * Math.cos(absoluteBearing);
                s.setPosition(new Point2D.Double(enemyX, enemyY));
                updateSelf(s);
                break;
            default: return;
        }
    }

    // may be overriden by extending classes that wish to provide a faux event
    protected void updateSelf(EventWrapperImpl event) {
        if (event.getPosition() == null) {throw new RuntimeException("this method expects event to contain a populated position");}
        
        // Don't estimate wall damage - it causes too many false negatives
        // Wall hits are rare compared to bullet fires
        wallHitDamage = 0;
        
        // Update position first
        this.position = new Point2D.Double(event.getPosition().x, event.getPosition().y);
        // maintain only last N scans?
        while (scanHistory.size() > 10) {scanHistory.poll();}
        // Check for firing BEFORE adding new scan
        doHasFired(event);
        // append x and y to 
        scanHistory.add(event);
        this.updatePredictor();
    }

    // May be overriden by extending classes to prevent recalculation of the predictor
    protected void updatePredictor() {        
        Predictor p = new SimplePredictor(this.getScanHistory());
        EventWrapperImpl event = this.getScanHistory().getLast();
        event.setPredictor(p);
    }
    
    /**
     * Calculate enemy's gun heat at given time
     */
    private double getGunHeat(long time) {
        if (time <= 30) {
            // First 30 ticks, gun starts at 3.0 heat
            return Math.max(0, 3.0 - (time * GUN_COOLING_RATE));
        } else {
            // After first shot, calculate based on last bullet power
            return Math.max(0, 
                Rules.getGunHeat(lastBulletPower) - 
                ((time - lastBulletFireTime) * GUN_COOLING_RATE));
        }
    }

    /**
     * If this enemy has fired then create a wave and add it to this enemy's wave collection
     * @param s
     */
    protected void doHasFired(EventWrapperImpl newScan) {
        long currentTime = this.getBattlefield().getSelf().getTime();
        double currentEnergy = newScan.getEnergy();
        
        // Calculate energy drop accounting for wall damage
        double energyDrop = energy - currentEnergy - wallHitDamage;
        
        // Debug output
        if (energyDrop > 0.05) {
            System.out.println("Enemy " + id + " energy drop: " + energyDrop + 
                " (prev=" + energy + ", curr=" + currentEnergy + ", wall=" + wallHitDamage + ")");
        }
        
        // Check if energy drop indicates a bullet was fired
        // Use wider bounds for floating point precision
        if (energyDrop > 0.0999 && energyDrop < 3.0001) {
            // Check if gun heat allows firing
            double gunHeat = getGunHeat(currentTime);
            System.out.println("Enemy " + id + " potential fire detected! Gun heat: " + gunHeat);
            
            if (gunHeat < 0.0001) {
                double bulletPower = energyDrop;
                double velocity = 20.0 - (3.0 * bulletPower);
                
                // Calculate scan staleness (ticks since last scan)
                long lastScanTime = scanHistory.isEmpty() ? currentTime : scanHistory.getLast().getTime();
                int staleness = (int)(currentTime - lastScanTime);
                
                System.out.println("Creating wave for " + id + " with power " + bulletPower + 
                    ", velocity " + velocity + ", staleness " + staleness);
                
                Wave wave = new WaveImpl(this, velocity, staleness);
                this.waves.add(wave);
                wave.update();
                
                // Update gun heat tracking
                lastBulletPower = bulletPower;
                lastBulletFireTime = currentTime;
            }
        }
        
        // Reset wall damage and update energy
        wallHitDamage = 0;
        energy = currentEnergy;
    }
    
    public void adjustEnergyForBulletHit(double adjustment) {
        // This method is no longer needed with the simpler approach
    }

    @Override
    public Area getOutline(int ticks) {
        Point2D.Double pos = this.position;
        if (pos == null) {return new Area();}
        if (ticks != 0) {pos = this.predictPosition(ticks);}
        
        Area a = new Area(new java.awt.geom.Ellipse2D.Double(
            pos.x - OUTLINE_RADIUS, 
            pos.y - OUTLINE_RADIUS, 
            OUTLINE_DIAMETER, 
            OUTLINE_DIAMETER
        ));
        return a;
    }

    /**
     * Draws the outline of this obstacle for debugging purposes
     * We recalculate this on every capp to drawOutline because this method is only going
     * to be called onPaint, and most of the time paint will not be called.
     * Previously we recalculated this in update()
     * @param g the Graphics2D context from onPaint method
     */
    public void drawOutline(java.awt.Graphics2D g) {
        // Draw current position circle
        Area outline = this.getOutline(0);
        if (outline != null) {
            g.setColor(java.awt.Color.RED);
            g.draw(outline);
        }
        
        // Draw predicted movement direction arrow
        if (scanHistory.size() >= 3) {
            Point2D.Double currentPos = this.position;
            Point2D.Double predictedPos = this.predictPosition(10); // 10 ticks ahead
            
            if (currentPos != null && predictedPos != null && 
                currentPos.distance(predictedPos) > 1) { // Only draw if there's movement
                
                // Draw arrow from current to predicted position
                g.setColor(java.awt.Color.ORANGE);
                
                // Arrow line
                g.drawLine((int)currentPos.x, (int)currentPos.y, 
                          (int)predictedPos.x, (int)predictedPos.y);
                
                // Calculate direction vector
                double dx = predictedPos.x - currentPos.x;
                double dy = predictedPos.y - currentPos.y;
                double length = Math.sqrt(dx * dx + dy * dy);
                
                // Normalize
                dx /= length;
                dy /= length;
                
                // Arrow head at predicted position
                double arrowLength = 10;
                double arrowAngle = Math.PI / 6; // 30 degrees
                
                // Rotate the direction vector for arrowhead points
                double cos = Math.cos(arrowAngle);
                double sin = Math.sin(arrowAngle);
                
                // Left side of arrowhead
                int x1 = (int)(predictedPos.x - arrowLength * (dx * cos + dy * sin));
                int y1 = (int)(predictedPos.y - arrowLength * (dy * cos - dx * sin));
                
                // Right side of arrowhead
                int x2 = (int)(predictedPos.x - arrowLength * (dx * cos - dy * sin));
                int y2 = (int)(predictedPos.y - arrowLength * (dy * cos + dx * sin));
                
                g.drawLine((int)predictedPos.x, (int)predictedPos.y, x1, y1);
                g.drawLine((int)predictedPos.x, (int)predictedPos.y, x2, y2);
            }
        }
    }

    
}

