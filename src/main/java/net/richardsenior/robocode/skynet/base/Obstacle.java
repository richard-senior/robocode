package net.richardsenior.robocode.skynet.base;
import java.awt.geom.Area;
import java.awt.geom.Point2D;

/**
 * Any kind of battlefield obstacle.
 * Something which should be avoided in terms of movement.
 */
public interface Obstacle {

    // per tick update of this obstacle
    // some obstacles may not need updating every tick
    public void update();
    /**
     * Gets the outline/shape of this obstacle.
     * If it's a robot it will be a rectangle, if it's a wave it will be a circle etc.
     * If its an individual wave obstacle it will be a sector of a circle.
     * @return the area representing this obstacle's shape
     */
    public Area getOutline();
    
    /**
     * Gets the current position of this obstacle (the last scanned location)
     * @return the position as a Point2D
     */
    public Point2D getPosition();

    /**
     * Gets the battlefield on which this obstacle exists
     * Probably given at construction time
     * @return
     */
    public Battlefield getBattlefield();

    /**
     * Draws the outline of this obstacle's area shape for debugging purposes
     * @param g the Graphics2D context from onPaint method
     */
    public void drawOutline(java.awt.Graphics2D g);

    /**
     *  when was the last confirmed sighting (scan) of this obstacle
     *  in ticks
     */
    public long lastSeen();
}