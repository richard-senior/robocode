package net.richardsenior.robocode.skynet.robot;

import robocode.*;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.impl.BattlefieldImpl;
import robocode.ScannedRobotEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;

/**
 * Simple delegate wrapper around the Battlefield object allowing simple abstraction
 */
public class SkynetRobot extends AdvancedRobot {
    // in future this may be a collection of different battlefield implementions (melee, 1v1 etc.)
    private Battlefield battlefield;

    @Override
    public void run() {
        System.out.println("=== SkynetRobot starting ===");
        
        try {
            // Test battlefield creation only
            System.out.println("Creating battlefield...");
            this.battlefield = new BattlefieldImpl(this);
            System.out.println("Battlefield created successfully");
            
            setAdjustGunForRobotTurn(true);
            setAdjustRadarForGunTurn(true);
            
            System.out.println("Starting main loop...");
            int count = 0;
            while (true) {
                // Test battlefield update but limit logging
                if (count % 1000 == 0) {
                    System.out.println("Update count: " + count + ", obstacles: " + battlefield.size());
                }
                this.battlefield.update();
                count++;
                
                // Basic robot movement
                turnRadarRight(10);
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {this.battlefield.update(e);}
    @Override
    public void onRobotDeath(RobotDeathEvent e) {this.battlefield.update(e);}
    @Override
    public void onBulletHit(BulletHitEvent e) {this.battlefield.update(e);}
    @Override
    public void onBulletMissed(BulletMissedEvent e) {this.battlefield.update(e);}
    @Override
    public void onHitByBullet(HitByBulletEvent e) {this.battlefield.update(e);}
    @Override
    public void onHitRobot(HitRobotEvent e) {this.battlefield.update(e);}
    @Override
    public void onHitWall(HitWallEvent e) {this.battlefield.update(e);}
    @Override
    public void onCustomEvent(CustomEvent e) {this.battlefield.update(e);}
    @Override
    public void onRoundEnded(RoundEndedEvent e) {this.battlefield.update(e);}
    @Override
    public void onBattleEnded(BattleEndedEvent e) {this.battlefield.update(e);}
    @Override
    public void onDeath(DeathEvent e) {this.battlefield.update(e);}
    @Override
    public void onSkippedTurn(SkippedTurnEvent e) {this.battlefield.update(e);}
    @Override
    public void onWin(WinEvent e) {this.battlefield.update(e);}
    @Override
    public void onPaint(java.awt.Graphics2D g) {battlefield.onPaint(g);}
}
