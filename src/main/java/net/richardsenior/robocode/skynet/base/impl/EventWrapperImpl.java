package net.richardsenior.robocode.skynet.base.impl;

import java.awt.geom.Point2D;
import net.richardsenior.robocode.skynet.base.EventWrapper;
import net.richardsenior.robocode.skynet.base.Predictor;
import robocode.ScannedRobotEvent;

/**
 * Simple Implementation of EventWrapper that wraps ScannedRobotEvent data etc.
 */
public class EventWrapperImpl implements EventWrapper {
    private String name;
    private double energy, bearing, distance, velocity, heading;
    private long time;
    private Point2D.Double position;
    private Predictor predictor;


    /********************************************************/   
    /**************** Constructors **************************/ 
    /********************************************************/  
    /** For use by extending classes */
    protected EventWrapperImpl(String name) {
        this.name = name;
    }
    // parse a robocode event
    public EventWrapperImpl(ScannedRobotEvent event) {
        this(event.getName());
        this.energy = event.getEnergy();
        this.bearing = event.getBearing();
        this.distance = event.getDistance();
        this.velocity = event.getVelocity();
        this.heading = event.getHeading();
        this.time = event.getTime();
    }    

    public EventWrapperImpl(ScannedRobotEvent event, Point2D.Double position) {
        this(event);
        this.position = position;
    }

    public EventWrapperImpl(ScannedRobotEvent event, Point2D.Double position, Predictor predictor) {
        this(event, position);
        this.predictor = predictor;
    }
    /********************************************************/ 

    
    @Override
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    @Override
    public double getEnergy() {return energy;}
    public void setEnergy(double energy) {this.energy = energy;}
    @Override
    public double getBearing() {return bearing;}
    public void setBearing(double bearing) {this.bearing = bearing;}
    @Override
    public double getDistance() {return distance;}
    public void setDistance(double distance) {this.distance = distance;}
    @Override
    public double getVelocity() {return velocity;}
    public void setVelocity(double velocity) {this.velocity = velocity;}
    @Override
    public double getHeading() {return heading;}
    public void setHeading(double heading) {this.heading = heading;}
    @Override
    public long getTime() {return time;}
    public void setTime(long time) {this.time = time;}
    @Override
    public Point2D.Double getPosition() {return position;}
    public void setPosition(Point2D.Double position) {this.position = position;}
    @Override
    public Predictor getPredictor() {return predictor;}
    public void setPredictor(Predictor predictor) {this.predictor = predictor;}

}
