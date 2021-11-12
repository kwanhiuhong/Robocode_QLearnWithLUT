package ece.cpen502.Robots;

import ece.cpen502.LUT.*;
import robocode.*;

public class MyRobot extends AdvancedRobot {

    private EnemyRobot enemy = new EnemyRobot();
    private LookupTable lookupTable = new LookupTable();
    private LearningAgent learningAgent = new LearningAgent(lookupTable);
    private int isHitByBullet = 0;
    private int hasHitWall = 0;

    public void run() {
        System.out.println("See state counts");

        while (true) {
            int state = this.getCurrentState();
//            int action = learningAgent
            this.resetState();

            ahead(100);
            turnGunRight(360);
            back(100);
            turnGunRight(360);
        }
    }
    /**
     * onScannedRobot: What to do when you see another robot
     */

    public void onScannedRobot (ScannedRobotEvent e){
        System.out.println("scanned once");
        double absoluteBearing = (getHeading() + e.getBearing()) % (360) * Math.PI/180;
        enemy.bearing = e.getBearingRadians();
        enemy.heading = e.getHeadingRadians();
        enemy.velocity = e.getVelocity();
        enemy.distance = e.getDistance();
        enemy.energy = e.getEnergy();
        enemy.xCoord = getX() + Math.sin(absoluteBearing) * e.getDistance();
        enemy.yCoord = getY() + Math.cos(absoluteBearing) * e.getDistance();
//        fire(3);
    }

    //TODO update the reward
    /**
     * onHitByBullet: What to do when you're hit by a bullet
     */
    public void onHitByBullet(HitByBulletEvent event) {
        double power = event.getBullet().getPower();
        double rewardOffset = -(4 * power + 2 * (power - 1));
        System.out.println("We got shot by a bullet! Reward is changed by: " + rewardOffset);
//        reward += rewardOffset;
        this.isHitByBullet = 1;
    }

    /**
     * onHitByBullet: What to do when you're hit by a wall
     */
    public void onHitWall(HitWallEvent event) {
        double rewardOffset = -10.0;
        System.out.println("We just hit the wall! reward is changed by " + rewardOffset);
//        reward += rewardOffset;
        this.hasHitWall = 1;
    }

    //helper functions
    private int getCurrentState() {
        int distance = RobotState.getDistance(enemy.distance);
        int enemyBearing = RobotState.getEnemyBearing(enemy.bearing);
        int heading = RobotState.getDirection(getHeading());
        int energy = RobotState.getEnergy(getEnergy());
        return RobotState.getState(distance, enemyBearing, heading, energy, hasHitWall, isHitByBullet);
    }

    private void resetState() {
        this.hasHitWall = 0;
        this.isHitByBullet = 0;
    }
}
