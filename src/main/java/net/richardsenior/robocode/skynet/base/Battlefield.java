package net.richardsenior.robocode.skynet.base;

import java.util.Set;
import robocode.AdvancedRobot;
import java.awt.geom.Area;

/**
 * A Set of Obstacles, some of which may be Enemies etc.
 * Probably should be implemented with weak references
 * Also should probably implement Robot or AdvancedRobot so that we can do absolute positioning etc.
 * @author Richard Senior
 */
public interface Battlefield extends Set<Obstacle> {
    /*
    Called on every tick (or every n'th tick)
    Here update any battlefield-wide data, such as recalculating cell gravity values etc.
    */
    public void update();

    // universal event handler (all events, such as BulletHit, RobotDeath etc passed here)
    public void update(robocode.Event event);

    /** 
     * if we have designated a target then we can get it here 
     * If the target dies this will be unset so
     * this could return null
     */
    public Enemy getTarget();
    /** 
     * set a target 
    */
    public void setTarget(Enemy e);

    /*
    searches self for any 'Obstacle' of type Enemy with the given name
    if no such Enemy exists return a new Enemy instance with that name
    and adds it to the Set. Note that in this case the enemy will be 'empty' except for name
    Override this method to return different implementations of Enemy
    */
    public Enemy getEnemy(String name);
    
    /**
     * Returns just the Enemy implementations from our collection, excluding ourself.
     * That is, returns our remaining opponents
     * @return
     */
    public Enemy[] getEnemies();
    /**
     * Returns the number of obstacles in the set, minus one (our robot)
     * That is the number of actual enemies
     * When compared to getSelf().getOthers() can be useful in determining
     * whether we have scanned all opponents (determine scanner activity)
     * @return
     */
    public int getEnemyCount();
    /**
     * Returns the tick number of the obstacle with the smallest 
     * lastSeen tick number (the oldest scan)
     * Useful for determining scanner activity
     * @return
     */
    public long getOldestSighting();
    /**
     * Gets the actual robot instance (our robot)
     * @return Robot or AdvancedRobot instance etc.
     */
    public AdvancedRobot getSelf();

    /** Gets our implementation of the radar */
    public Scanner getScanner();
    /** Gets our implementation of the mover */
    public Mover getMover();
    
    /**
     * Gets the Lagrange point - the least densely populated spot on the battlefield
     * @return Point2D representing the safest location away from enemies
     */
    public java.awt.geom.Point2D.Double getLagrangePoint();
    
    /*
    * used in debug to draw battlefield hazards etc.
    */
    public void onPaint(java.awt.Graphics2D g);

    /**
     * Returns an Area representing the entire battlefield
     * That is, the Outlines of all obstacles on the battlefield inside a rectangle.
     * The 'solid' part of the outline is 'safe space'
     * That is anything we should avoid is 'subtracted' from this shape
     * @param ticks The number of ticks from now, in which we want to try and determine what the battlefield will look like
     * @return
     */
    public Area getBattlefieldArea(int ticks);

    /****************
    Static Utility Methods
    *****************/
}
