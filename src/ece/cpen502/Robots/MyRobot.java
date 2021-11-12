package ece.cpen502.Robots;

import ece.cpen502.LUT.*;
import javafx.util.Pair;
import robocode.*;

import java.util.Random;

public class MyRobot extends AdvancedRobot {
    private static LookupTable lut;
    private LearningAgent agent;
    private double enemyDistance;
    private double enemyBearing;
    private int hitWall = 0;
    private int hitByBullet = 0;
    private EnemyRobot enemyTank;
    private double robotEnergy;
    private double reward = 0.0;
    private double rewardChangeDefault = 1.0;
    // TODO
    private double bulletPower;
//    private static int numOfState = RobotState.stateCount

    public void run() {

        // -------------------------------- Initialize robot tank parts ------------------------------------------------
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true); // we need to adjust radar based on the distance and direction of the enemy tank
        enemyTank = new EnemyRobot();
        RobotState.initialEnergy = this.getEnergy();
        robotEnergy = RobotState.initialEnergy;
        // -------------------------------- Initialize reinforcement learning parts ------------------------------------
        lut = new LookupTable(RobotState.stateCount, RobotAction.actionsCount);
        // Load data into the table
        try{
            lut.load("lutRobotData.dat");
        }catch(Exception e){
            System.out.println(e);
        }
        agent = new LearningAgent();
        // ------------------------------------------------ Run --------------------------------------------------------
        while (true) {
            // Replace the next 4 lines with any behavior you would like
            ahead(100);
            turnGunRight(360);
            setBulletPower();
            selectRobotAction();
            // TODO check place of learning
            // we need to do some back steps here?? -> q learning kind of things q is a state action table
            // agent.learn(new Pair<>(state, action), reward);
            execute();
        }
    }
    /**
     * we should decrease energy in case we are close to the enemy
     * and use maximum energy other time
     */
    private void setBulletPower(){
        // TODO
        bulletPower = 3.0;
    }

    /**
     * selectRobotAction: select robot action based on robot current state
     */
    private void selectRobotAction(){
        int state = getRobotState();
        int action = agent.selectAction(state);
        resetHitWall();
        resetHitByBullet();
        // learning here
        // TODO: check the place of learning
//        agent.learn(new Pair<>(state, action), reward);
        switch(action){
            case RobotAction.moveForward:
                ahead(RobotAction.moveDistance);
                break;
            case RobotAction.moveBack:
                back(RobotAction.moveDistance);
                break;
            case RobotAction.headRight:
                ahead(RobotAction.moveDistance);
                turnRight(90.0);
                break;
            case RobotAction.headLeft:
                ahead(RobotAction.moveDistance);
                turnLeft(90.0);
                break;
            case RobotAction.backRight:
                back(RobotAction.moveDistance);
                turnRight(90.0);
                break;
            case RobotAction.backLeft:
                back(RobotAction.moveDistance);
                turnLeft(90.0);
                break;
        }

    }

    private int getRobotState(){
        // state is based on int: numDistance, numBearing, numEnergy, numHitWall, numHitByBullet
        int curDistance = RobotState.calcDistanceState(enemyDistance);
        int curBearing = RobotState.calcBearingState(enemyBearing);
        int curEnergy = RobotState.calcEnergyState(robotEnergy);
        int curHitWal = hitWall;
        int curHitByBullet = hitByBullet;

        // hitWall = 0;
        return RobotState.stateMap[curDistance][curBearing][curEnergy][curHitWal][curHitByBullet];
    }


    @Override
    public void onHitWall(HitWallEvent event) {
        super.onHitWall(event);
        robotEnergy = getEnergy();
        hitWall = 1;
        // update reward
        reward -= rewardChangeDefault;
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        super.onBulletHit(event);
        robotEnergy = event.getEnergy();

        // TODO: update reward -> check!

    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        super.onBulletMissed(event);
        // TODO: update reward -> check!
    }
    public double calculateReward(){
        // TODO
        // priorQ = q.outputFor(previousStateAction);
        // currentQmax = q.outputFor(currentMaxStateAction);
        // updateQ = priorQ + alpha*(r+gamma*currentQmax - priorQ)
        return 0.0;
    }

    // for exploration move
    public int selectRandomAction(){
        Random rand = new Random();
        return rand.nextInt(6);
    }
    /**
     * onHitByBullet: What to do when you're hit by a bullet
     */
    public void onHitByBullet (HitByBulletEvent e){
        // update robot energy
        robotEnergy = getEnergy();
        hitByBullet = 1;
        turnLeft(90 - e.getBearing());
        // TODO: update reward
    }

    /**
     * onScannedRobot: What to do when you see another robot
     */

    public void onScannedRobot (ScannedRobotEvent e){
        enemyDistance = e.getDistance();
        enemyBearing = e.getBearing();
        robotEnergy = e.getEnergy();
        fire(1);
    }

    private void resetHitWall(){
        hitWall = 0;
    }

    private void resetHitByBullet(){
        hitByBullet = 0;
    }
}
