package net.richardsenior.robocode.skynet.base.impl;

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.Queue;

import javax.management.RuntimeErrorException;

import java.util.LinkedList;
import net.richardsenior.robocode.skynet.base.Wave;
import net.richardsenior.robocode.skynet.base.Predictor;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Enemy;
import robocode.*;


/**
 * Basic Implementation of Enemy interface.
 * An Enemy is an 'Obstacle' with other properties.
 * Enemies can be 'targetted' and also 'avoided' by movement algorithms.
 * @author Richard Senior
 */
public class EnemyImpl implements Enemy {
    private String id;
    private Queue<EventWrapperImpl>scanHistory;
    private Area outline;
    private Point2D.Double position;
    private Battlefield battlefield;
    private double lastEnemyEnergy = 100.0; // Default starting energy
    
    @Override
    public Point2D.Double getPosition() {
        if (position == null) {throw new NullPointerException("Position not yet set for Enemy");}
        return this.position;
    }    
    @Override
    public String getId() {return this.id;}
    @Override
    public Queue<EventWrapperImpl> getScanHistory() {return this.scanHistory;}
    @Override
    public Area getOutline() {return outline;}   
    @Override
    public Battlefield getBattlefield() {return battlefield;}

    /** Constructor */
    public EnemyImpl(String id, Battlefield battlefield) {  
        this.id = id;      
        this.scanHistory = new LinkedList<EventWrapperImpl>();
        this.battlefield = battlefield;
    }

    @Override
    public Point2D.Double predictPosition(int ticks) {
        if (scanHistory.size() < 3) {return this.position;}
        return scanHistory.peek().getPredictor().predict(ticks);
    }

    @Override
    public void update() {
        // enemies have no per-tick update behaviour currently
        // see update(Event) instead
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
                double enemyX = self.getX() + sre.getDistance() * Math.sin(absoluteBearing);
                double enemyY = self.getY() + sre.getDistance() * Math.cos(absoluteBearing);
                s.setPosition(new Point2D.Double(enemyX, enemyY));
                updateSelf(s);
                break;
            case "BulletHitEvent":
                break;
            default: return;                
        }
    }

    // may be overriden by extending classes that wish to provide a faux event
    protected void updateSelf(EventWrapperImpl event) {
        if (event.getPosition() == null) {throw new RuntimeException("this method expects event to contain a populated position");}
        // Update position first
        this.position = new Point2D.Double(event.getPosition().x, event.getPosition().y);
        // reset outline position
        this.outline = new Area(new java.awt.geom.Ellipse2D.Double(this.position.x - 18, this.position.y - 18, 36, 36));
        // maintain only last N scans?
        while (scanHistory.size() > 5) {scanHistory.poll();}                
        // append x and y to 
        scanHistory.add(event);        
        this.updatePredictorAndFiringEvent();
    }

    // May be overriden by extending classes to prevent recalculation of the predictor
    protected void updatePredictorAndFiringEvent() {        
        Predictor p = new PolynomialPredictor(this.getScanHistory());
        EventWrapperImpl event = this.getScanHistory().peek();
        event.setPredictor(p);
        // if this enemy has fired, then create a wave and add it to Battlefield
        doHasFired(event);
    }

    /**
     * If this enemy has fired then create a wave and add it to battlefield
     * @param s
     */
    private void doHasFired(EventWrapperImpl s) {
        // has this enemy fired?
        double currentEnergy = s.getEnergy();
        // energy drop is previous energy - current energy  
        double ed = lastEnemyEnergy - currentEnergy;
        System.out.println("Energy drop: " + ed + " (last: " + lastEnemyEnergy + ", current: " + currentEnergy + ")");
        
        if (ed < 3.01 && ed > 0.09) {
            System.out.println("Enemy fired! Creating wave...");
            // enemy has fired, create a wave here
            double velocity = 20.0 - (3.0 * ed);
            Wave wave = new WaveImpl(this, velocity);
            this.getBattlefield().add(wave);
            System.out.println("Wave created and added to battlefield");
        }
        
        // Update last energy for next comparison
        lastEnemyEnergy = currentEnergy;
    }

    /**
     * Draws the outline of this obstacle for debugging purposes
     * @param g the Graphics2D context from onPaint method
     */
    public void drawOutline(java.awt.Graphics2D g) {
        if (outline != null) {
            g.setColor(java.awt.Color.RED);
            g.draw(outline);
        }
    }
    
}

