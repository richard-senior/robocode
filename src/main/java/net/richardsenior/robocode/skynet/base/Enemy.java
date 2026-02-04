package net.richardsenior.robocode.skynet.base;

import java.awt.geom.Point2D;
import java.util.Deque;
import java.util.List;

/**
 * Enemy robot interface extending Obstacle.
 * Represents an enemy that should be tracked and avoided.
 */
public interface Enemy extends Obstacle {
    // handler for all event types (ScannedRobotEvent etc)
    public void update(robocode.Event event);
    // get the ID of this Enemy, which is usually its name
    public String getId();
    /* 
        returns the bearing and distance to the enemy relative to our robot
        With x being distance and y being the absolute bearing in radians
    */
    public Point2D.Double getPolarPosition();
    // gets the last known centre position of the enemy in absolute cartesian coordinates
    public Point2D.Double getPosition();
    // predicts where the enemy will be in n ticks based on this implementation's hueristics
    // the base implementation will use quadratic curve fitting on the last few scans
    public Point2D.Double predictPosition(int ticks);
    // returns an ordered structure 'n' in length of sightings of this enemy
    public Deque<? extends EventWrapper> getScanHistory();
    // returns the waves fired by this enemy
    public List<Wave> getWaves();
    // returns the gun used to target this specific enemy
    public Gun getGun();
    // returns the predictor used for this enemy's movement
    public Predictor getPredictor();
}