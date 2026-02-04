package net.richardsenior.robocode.skynet.base.impl;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Enemy;
import net.richardsenior.robocode.skynet.base.Obstacle;
import net.richardsenior.robocode.skynet.base.Scanner;
import net.richardsenior.robocode.skynet.base.Mover;
import net.richardsenior.robocode.skynet.base.EventWrapper;
import net.richardsenior.robocode.skynet.base.Wave;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import robocode.*;
import java.util.ArrayDeque;
import java.util.Set;
/**
 * @see net.richardsenior.robocode.skynet.base.Battlefield
 * Implmementation of Battlefield underpinned by a WeakHashSet of Obstacles.
 * This allows Obstacles such as bullets and dead enemies to be automatically removed from the battlefield
 * Battlefield is usually injected (IOC) into other objects maintaining a central source of information
 * @author Richard Senior
 */
public class BattlefieldImpl implements Battlefield {
    private static final double WALL_EXCLUSION_ZONE = 28.0;
    private static final int LAGRANGE_GRID_SIZE = 10; // 10x10 grid for sampling
    /** Enemies, and Waves etc. */
    private Set<Obstacle> obstacles;
    /** The currently targetted enemy */
    private Enemy target;
    /** Us (Our tank) implemented as an Obstacle */
    private Enemy selfEnemy;
    /** The Robocode Robot Object */
    private AdvancedRobot self;
    /** Our radar implementation */
    private Scanner scanner;
    /** Implements our movement strategies */
    private Mover mover;    
    private Enemy[] enemies;
    /** The time in ticks since we last saw the least recently scanned enemy */
    private long oldestScan = 0;
    /** A point on the battlefield which is least densely populated. Like the inverse of centre of gravity */
    private Point2D.Double lagrangePoint;

    /** Interface defined access methods */
    @Override
    public Scanner getScanner() {return this.scanner;}
    @Override
    public Mover getMover() {return this.mover;}
    @Override
    public AdvancedRobot getSelf() {return this.self;}
    @Override
    public Point2D.Double getLagrangePoint() {return this.lagrangePoint;}
    
    /** Access methods peculiar to this implementation of the Battlefield Interface */
    public Enemy getSelfAsEnemy() {return this.selfEnemy;}
    

    // constructor
    public BattlefieldImpl(AdvancedRobot self) {
        this.self = self;                
        this.obstacles = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<Obstacle, Boolean>());
        // create an Enemy class which represents this robot (for wave generation only)
        this.selfEnemy = new SelfEnemyImpl(this);
        this.obstacles.add(this.selfEnemy);
        this.scanner = new ScannerImpl(this);
        this.mover = new MoverImpl(this);
    }

    private void calculateLagrangePoint() {
        if (this.enemies == null || this.enemies.length == 0) {
            this.lagrangePoint = null;
            return;
        }
        
        double fieldWidth = self.getBattleFieldWidth();
        double fieldHeight = self.getBattleFieldHeight();
        double cellWidth = fieldWidth / LAGRANGE_GRID_SIZE;
        double cellHeight = fieldHeight / LAGRANGE_GRID_SIZE;
        
        java.awt.geom.Point2D.Double bestSpot = null;
        double maxMinDistance = 0;
        
        for (int x = 0; x < LAGRANGE_GRID_SIZE; x++) {
            for (int y = 0; y < LAGRANGE_GRID_SIZE; y++) {
                java.awt.geom.Point2D.Double testPoint = new java.awt.geom.Point2D.Double(
                    (x + 0.5) * cellWidth, 
                    (y + 0.5) * cellHeight
                );
                
                // Find distance to nearest enemy
                double minDistSq = Double.POSITIVE_INFINITY;
                for (Enemy enemy : this.enemies) {
                    java.awt.geom.Point2D.Double enemyPos = enemy.getPosition();
                    if (enemyPos != null) {
                        double distSq = testPoint.distanceSq(enemyPos);
                        if (distSq < minDistSq) {
                            minDistSq = distSq;
                        }
                    }
                }
                
                if (minDistSq > maxMinDistance) {
                    maxMinDistance = minDistSq;
                    bestSpot = testPoint;
                }
            }
        }
        
        this.lagrangePoint = bestSpot;
    }
    
    @Override
    public Enemy getTarget() {
        if (this.enemies == null || this.enemies.length == 0) {
            this.target = null;
            return null;
        }
        
        Point2D.Double myPos = new Point2D.Double(self.getX(), self.getY());
        
        Enemy closest = null;
        double closestDistSq = Double.POSITIVE_INFINITY;
        
        for (Enemy enemy : this.enemies) {
            if (enemy.getPosition() != null) {
                double distSq = myPos.distanceSq(enemy.getPosition());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = enemy;
                }
            }
        }
        
        this.target = closest;
        return this.target;
    }
    
    @Override
    public void setTarget(Enemy e) {this.target = e;}
    
    @Override
    public void update() {   
        this.scanner.scan();        
        // Update all obstacles        
        ArrayDeque<Enemy>enemies = new ArrayDeque<Enemy>();
        this.oldestScan = Long.MAX_VALUE;
        for (Obstacle obstacle : obstacles) {
            // maintain a count of how many enemies we have scanned
            if (obstacle instanceof Enemy && !(obstacle instanceof SelfEnemyImpl)) {                
                enemies.add((Enemy)obstacle);
                if (((Enemy)obstacle).getScanHistory() != null && !((Enemy)obstacle).getScanHistory().isEmpty()) {
                    // Get the LAST (most recent) scan, not the first (oldest)
                    EventWrapper mostRecent = ((Enemy)obstacle).getScanHistory().getLast();
                    long t = mostRecent.getTime();
                    // how long ago did we scan the the whole battlefield?
                    if (t < this.oldestScan) {this.oldestScan = t;}
                }
            }
            obstacle.update();
        }
        this.enemies = enemies.toArray(new Enemy[enemies.size()]);
        
        // Calculate Lagrange point (least densely populated spot)
        calculateLagrangePoint();
        
        this.mover.update();
        this.mover.doMove();
        
        // Get target (will auto-select if none exists)
        Enemy selectedTarget = this.getTarget();
        
        // Update and fire gun for selected target
        if (selectedTarget != null && selectedTarget.getGun() != null) {
            selectedTarget.getGun().update();
            selectedTarget.getGun().fire();
        }
    }

    @Override
    public void update(robocode.Event event) {
        switch (event.getClass().getSimpleName()) {
            case "RobotDeathEvent":
                RobotDeathEvent rde = (RobotDeathEvent) event;
                String deadRobotName = rde.getName();
                // null this target
                if (this.getTarget()!=null && this.getTarget().getId().equals(deadRobotName)) {this.setTarget(null);}
                // Remove it from the obstacles seet
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
            case "HitWallEvent":
                if (mover != null) {
                    mover.onHitWall();
                }
                break;
            default: return;                
        }
    }
    @Override
    public int getEnemyCount() {return this.enemies != null ? this.enemies.length : 0;}
    @Override
    public long getOldestSighting() {return this.oldestScan;}
    @Override
    public Enemy[] getEnemies() {return this.enemies;}

    
    public Area getBattlefieldArea(int ticks) {
        // create a rectangle the size of the battlefield, minus a constant amount such that we don't go into the walls
        Area area = new Area(new java.awt.geom.Rectangle2D.Double(WALL_EXCLUSION_ZONE, WALL_EXCLUSION_ZONE, 
            getSelf().getBattleFieldWidth() - 2 * WALL_EXCLUSION_ZONE, getSelf().getBattleFieldHeight() - 2 * WALL_EXCLUSION_ZONE));
        
        int obstacleCount = 0;
        int enemyCount = 0;
        
        // iterate all obstacles and subtract them from this rectangle
        for (Obstacle obstacle : obstacles) {
            // Skip selfEnemy - we don't want to block our own sensor pixels
            if (obstacle instanceof SelfEnemyImpl) {
                continue;
            }
            
            Area outline = obstacle.getOutline(ticks);
            if (outline != null && !outline.isEmpty()) {
                // Test: does the outline actually contain points?
                java.awt.geom.Rectangle2D bounds = outline.getBounds2D();
                if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                    area.subtract(outline);
                    obstacleCount++;
                    if (obstacle instanceof Enemy) {
                        enemyCount++;
                    }
                }
            }
        }
        
        return area;
    }

    @Override
    public Enemy getEnemy(String name) {
        if (name == null) {throw new IllegalArgumentException("Enemy name cannot be null");}
        // Search in obstacles set, not the enemies array
        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof Enemy && !(obstacle instanceof SelfEnemyImpl)) {
                if (((Enemy)obstacle).getId().equals(name)) {
                    return (Enemy)obstacle;
                }
            }
        }
        // create new enemy and add it
        Enemy e = new EnemyImpl(name, this);
        this.add(e);
        return e;
    }
    
    @Override
    public void onPaint(java.awt.Graphics2D g) {
        // Draw battlefield border
        g.setColor(java.awt.Color.GREEN);
        g.drawRect((int)WALL_EXCLUSION_ZONE, (int)WALL_EXCLUSION_ZONE, 
            (int)(getSelf().getBattleFieldWidth() - 2 * WALL_EXCLUSION_ZONE),
            (int)(getSelf().getBattleFieldHeight() - 2 * WALL_EXCLUSION_ZONE));
        
        // Draw obstacles (enemies and waves)
        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof EnemyImpl) {
                ((EnemyImpl)obstacle).drawOutline(g);
            } else if (obstacle instanceof Wave) {
                // Draw wave annular segments
                Area waveOutline = obstacle.getOutline(0);
                if (waveOutline != null) {
                    g.setColor(new java.awt.Color(255, 0, 0, 100)); // Semi-transparent red
                    g.fill(waveOutline);
                    g.setColor(java.awt.Color.RED);
                    g.draw(waveOutline);
                }
            }
        }
        
        // Draw waves from enemies
        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof Enemy) {
                Enemy enemy = (Enemy)obstacle;
                for (Wave wave : enemy.getWaves()) {
                    Area waveOutline = wave.getOutline(0);
                    if (waveOutline != null) {
                        g.setColor(new java.awt.Color(255, 100, 0, 80)); // Semi-transparent orange
                        g.fill(waveOutline);
                        g.setColor(java.awt.Color.ORANGE);
                        g.draw(waveOutline);
                    }
                }
            }
        }
        
        // Draw mover and scanner
        if (mover != null) {mover.doPaint(g);}
        if (scanner != null) {scanner.doPaint(g);}
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
