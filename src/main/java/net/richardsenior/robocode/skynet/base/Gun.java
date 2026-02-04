package net.richardsenior.robocode.skynet.base;

/** Determins and executes firing strategy including setting the current target etc. */
public interface Gun {
    public void update();
    public void fire();
}
