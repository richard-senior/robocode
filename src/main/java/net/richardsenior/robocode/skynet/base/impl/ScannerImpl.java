package net.richardsenior.robocode.skynet.base.impl;
import net.richardsenior.robocode.skynet.base.Battlefield;
import net.richardsenior.robocode.skynet.base.Scanner;

public class ScannerImpl implements Scanner {
    private Battlefield battlefield;

    public ScannerImpl(Battlefield battlefield) {
        this.battlefield = battlefield;
        battlefield.getSelf().setAdjustGunForRobotTurn(true);
        battlefield.getSelf().setAdjustRadarForGunTurn(true);
    }

    @Override
    public Battlefield getBattlefield() {return this.battlefield;}

    @Override
    public void scan() {
        long time = battlefield.getSelf().getTime();
        long stale = battlefield.getOldestSighting();
        long stalest = time - stale;
        if (time < 10 || stalest > 10) {
            System.out.println("Stale Scan or new round " + stalest);
            battlefield.getSelf().turnRadarRight(45);
        } else {
            System.out.println("wobbling");
            battlefield.getSelf().turnRadarRight(45);
        }
    }
}
