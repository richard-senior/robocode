package net.richardsenior.robocode.skynet.base;
import java.awt.geom.Point2D;

/**
 * Interface for use by anything which predicts a location and 'n' ticks in the future.
 */
public interface Predictor {
    /** 
    * predict the x,y coordinates of the object this predictor is for, in n ticks in the future 
    * For example if this predictor is underpinned by some sort of polynomial fit to past locations,
    * it would evaluate the polynomial at 'n' ticks in the future and return that location
    * @param ticks number of ticks in the future to predict
    */
    public Point2D.Double predict(int ticks);
    /**
     * Assuming travel in a straight line at given velocity from origin point,
     * get the intercept point with this predictor's predicted location.
     * For example how where will the object this predictor is predicting for be if
     * I travel towards it at 'velocity' pixels per tick from 'origin' point.
     * Used for (very) loosely estimating targeting solutions.
     * @param velocity
     * @param origin
     * @return
     */
    public Point2D.Double getIntercept(double velocity, Point2D.Double origin);
}
