package ece.cpen502.Robots;
import ece.cpen502.LUT.*;
import javafx.util.Pair;
import robocode.*;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
    private int centerX;
    private int centerY;

    //
    Writer log;
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
        centerX = (int) getBattleFieldWidth()/2;
        centerY = (int) getBattleFieldHeight()/2;
        // ------------------------------------------------ Run --------------------------------------------------------

        while (true) {
            if(totalNumRounds > 4000) {epsilon = 0.0;}
            setBulletPower();
            selectRobotAction();
            // update previous Q
            agent.train(currentState, currentAction, currentReward, currentAlgo);
            execute();
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
                setAhead(RobotAction.moveDistance);
                execute();
                break;
            case RobotAction.moveBack:
                setBack(RobotAction.moveDistance);
                execute();
                break;
            case RobotAction.headRight:
                setAhead(RobotAction.moveDistance);
                setTurnRight(90.0);
                execute();
                break;
            case RobotAction.headLeft:
                setAhead(RobotAction.moveDistance);
                setTurnLeft(90.0);
                execute();
                break;
            case RobotAction.backRight:
                setBack(RobotAction.moveDistance);
                setTurnRight(90.0);
                execute();
                break;
            case RobotAction.backLeft:
                setBack(RobotAction.moveDistance);
                setTurnLeft(90.0);
                execute();
                break;
            case RobotAction.tryFire:
                targetEnemyAndFire();
                execute();
                break;
            case RobotAction.goToCenter:
                goToCenter(centerX, centerY, getX(), getY(), getHeadingRadians());
                execute();
                break;
        }
    }

    public void targetEnemyAndFire(){
        foundEnemy = false;
        while(!foundEnemy){
            turnRadarLeft(90);
            turnLeft(getRadarHeading());
            execute();
        }
        turnGunRight(getHeading() - getGunHeading()  + enemyTank.bearing);
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
        currentState = RobotState.getState(curDistance, enemyBearing, heading, curEnergy, hasHitWall, isHitByBullet);
        return currentState;
    }


    @Override
    public void onHitWall(HitWallEvent event) {
        super.onHitWall(event);
        robotEnergy = getEnergy();
        hasHitWall = 1;
        // update reward
        currentReward = badReward;
        goToCenter(centerX, centerY, getX(), getY(), getHeadingRadians());
    }

    private void goToCenter(double x, double y, double myX, double myY, double myHeading){
        double degToCenter = getBearingToCenter(centerX, centerY, getX(), getY(), getHeadingRadians());
        setTurnRightRadians(degToCenter);
        setAhead(100);
        execute();
    }

    public double getBearingToCenter(double x, double y, double myX, double myY, double myHeading){
        double deg = Math.PI/2 - Math.atan2(y - myY, x-myX);
        return normAngle(deg - myHeading);
    }

    public double normAngle(double ang){
        if(ang <= -Math.PI){
            ang += 2*Math.PI;
        }
        if(ang > Math.PI){
            ang -= 2*Math.PI;
        }
        return ang;
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        super.onBulletHit(event);
        fire(bulletPower);
        robotEnergy = event.getEnergy();
        currentReward = goodReward;
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        super.onBulletMissed(event);
        turnGunRight(180 - getGunHeading());
    }

    /**
     * onHitByBullet: What to do when you're hit by a bullet
     */
    public void onHitByBullet (HitByBulletEvent e){
        // update robot energy
        robotEnergy = getEnergy();
        isHitByBullet = 1;
        turnRadarRight(e.getBearing());
        turnGunRight(e.getBearing());
        fire(bulletPower);
        execute();
        currentReward = badReward;
    }

    /**
     * onScannedRobot: What to do when you see another robot
     */

    public void onScannedRobot (ScannedRobotEvent e){
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
        if(numRoundsTo100<100){
            System.out.println("win");
            totalNumRounds++;
            numRoundsTo100++;
            numWins++;
        }else{
            LookupTable hey = agent.lookupTable;
            System.out.println(" !!!!!!!!! " +"win percentage"+ " " + numWins/numRoundsTo100);
            numRoundsTo100 = 0;
            numWins = 0;
        }

        //
        agent.train(currentState, currentAction, currentReward, currentAlgo);
    }

    @Override
    // look up the previous srare and do back step q.train
    public void onDeath(DeathEvent event) {
        super.onDeath(event);
        currentReward = badReward*2;
        // TODO: record game
        if(numRoundsTo100 < 100){
            System.out.println("lose");
            totalNumRounds++;
            numRoundsTo100++;
        }else{
            LookupTable hey = agent.lookupTable;
            System.out.println(" !!!!!!!!! " +"win percentage"+ " " + numWins/numRoundsTo100);
            numRoundsTo100 = 0;
            numWins = 0;
        }

        //
        agent.train(currentState, currentAction, currentReward, currentAlgo);
    }
}
