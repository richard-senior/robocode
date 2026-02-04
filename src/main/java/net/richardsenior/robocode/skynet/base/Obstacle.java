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
     * Gets the outline of this obstacle as it is now (ticks=0)
     * or at 'ticks' in the future.
     * Using any internal prediction mechanisms required
     * The outline may be multiple 'Area' objects 'summed' into one Area
     * @param ticksFuture
     * @return an area in the form of a closed path
     */
    public Area getOutline(int ticks);

    /**
     *  when was the last confirmed sighting (scan) of this obstacle
     *  in ticks
     */
    public long lastSeen();
}