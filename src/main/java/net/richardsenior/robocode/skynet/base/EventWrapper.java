package net.richardsenior.robocode.skynet.base;
import java.awt.geom.Point2D;

// interface which wraps ScannedRobotEvent for use keeping history and extending the scan event etc.
public interface EventWrapper {
    /** name of this object at the time of the event, if it has a name */
    public String getName();
    /* energy of this object at the time of the event */
    public double getEnergy();
    /* bearing from us to this object at the time of the event (radians) */
    public double getBearing();
    /* how far away from us was this object at the time of the event */
    public double getDistance();
    /** what is the velocity of this object (pixels per tick) */
    public double getVelocity();
    /** what is the heading of this object (radians) */
    public double getHeading();
    /** the time in ticks that this event occurred */
    public long getTime();
    /** The current position of the object that this event describes */
    public Point2D.Double getPosition();
    /** An object which can be used to predict where this object will be in the future */
    public Predictor getPredictor();
}
