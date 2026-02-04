package net.richardsenior.robocode.skynet.base;
import java.awt.geom.Point2D;

/**
 * Generic interface for predicting enemy behavior for targeting purposes.
 * Can predict either bearing offsets (for bin-based targeting) or positions (for geometric targeting).
 */
public interface TargetingPredictor {
    /**
     * Predict the bearing offset (in radians) from direct aim where the enemy is likely to be.
     * @param ourPosition Our current position
     * @param enemyPosition Enemy's current position
     * @param bulletVelocity Speed of our bullet
     * @param directBearing The direct bearing from us to enemy
     * @return Predicted bearing offset in radians (add to directBearing for final aim)
     */
    double predictBearingOffset(Point2D.Double ourPosition, Point2D.Double enemyPosition, 
                                double bulletVelocity, double directBearing);
    
    /**
     * Record a shot for learning purposes.
     * @param wave The wave representing the bullet we fired
     */
    void recordShot(Wave wave);
    
    /**
     * Record the result of a shot (hit or miss) for learning purposes.
     * @param wave The wave that was fired
     * @param actualPosition Where the enemy actually was when the wave reached them
     */
    void recordResult(Wave wave, Point2D.Double actualPosition);
}
