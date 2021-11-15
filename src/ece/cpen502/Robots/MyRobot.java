package ece.cpen502.Robots;
import ece.cpen502.LUT.*;
import javafx.util.Pair;
import robocode.*;

import java.awt.*;
import java.util.Random;

public class MyRobot extends AdvancedRobot {
    private static LookupTable lut;
    //    public enum tankMode {scan, actions};
    //    private tankMode operationalMode = tankMode.scan;
    // --------- game rounds record
    private static int totalNumRounds = 0;
    private static int numRoundsTo100 = 0;
    private static int numWins = 0;
    private double epsilon = 0.7;
    // --------- state record
    private int currentAction;
    private int currentState;
    private final LearningAgent.Algo currentAlgo = LearningAgent.Algo.QLearn;

    private int hasHitWall = 0;
    private int isHitByBullet = 0;
    // ---------- program components
    private LearningAgent agent;
    private EnemyRobot enemyTank;
    private double robotEnergy;

    // -------- reward
    //the reward policy should be killed > bullet hit > hit robot > hit wall > bullet miss > got hit by bullet
    private double currentReward = 0.0;
    private final double goodReward = 1.0;
    private final double badReward = -0.25;
    // TODO
    private double bulletPower;
    private boolean foundEnemy;

    public void run() {

        // -------------------------------- Initialize robot tank parts ------------------------------------------------
        setBulletColor(Color.red);
        setGunColor(Color.green);
        setBodyColor(Color.yellow);
        setRadarColor(Color.blue);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true); // we need to adjust radar based on the distance and direction of the enemy tank
        enemyTank = new EnemyRobot();
        RobotState.initialEnergy = this.getEnergy();
        robotEnergy = RobotState.initialEnergy;
        // -------------------------------- Initialize reinforcement learning parts ------------------------------------
        lut = new LookupTable();
        agent = new LearningAgent(lut);
        // ------------------------------------------------ Run --------------------------------------------------------

        while (true) {
            System.out.println("See state counts");
            if(totalNumRounds > 4000) {epsilon = 0.0;}
            turnRadarLeft(90);
            setBulletPower();
            selectRobotAction();
            // update previous Q
            try{agent.train(currentState, currentAction, currentReward, currentAlgo);}catch(Exception e){
                System.out.println("here");
            }

        }
    }
    /**
     * we should decrease energy in case we are close to the enemy
     * and use maximum energy other time
     */
    private void setBulletPower(){
        int ratioDistance = (int)(1000/enemyTank.distance);
        if(ratioDistance>3){
            bulletPower = 3;
        }else{
            bulletPower = ratioDistance;
        }
    }

    /**
     * selectRobotAction: select robot action based on robot current state
     */
    private void selectRobotAction(){
        int state = getRobotState();
        currentAction = agent.getAction(state, epsilon);
        this.resetState(); // reset hitWall hitByBullet
        switch(currentAction){
            case RobotAction.moveForward:
                ahead(RobotAction.moveDistance);
                execute();
                break;
            case RobotAction.moveBack:
                back(RobotAction.moveDistance);
                execute();
                break;
            case RobotAction.headRight:
                ahead(RobotAction.moveDistance);
                turnRight(90.0);
                execute();
                break;
            case RobotAction.headLeft:
                ahead(RobotAction.moveDistance);
                turnLeft(90.0);
                execute();
                break;
            case RobotAction.backRight:
                back(RobotAction.moveDistance);
                turnRight(90.0);
                execute();
                break;
            case RobotAction.backLeft:
                back(RobotAction.moveDistance);
                turnLeft(90.0);
                execute();
                break;
            case RobotAction.tryFire:
                targetEnemyAndFire();
                execute();
                break;
        }
    }

    public void targetEnemyAndFire(){
        foundEnemy = false;
        while(!foundEnemy){
            turnRadarLeft(90);
            execute();
        }
        turnGunLeft(getGunHeading() - getHeading() - enemyTank.bearing);
        setBulletPower();
        fire(bulletPower);
    }

    /*
    get and set current state
     */
    private int getRobotState(){
        int curDistance = RobotState.calcDistanceState(enemyTank.distance);
        int enemyBearing = RobotState.getEnemyBearing(enemyTank.bearing);
        int curEnergy = RobotState.calcEnergyState(getEnergy());
        int heading = RobotState.getDirection(getHeading());
        try{currentState = RobotState.getState(curDistance, enemyBearing, heading, curEnergy, hasHitWall, isHitByBullet);}catch(Exception e){
            System.out.println("here");
        }
        return currentState;
    }


    @Override
    public void onHitWall(HitWallEvent event) {
        super.onHitWall(event);
        robotEnergy = getEnergy();
        hasHitWall = 1;
        // update reward
        currentReward = badReward;
        turnLeft(180);
        ahead(100);
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        super.onBulletHit(event);
        robotEnergy = event.getEnergy();
        currentReward = goodReward;
    }

    /**
     * onHitByBullet: What to do when you're hit by a bullet
     */
    public void onHitByBullet (HitByBulletEvent e){
        // update robot energy
        robotEnergy = getEnergy();
        isHitByBullet = 1;
        turnLeft(90 - e.getBearing());
        currentReward = badReward;
    }

    /**
     * onScannedRobot: What to do when you see another robot
     */

    public void onScannedRobot (ScannedRobotEvent e){
        System.out.println("scanned once");
        foundEnemy = true;
        double absoluteBearing = (getHeading() + e.getBearing()) % (360) * Math.PI/180;
        enemyTank.bearing = e.getBearingRadians();
        enemyTank.heading = e.getHeadingRadians();
        enemyTank.velocity = e.getVelocity();
        enemyTank.distance = e.getDistance();
        enemyTank.energy = e.getEnergy();
        enemyTank.xCoord = getX() + Math.sin(absoluteBearing) * e.getDistance();
        enemyTank.yCoord = getY() + Math.cos(absoluteBearing) * e.getDistance();
    }

    /*
    reset hitWall and hitByBullet states
     */
    private void resetState() {
        this.hasHitWall = 0;
        this.isHitByBullet = 0;
    }

    @Override
    public void onWin(WinEvent event) {
        super.onWin(event);
        currentReward = goodReward*2;
        // TODO: record game
        totalNumRounds++;
        numRoundsTo100++;
        numWins++;
        //
        try{agent.train(currentAction, currentState, currentReward, currentAlgo);}catch(Exception e){
            System.out.println("here");
        }
    }

    @Override
    // look up the previous srare and do back step q.train
    public void onDeath(DeathEvent event) {
        super.onDeath(event);
        currentReward = badReward*2;
        // TODO: record game
        totalNumRounds++;
        numRoundsTo100++;
        //
        try{agent.train(currentAction, currentState, currentReward, currentAlgo);}catch(Exception e){
            System.out.println("here");
        }
    }
}
