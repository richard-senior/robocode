package net.richardsenior.robocode.skynet.base.impl;

import java.util.LinkedList;

import net.richardsenior.robocode.skynet.base.Battlefield;

/**
 * An extended Enemy for use in treating ourselves (this robot) in the same way as other enemies on 
 * the battlefield for the purposes of calculating how other enemies see us in terms of movement etc.
 */
public class SelfEnemyImpl extends EnemyImpl {
    public SelfEnemyImpl(Battlefield battlefield) {  
        super(battlefield.getSelf().getName(), battlefield);
    }   
    @Override
    public void update() {
        EventWrapperImpl e = new EventWrapperImpl(this.getBattlefield().getSelf().getName());
        Battlefield bf = this.getBattlefield();
        e.setPosition(new java.awt.geom.Point2D.Double(bf.getSelf().getX(), bf.getSelf().getY()));
        e.setHeading(bf.getSelf().getHeadingRadians());
        e.setVelocity(bf.getSelf().getVelocity());
        e.setEnergy(bf.getSelf().getEnergy());
        e.setBearing(0);
        e.setDistance(0);        
        e.setTime(bf.getSelf().getTime());
        updateSelf(e);
    }

    /**
     * We don't want to update the predictor or create self waves
     */
    @Override
    protected void updatePredictorAndFiringEvent() {}
}
