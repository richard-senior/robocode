package net.richardsenior.robocode.skynet.base.impl;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Enemy;
import net.richardsenior.robocode.skynet.base.Obstacle;
import net.richardsenior.robocode.skynet.base.Scanner;
import net.richardsenior.robocode.skynet.base.Wave;
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
    private Scanner scanner;
    private int scannedEnemies = 0;
    private long oldestScan = 0;

    public AdvancedRobot getSelf() {return this.self;}
    public Enemy getSelfAsEnemy() {return this.selfEnemy;}

    // private constructor to prevent instantiation from outside the class
    public BattlefieldImpl(AdvancedRobot self) {
        this.self = self;                
        this.obstacles = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<Obstacle, Boolean>());
        // create an Enemy class which represents this robot
        this.selfEnemy = new SelfEnemyImpl(this);
        this.obstacles.add(this.selfEnemy);
        this.scanner = new ScannerImpl(this);
        this.cells = new double[10][10]; // example: 10x10 grid of cells
    }
    
    @Override
    public void update() {   
        this.scanner.scan();        
        // Update all obstacles
        this.scannedEnemies = 0;
        this.oldestScan = Long.MAX_VALUE;
        for (Obstacle obstacle : obstacles) {
            // maintain a count of how many enemies we have scanned
            if (obstacle instanceof Enemy && !(obstacle instanceof SelfEnemyImpl)) {
                this.scannedEnemies++;
                long t = ((Enemy)obstacle).getScanHistory().peek().getTime();
                // how long ago did we scan the the whole battlefield?
                if (t < this.oldestScan) {this.oldestScan = t;}
            }
            obstacle.update();
        }
    }

    @Override
    public void update(robocode.Event event) {
        switch (event.getClass().getSimpleName()) {
            case "RobotDeathEvent":
                RobotDeathEvent rde = (RobotDeathEvent) event;
                String deadRobotName = rde.getName();
                obstacles.removeIf(obstacle -> 
                    obstacle instanceof Enemy && 
                    ((Enemy) obstacle).getId().equals(deadRobotName)
                );
                break;
            case "ScannedRobotEvent":                
                ScannedRobotEvent sre = (ScannedRobotEvent) event;
                Enemy enemy = this.getEnemy(sre.getName());
                enemy.update(sre);
                break;
            default: return;                
        }
    }
    @Override
    public int getEnemyCount() {return this.scannedEnemies;}
    @Override
    public long getOldestSighting() {return this.oldestScan;}

    @Override
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
    
    @Override
    public void onPaint(java.awt.Graphics2D g) {
        // Draw debug info box
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(10, 10, 300, 200);
        g.setColor(java.awt.Color.WHITE);
        g.drawRect(10, 10, 300, 200);
        
        // Draw obstacle list
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
        g.drawString("Obstacles (" + obstacles.size() + "):", 15, 30);
        
        int y = 50;
        int count = 0;
        for (Obstacle obstacle : obstacles) {
            if (count >= 12) {
                g.drawString("... (" + (obstacles.size() - count) + " more)", 15, y);
                break;
            }
            
            String className = obstacle.getClass().getSimpleName();
            String name = "Unknown";
            
            if (obstacle instanceof Enemy) {
                name = ((Enemy) obstacle).getId();
            } else if (obstacle instanceof Wave) {
                Wave wave = (Wave) obstacle;
                name = "R:" + wave.getRadius();
            }
            
            g.drawString(count + ": " + className + " - " + name, 15, y);
            y += 15;
            count++;
        }
        
        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            obstacle.drawOutline(g);
        }
    }
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
