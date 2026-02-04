package net.richardsenior.robocode.skynet.base.impl;

import net.richardsenior.robocode.skynet.base.Predictor;
import net.richardsenior.robocode.skynet.base.EventWrapper;
import net.richardsenior.robocode.skynet.base.Wave;
import java.util.Queue;
import java.awt.geom.Point2D;

/**
 * Simple linear predictor assuming constant velocity movement.
 * Fits a linear equation to scan history and extrapolates.
 */
public class SimplePredictor implements Predictor {
    private Point2D.Double position;
    private Point2D.Double velocity;
    
    public SimplePredictor(Queue<? extends EventWrapper> scanHistory) {
        if (scanHistory.size() < 2) {
            position = null;
            velocity = null;
            return;
        }
        
        EventWrapper[] scans = scanHistory.toArray(new EventWrapper[0]);
        int n = scans.length;
        
        // Use least squares to fit velocity
        double sumVx = 0, sumVy = 0;
        int count = 0;
        
        for (int i = 1; i < n; i++) {
            double dt = scans[i].getTime() - scans[i-1].getTime();
            if (dt > 0) {
                sumVx += (scans[i].getPosition().x - scans[i-1].getPosition().x) / dt;
                sumVy += (scans[i].getPosition().y - scans[i-1].getPosition().y) / dt;
                count++;
            }
        }
        
        if (count == 0) {
            position = null;
            velocity = null;
            return;
        }
        
        velocity = new Point2D.Double(sumVx / count, sumVy / count);
        
        // Use most recent position
        EventWrapper last = scans[n-1];
        position = new Point2D.Double(last.getPosition().x, last.getPosition().y);
    }
    
    @Override
    public Point2D.Double predict(int ticks) {
        if (position == null || velocity == null) return null;
        
        return new Point2D.Double(
            position.x + velocity.x * ticks,
            position.y + velocity.y * ticks
        );
    }
    
    @Override
    public Point2D.Double getIntercept(double bulletVelocity, Point2D.Double firingPosition) {
        if (position == null || velocity == null) return position;
        
        double dx = position.x - firingPosition.x;
        double dy = position.y - firingPosition.y;
        
        double a = velocity.x*velocity.x + velocity.y*velocity.y - bulletVelocity*bulletVelocity;
        double b = 2*(dx*velocity.x + dy*velocity.y);
        double c = dx*dx + dy*dy;
        
        double discriminant = b*b - 4*a*c;
        if (discriminant < 0 || Math.abs(a) < 1e-6) return position;
        
        double t = (-b - Math.sqrt(discriminant)) / (2*a);
        if (t < 0) t = (-b + Math.sqrt(discriminant)) / (2*a);
        if (t < 0) return position;
        
        return predict((int)Math.ceil(t));
    }
    
    // TargetingPredictor methods - not used by geometric predictors
    @Override
    public double predictBearingOffset(Point2D.Double ourPosition, Point2D.Double enemyPosition,
                                      double bulletVelocity, double directBearing) {
        return 0.0;
    }
    
    @Override
    public void recordShot(Wave wave) {
    }
    
    @Override
    public void recordResult(Wave wave, Point2D.Double actualPosition) {
    }
}
