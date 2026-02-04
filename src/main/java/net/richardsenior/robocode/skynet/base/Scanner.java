package net.richardsenior.robocode.skynet.base;

import java.awt.geom.Point2D;

public interface Scanner {
    /**
     * Will probably have been injected into the implementation at construction
     * @return
     */
    public Battlefield getBattlefield();

    /* 
    * control the scanner (radar) using some form of logic based upon
    * where the enemies are
    * That is, if the enemies are all around us or we haven't scanned the whole battlefield for a while then
    * do a full 360 scan.
    * But if we know the enemies exist within a 'sector' of the battlefield then simply scan from one edge of the sector
    * to the other repeatedly etc.
    */
    public void scan();
    /**
     * returns the coordinate of a point on the battlefield which represents the 'centre of mass'
     * For example if all opponents are on the left hand side of the battlefield then this point will
     * be somewhere between them on the left hand side of the battlefield
     * Can be used for movement strategies orienting ourselves for max velocity etc.
     * @return
     */
    public Point2D.Double getMassCentre();
    /**
     * For the purpose of scanning which enemy is further clockwise of us.
     * That is imagine we are at the centre of a clock face and there are two robots at
     * 2pm and 4pm then the furthest clockwise is the one at 4pm
     * A robot at 10pm is ignored, this is anti-clockwise
     * @return
     */
    public Point2D.Double getFurthestClockwise();
    /**
     * Same as {@link #getFurthestClockwise()} but in the other direction
     * @return
     */
    public Point2D.Double getFurthestAntiClockwise();
    
    /**
     * Draw debug visualization
     */
    public void doPaint(java.awt.Graphics2D g);

}
