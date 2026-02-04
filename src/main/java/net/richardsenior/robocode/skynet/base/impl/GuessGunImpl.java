package net.richardsenior.robocode.skynet.base.impl;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import robocode.util.Utils;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Enemy;
import net.richardsenior.robocode.skynet.base.Gun;

/**
 * Statistical targeting gun using bin-based GuessFactor prediction.
 * Learns enemy movement patterns and adapts aim accordingly.
 * Each gun instance is dedicated to targeting one specific enemy.
 */
public class GuessGunImpl implements Gun {
    private static final double FIRE_POWER = 1.5;
    private Battlefield battlefield;
    private Enemy targetEnemy;
    private BinPredictor binPredictor;
    private Point2D.Double aimPoint;

    public GuessGunImpl(Battlefield battlefield, Enemy targetEnemy) {
        this.battlefield = battlefield;
        this.targetEnemy = targetEnemy;
        this.binPredictor = new BinPredictor();
        
        // Make gun and radar independent of robot body
        battlefield.getSelf().setAdjustGunForRobotTurn(true);
        battlefield.getSelf().setAdjustRadarForGunTurn(true);
    }

    @Override
    public void update() {
        if (targetEnemy.getScanHistory().isEmpty()) {
            aimPoint = null;
            return;
        }
        
        double bulletVelocity = 20.0 - (3.0 * FIRE_POWER);
        
        Point2D.Double myPos = new Point2D.Double(
            battlefield.getSelf().getX(),
            battlefield.getSelf().getY()
        );
        
        // Get predicted intercept point instead of current position
        Point2D.Double interceptPoint = targetEnemy.getPredictor().getIntercept(bulletVelocity, myPos);
        if (interceptPoint == null) {
            interceptPoint = targetEnemy.getPosition();
        }
        
        // Calculate bearing to intercept point
        double directBearing = Math.atan2(
            interceptPoint.x - myPos.x,
            interceptPoint.y - myPos.y
        );
        
        // Get bearing offset from bin predictor
        double bearingOffset = binPredictor.predictBearingOffset(
            myPos, interceptPoint, bulletVelocity, directBearing);
        
        double aimBearing = directBearing + bearingOffset;
        
        // Calculate aim point (for visualization)
        double distance = myPos.distance(interceptPoint);
        aimPoint = new Point2D.Double(
            myPos.x + Math.sin(aimBearing) * distance,
            myPos.y + Math.cos(aimBearing) * distance
        );
        
        // Turn gun to aim bearing
        double gunHeading = battlefield.getSelf().getGunHeadingRadians();
        double turn = Utils.normalRelativeAngle(aimBearing - gunHeading);
        
        battlefield.getSelf().setTurnGunRightRadians(turn);
    }
    
    public BinPredictor getBinPredictor() {
        return binPredictor;
    }

    @Override
    public void fire() {
        if (aimPoint == null) return;
        if (battlefield.getSelf().getGunHeat() > 0) return;
        
        double turnRemaining = Math.abs(battlefield.getSelf().getGunTurnRemainingRadians());
        
        if (turnRemaining < 0.05) {
            battlefield.getSelf().setFire(FIRE_POWER);
        }
    }

    public void doPaint(Graphics2D g) {
    }
}
