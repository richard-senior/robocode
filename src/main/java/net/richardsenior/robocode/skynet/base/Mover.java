package net.richardsenior.robocode.skynet.base;

/**
 * Encompasses movement strategy
 */
public interface Mover {
    /**
     * Called per tick, do any calculations
     */
    public void update();
    /**
     * Usually called per tick after update, but can be called arbitrarily
     */
    public void doMove();
    /**
     * Called when robot hits a wall - triggers emergency escape
     */
    public void onHitWall();
    /**
     * Debugging method to paint anything relevant to the mover strategy
     * @param g
     */
    public void doPaint(java.awt.Graphics2D g);
}
