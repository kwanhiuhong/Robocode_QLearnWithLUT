package ece.cpen502.Robots;
import ece.cpen502.LUT.EnemyRobot;
import ece.cpen502.LUT.Log;
import ece.cpen502.LUT.RobotAction;
import ece.cpen502.LUT.RobotState;
import ece.cpen502.LearningAgents.LearningAgentNN;
import ece.cpen502.NN.RLNeuralNet;
import ece.cpen502.ReplayMemory.*;
import robocode.*;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class NNRobot extends AdvancedRobot {
    private final String fileToSaveName = "robotMiddleReward";
    private final String fileTerminalReward = "robotTerminalReward";
    // --------- game rounds record
    private static int totalNumRounds = 1;
    private static double numRoundsTo100 = 1;
    private static double numWins = 0;
    private static int countOf100Round = 0;
    private static double epsilon = 0.5;

    // --------- state record
    private int actionTaken;
    private double[] state;
    private double qConvergence = 0;
    private double actionsCount = 0;

    private final LearningAgentNN.Algo currentAlgo = LearningAgentNN.Algo.QLearn;

    private int hasHitWall = 0;
    private int isHitByBullet = 0;
    // ---------- program components
    private static LearningAgentNN agent = new LearningAgentNN();
    private EnemyRobot enemyTank;

    // -------- reward
    //the reward policy should be killed > bullet hit > hit robot > hit wall > bullet miss > got hit by bullet
    private double currentReward = 0.0;
    private final double goodInstantReward = 5.0;
    private final double badInstantReward = -2.0;
    private final double winReward = 10;
    private final double loseReward = -10;

    private double fireMagnitude;
    private boolean loadPrevTrainedWeights = true;

    private final boolean memoryReplayModeOn = false;
    private double[] stateT;
    private int actionT;
    private final int memorySize = 10;
    private final int miniSetSize = 5;
    private int count = this.memorySize + 1;
    private ReplayMemory<Object> memory = new ReplayMemory<Object>(memorySize);

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
        // -------------------------------- Initialize reinforcement learning parts ------------------------------------
//        agent = new LearningAgentNN();
        if(!LearningAgentNN.nn.areWeightsLoaded && loadPrevTrainedWeights){
            double[][] weights_1 = fileLoader("inputToHiddenWeights", RLNeuralNet.numInputs + 1, LearningAgentNN.noOfHiddenNeurons+1);
            LearningAgentNN.nn.setInputToHiddenWeights(weights_1);
            double[][] weights_2 = fileLoader("hiddenToOutputWeights", LearningAgentNN.noOfHiddenNeurons + 1, RLNeuralNet.numOutputs);
            LearningAgentNN.nn.setHiddenToOutputWeights(weights_2);
            LearningAgentNN.nn.areWeightsLoaded = true;
        }
        // ------------------------------------------------ Run --------------------------------------------------------
        while (true) {
            if(memoryReplayModeOn){
                // save n times?
                // while(){
                // agent.train(state, actionTaken, currentReward, currentAlgo);
                // }
                if(memory.sizeOf() < miniSetSize){
                    addInMemory();
                    count --;
                }else{
                    Object[] sample = memory.sample(miniSetSize);
                    int a = 2;
                    for (Object o: sample) {
                        Experience curE = (Experience) o;
                        agent.train(curE.getState_t(), curE.getAction_t(), curE.getReward_t(), currentAlgo);
                    }
                    addInMemory();
                    count = 5;
                }
            }else{
                double[] stateBeforeAction = getRobotState();
                selectRobotAction(stateBeforeAction);
                actionsCount++;
                qConvergence += agent.train(state, actionTaken, currentReward, currentAlgo);
            }
            this.currentReward = 0;
            adjustAndFire();
        }
    }
// memory replay
    private void addInMemory(){
        if(stateT == null){
            stateT = getRobotState();
            actionT = selectRobotAction(stateT);
        }else{
            double[] state_t1 = getRobotState();
            Experience et = new Experience(stateT, actionT, currentReward, state_t1);
            stateT = getRobotState();
            actionT = selectRobotAction(stateT);
            memory.add(et);
        }
    }
    /**
     * selectRobotAction: select robot action based on robot current state
     */
    private int selectRobotAction(double[] state){
        actionTaken = agent.getAction(state, epsilon);
        this.resetState(); // reset hitWall hitByBullet
        switch(actionTaken){
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
        }
        return actionTaken;
    }

    private void adjustAndFire() {
        fireMagnitude = 800 / enemyTank.distance;
        fireMagnitude = fireMagnitude > 3 ? 3 : fireMagnitude;
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        adjustGunAngle();
        if (getGunHeat() == 0) setFire(fireMagnitude);
        execute();
    }

    private void adjustGunAngle() {
        long bulletArrivalTime, bulletTransmissionTime;
        double coordinate[] = {enemyTank.xCoord, enemyTank.yCoord};
        for (int i = 0; i < 19; i++) {
            double distance = euclideanDistance(getX(), getY(), coordinate[0], coordinate[1]);
            bulletTransmissionTime = (int) Math.round((distance / (20 - (fireMagnitude * 3))));
            bulletArrivalTime = bulletTransmissionTime + getTime() - 9;
            coordinate = calculatePosition(bulletArrivalTime);
        }
        double gunOffset = getGunHeadingRadians() - (Math.PI / 2 - Math.atan2(coordinate[1] - getY(), coordinate[0] - getX()));
        setTurnGunLeftRadians(normalize(gunOffset));
    }

    private double normalize(double angle) {
        if (angle > Math.PI) angle -= 2*Math.PI;
        if (angle < -Math.PI) angle += 2*Math.PI;
        return angle;
    }

    private double[] calculatePosition(long bulletArrivalTime) {
        double difference = bulletArrivalTime - enemyTank.time;
        double coordinate[] = new double[2];
        coordinate[0] = enemyTank.xCoord + difference * enemyTank.velocity * Math.sin(enemyTank.heading);
        coordinate[1] = enemyTank.yCoord + difference * enemyTank.velocity * Math.cos(enemyTank.heading);
        return coordinate;
    }

    private double euclideanDistance(double x1, double y1, double x2, double y2) {
        double xDiff = x2 - x1, yDiff = y2 - y1;
        return Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
    }

    private double[] getRobotState(){
        double curDistance = enemyTank.distance;
        double enemyBearing = enemyTank.bearing;
        double curEnergy = getEnergy();
        double heading = getHeading();
        state = new double[6];
        state[0] = curDistance;
        state[1] = enemyBearing;
        state[2] = heading;
        state[3] = curEnergy;
        state[4] = hasHitWall;
        state[5] = isHitByBullet;
        //state = {curDistance, enemyBearing, heading, curEnergy, hasHitWall, isHitByBullet};
        return state;
    }

    private void resetState() {
        this.hasHitWall = 0;
        this.isHitByBullet = 0;
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        currentReward += goodInstantReward;
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        currentReward += badInstantReward;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e){
        isHitByBullet = 1;
        currentReward -= e.getBullet().getPower();

        // run away from wall and run towards the enemy
        double degToEnemy= getBearingToTarget(enemyTank.xCoord, enemyTank.yCoord, getX(), getY(), getHeadingRadians());
        setTurnRightRadians(degToEnemy+2);
        setAhead(100);
        execute();
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        currentReward += badInstantReward;
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        hasHitWall = 1;
        currentReward += badInstantReward;
        if(euclideanDistance(getX(), getY(),enemyTank.xCoord, enemyTank.yCoord)>200){
            double degToEnemy= getBearingToTarget(enemyTank.xCoord, enemyTank.yCoord, getX(), getY(), getHeadingRadians());
            setTurnRightRadians(degToEnemy);
            setAhead(200);
            execute();
        }
    }

    public double getBearingToTarget(double x, double y, double myX, double myY, double myHeading){
        double deg = Math.PI/2 - Math.atan2(y - myY, x-myX);
        return  normalize(deg - myHeading);
    }

    @Override
    public void onScannedRobot (ScannedRobotEvent e){
        double absoluteBearing = (getHeading() + e.getBearing()) % (360) * Math.PI/180;
        enemyTank.bearing = e.getBearingRadians();
        enemyTank.heading = e.getHeadingRadians();
        enemyTank.velocity = e.getVelocity();
        enemyTank.distance = e.getDistance();
        enemyTank.energy = e.getEnergy();
        enemyTank.xCoord = getX() + Math.sin(absoluteBearing) * e.getDistance();
        enemyTank.yCoord = getY() + Math.cos(absoluteBearing) * e.getDistance();
        enemyTank.time = getTime();
    }

    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        if (numRoundsTo100 < 100) {
            numRoundsTo100++;
        } else {
            countOf100Round ++;
            log100Round();
            logQConvergence();;
            System.out.println("\n\n !!!!!!!!! " +"win percentage"+ " " + ((numWins / numRoundsTo100) * 100) + "\n\n");
            numRoundsTo100 = 0;
            numWins = 0;

            actionsCount = 0;
            qConvergence = 0;
        }
        totalNumRounds++;
        if (totalNumRounds % 1000 == 0) epsilon = epsilon > 0.05 ? epsilon - 0.05 : 0;
        System.out.println("total: " + totalNumRounds + ", epsilon:" + epsilon);
    }


    @Override
    public void onWin(WinEvent event) {
        currentReward += winReward;
        numWins++;
        agent.train(state, actionTaken, currentReward, currentAlgo);
    }

    @Override
    public void onDeath(DeathEvent event) {
        currentReward += loseReward;
        agent.train(state, actionTaken, currentReward, currentAlgo);
    }

    private void logQConvergence(){
        double avgQConvergence = qConvergence / actionsCount * 100;
        File qConvergence = getDataFile("qConvergence");
        Log logFile = new Log();
        logFile.writeToFile(qConvergence, avgQConvergence, countOf100Round);

    }

    private void log100Round(){
        double winRate = (numWins/numRoundsTo100) * 100;
        File folderDst2 = getDataFile(fileToSaveName);
        Log logFile = new Log();
        logFile.writeToFile(folderDst2, winRate, countOf100Round);
    }

    @Override
    public File getDataFile(String filename) {
        return super.getDataFile(filename);
    }

    public double[][] fileLoader(String fileName, int i1, int i2){
        double[][] out = new double[i1][i2];
        try {
            File myObj = getDataFile(fileName);
            Scanner myReader = new Scanner(myObj);

            int i1_temp = 0;
            int i2_temp = 0;
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if(i2_temp < i2){
                    out[i1_temp][i2_temp] = Double.parseDouble(data);
                    i2_temp++;
                }else{
                    i1_temp++;
                    out[i1_temp][0] = Double.parseDouble(data);
                    i2_temp = 1;
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return out;
    }
}
