package net.richardsenior.robocode.skynet.base;

import java.awt.geom.Point2D;
import java.util.Queue;

/**
 * Enemy robot interface extending Obstacle.
 * Represents an enemy that should be tracked and avoided.
 */
public interface Enemy extends Obstacle {
    // handler for all event types (ScannedRobotEvent etc)
    public void update(robocode.Event event);
    // get the ID of this Enemy, which is usually its name
    public String getId();
    // gets the last known centre position of the enemy in absolute cartesian coordinates
    public Point2D.Double getPosition();
    // predicts where the enemy will be in n ticks based on this implementation's hueristics
    // the base implementation will use quadratic curve fitting on the last few scans
    public Point2D.Double predictPosition(int ticks);
    // returns an ordered structure 'n' in length of sightings of this enemy
    public Queue<? extends EventWrapper> getScanHistory();
}