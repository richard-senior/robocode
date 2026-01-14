package net.richardsenior.robocode.skynet.base;

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
}
