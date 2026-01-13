package net.richardsenior.robocode.skynet.base.impl;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Enemy;
import net.richardsenior.robocode.skynet.base.Obstacle;
import robocode.*;
import java.util.Set;
/**
 * @see net.richardsenior.robocode.skynet.base.Battlefield
 * Implmementation of Battlefield underpinned by a WeakHashSet of Obstacles.
 * This allows Obstacles such as bullets and dead enemies to be automatically removed from the battlefield
 * Implemented as a singleton allowing global access to battlefield data.
 * Implements an aribtrary grid of cells for spatial partitioning.
 * @author Richard Senior
 */
public class BattlefieldImpl implements Battlefield {
    private Set<Obstacle> obstacles;
    private Enemy selfEnemy;
    private AdvancedRobot self;
    private double[][] cells;

    public AdvancedRobot getSelf() {return this.self;}

    // private constructor to prevent instantiation from outside the class
    public BattlefieldImpl(AdvancedRobot self) {
        this.self = self;                
        this.obstacles = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<Obstacle, Boolean>());
        // create an Enemy class which represents this robot
        this.selfEnemy = new SelfEnemyImpl(this);
        this.obstacles.add(this.selfEnemy);
        this.cells = new double[10][10]; // example: 10x10 grid of cells
    }   
    
    @Override
    public void update() {           
        for (Obstacle obstacle : obstacles) {
            obstacle.update();
        }
    }

    @Override
    public void update(robocode.Event event) {
        // big switch/case statement of event types           
        switch (event.getClass().getSimpleName()) {
            case "BulletHitEvent":
                // handle bullet hit event
                break;
            case "RobotDeathEvent":
                // handle robot death event
                break;
            case "ScannedRobotEvent":
                // determine which robot was scanned and update it or add it etc.
                ScannedRobotEvent sre = (ScannedRobotEvent) event;
                Enemy enemy = this.getEnemy(sre.getName());
                enemy.update(sre);
                break;
            default: return;                
        }
    }

    public Enemy getEnemy(String name) {
        if (name == null) {throw new IllegalArgumentException("Enemy name cannot be null");}
        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof Enemy && ((Enemy) obstacle).getId().equals(name)) {
                return (Enemy) obstacle;
            }
        }
        // create new enemy and add it
        Enemy e = new EnemyImpl(name, this);
        this.add(e);
        return e;
    }

    public void onPaint(java.awt.Graphics2D g) {
        // draw obstacles
        for (Obstacle obstacle : obstacles) {
            obstacle.drawOutline(g);
        }
    }

    /**************************
    Geometry Utility methods
    ***************************/


    /**************************
    Set Delegate Methods
    Implelement the Set interface using a weak hash set as the backing store
    ***************************/
    @Override
    public int size() {return obstacles.size();}
    @Override
    public boolean isEmpty() {return obstacles.isEmpty();}
    @Override
    public boolean contains(Object o) {return obstacles.contains(o);}
    @Override
    public java.util.Iterator<Obstacle> iterator() {return obstacles.iterator();}
    @Override
    public Object[] toArray() {return obstacles.toArray();}
    @Override
    public <T> T[] toArray(T[] a) {return obstacles.toArray(a);}
    @Override
    public boolean add(Obstacle obstacle) {return obstacles.add(obstacle);}
    @Override
    public boolean remove(Object o) {return obstacles.remove(o);}
    @Override
    public boolean containsAll(java.util.Collection<?> c) {return obstacles.containsAll(c);}
    @Override
    public boolean addAll(java.util.Collection<? extends Obstacle> c) {return obstacles.addAll(c);}
    @Override
    public boolean retainAll(java.util.Collection<?> c) {return obstacles.retainAll(c);}
    @Override
    public boolean removeAll(java.util.Collection<?> c) {return obstacles.removeAll(c);}
    @Override
    public void clear() {obstacles.clear();}
}
