package net.richardsenior.robocode.skynet.base.impl;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Scanner;
import net.richardsenior.robocode.skynet.base.Enemy;
import java.awt.geom.Point2D;

/**
 * Basic implementation of Scanner
 * In 1v1 or when battlefield.getTarget() is not null then
 * the scanner will lock on the chosen enemy
 * Otherwise the scanner will try to operate optimally which is
 * If it can scan ALL remaining enemies without rotating 360 degrees then
 * it will do so, otherwise it will rotate 360 degrees.
 * If the battlefield data is stale then it will rotate 360 degrees
 */
public class ScannerImpl implements Scanner {
    private static final double SCAN_MARGIN = 0.175; // ~10 degrees in radians
    private static final long MAX_STALE = 10;
    private static final double TWO_PI = 2 * Math.PI;
    
    private Battlefield battlefield;
    private Point2D.Double massCentre, furthestClockwise, furthestAntiClockwise, myPos;
    // bearings for the positions above
    private double centre, clock, anticlock;
    // true clockwise, false anti
    private double wobbleDirection = 1;

    @Override
    public Point2D.Double getMassCentre() {return this.massCentre;}
    @Override
    public Battlefield getBattlefield() {return this.battlefield;}
    @Override
    public Point2D.Double getFurthestClockwise() {return furthestClockwise;}
    @Override
    public Point2D.Double getFurthestAntiClockwise() {return furthestAntiClockwise;}

    public ScannerImpl(Battlefield battlefield) {
        this.battlefield = battlefield;
        double centerX = battlefield.getSelf().getBattleFieldWidth() / 2;
        double centerY = battlefield.getSelf().getBattleFieldHeight() / 2;
        this.massCentre = new Point2D.Double(centerX, centerY);
        this.furthestAntiClockwise = new Point2D.Double(centerX, centerY);
        this.furthestClockwise = new Point2D.Double(centerX, centerY);
        this.myPos = new Point2D.Double();
        battlefield.getSelf().setAdjustGunForRobotTurn(true);
        battlefield.getSelf().setAdjustRadarForGunTurn(true);
    }

    /**
     * For speed and clarity of understanding we should use an if-return rather than if-else
     * paradigm for conditionals. For example if we are to rotate the scanner fully, then do it and return etc.
     * We should not change this paradigm
     */
    @Override
    public void scan() {
        var self = battlefield.getSelf();
        // Wait for the last scanner operation to finish
        if (self.getRadarTurnRemaining() > 0) return;
        // Initial Scan
        Enemy[] enemies = battlefield.getEnemies();
        if (enemies == null || enemies.length < 1) {fullScan(); return;}
        // Check if we've scanned all enemies
        if (battlefield.getEnemyCount() < self.getOthers()) {fullScan(); return;}
        // now determine if we need a full refresh
        long stalest = self.getTime() - battlefield.getOldestSighting();
        if (stalest > MAX_STALE) {fullScan(); return;}
        // Calculate actual enemy boundaries
        this.doScanCalculations(self, enemies);
        // Scan between furthest clockwise and anti-clockwise
        sweepScan(self);
    }
    
    /**
     * Rotates radar 360 degrees
     */
    private void fullScan() {
        battlefield.getSelf().turnRadarRight(370 * wobbleDirection);
        wobbleDirection *= -1;
    }
    
    /**
     * Sweeps radar between furthest clockwise and anti-clockwise enemies
     * Alternates direction each call
     */
    private void sweepScan(robocode.AdvancedRobot self) {
        double radarHeading = Math.toRadians(self.getRadarHeading());
        // Alternate direction
        wobbleDirection *= -1;
        // Target either left or right edge
        double targetBearing = wobbleDirection > 0 ? this.clock : this.anticlock;
        // Calculate turn needed and normalize to [-π, π]
        double turn = targetBearing - radarHeading;
        turn = Math.atan2(Math.sin(turn), Math.cos(turn));
        self.turnRadarRightRadians(turn);
    }
    /**
     * Gets the centre of mass of the battlefield, furthest clockwise etc.
     */
    public void doScanCalculations(robocode.AdvancedRobot self, Enemy[] enemies) {
        myPos.setLocation(self.getX(), self.getY());
        double sumX = 0;
        double sumY = 0;
        int count = 0;
        this.anticlock = Double.MAX_VALUE;
        this.clock = -Double.MAX_VALUE;
        
        for (Enemy enemy : enemies) {
            Point2D pos = enemy.getPosition();
            if (pos != null) {
                double x = pos.getX();
                double y = pos.getY();
                sumX += x;
                sumY += y;
                count++;
                
                double bearing = Math.atan2(x - myPos.x, y - myPos.y);
                
                if (bearing < this.anticlock) {
                    this.anticlock = bearing;
                    this.furthestAntiClockwise.setLocation(x, y);
                }
                if (bearing > this.clock) {
                    this.clock = bearing;
                    this.furthestClockwise.setLocation(x, y);
                }
            }
        }
        
        // Add margins
        this.clock += SCAN_MARGIN;
        this.anticlock -= SCAN_MARGIN;
        
        // calculate mass centre
        this.massCentre.setLocation(sumX / count, sumY / count);
        // get mass centre bearing
        this.centre = Math.atan2(this.massCentre.x - myPos.x, this.massCentre.y - myPos.y);
    }
    
    @Override
    public void doPaint(java.awt.Graphics2D g) {
        // Draw circle on mass centre
        if (massCentre != null) {
            g.setColor(java.awt.Color.YELLOW);
            g.fillOval((int)massCentre.x - 10, (int)massCentre.y - 10, 20, 20);
        }
        
        // Draw line to furthest clockwise
        if (furthestClockwise != null) {
            g.setColor(java.awt.Color.CYAN);
            g.drawLine((int)myPos.x, (int)myPos.y, 
                      (int)furthestClockwise.x, (int)furthestClockwise.y);
        }
        
        // Draw line to furthest anti-clockwise
        if (furthestAntiClockwise != null) {
            g.setColor(java.awt.Color.MAGENTA);
            g.drawLine((int)myPos.x, (int)myPos.y, 
                      (int)furthestAntiClockwise.x, (int)furthestAntiClockwise.y);
        }
    }
}
