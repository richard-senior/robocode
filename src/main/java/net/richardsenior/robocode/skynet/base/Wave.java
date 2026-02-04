package net.richardsenior.robocode.skynet.base;
import java.awt.geom.Point2D;

/**
 * A wave is an expanding circle representing all possible locations of a bullet fired by an opponent.
 * When an opponent fires, we create a wave centered on the firing opponent's position at the time of firing.
 * The wave expands outward at the bullet speed, representing all possible locations the bullet could be.
 * By tracking waves, we can predict dangerous areas on the battlefield to avoid, improving our survival chances.
 */
public interface Wave extends Obstacle {    
    /**
     * Gets the center point of the wave.
     * @return the center position of the wave
     */
    public Point2D getCentre();
    
    /**
     * Gets the current radius of the wave.
     * @return the radius in pixels from the origin point
     */
    public int getRadius();
    // returns the enemy that created this wave
    // may return null if the enemy is dead, but the wave still exists
    public Enemy getEnemy();
    // the tick number on which this wave was created
    public long getCreatedTime();
    // the speed of the emenating wave
    public double getBulletSpeed();
}
