package net.richardsenior.robocode.skynet.base;

import java.util.Set;
import robocode.AdvancedRobot;

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

    /*
    searches self for any 'Obstacle' of type Enemy with the given name
    if no such Enemy exists return a new Enemy instance with that name
    and adds it to the Set. Note that in this case the enemy will be 'empty' except for name
    Override this method to return different implementations of Enemy
    */
    public Enemy getEnemy(String name);
    /**
     * Returns the number of Enemy obstacles in the set, minus one (us)
     * @return
     */
    public int getEnemyCount();
    /**
     * Returns the tick number of the obstacle with the smallest 
     * lastSeen tick number
     * That is the oldest scan
     * @return
     */
    public long getOldestSighting();

    // the Robot implementation
    public AdvancedRobot getSelf();
    // used in debug to draw battlefield hazards etc.
    public void onPaint(java.awt.Graphics2D g);


    /****************
    Static Utility Methods
    *****************/


}
