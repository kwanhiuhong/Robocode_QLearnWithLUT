package ece.cpen502.LUT;

public class RobotState {
    public static final int numEnemyDistance = 10; //max distance is sqrt(600^2 + 800^2) = 1000
    public static final int numEnemyBearing = 4; //head, back, right, left
    public static final int numDirection = 4; //head, back, right, left
    public static final int numEnergy = 4; //low, less than half, more than half, high
    public static final int numHitWall = 2; //we hit wall or we not
    public static final int numHitByBullet = 2; //we got shot or we not
    public static int statesCount = 0;
    public static final int maximumDistanceInGame = 1000;
    // get initial energy to calculate the energy level
    public static double initialEnergy;
    public static int stateCount = 0;

    public static int stateMap[][][][][][] = new int[numEnemyDistance][numEnemyBearing][numDirection][numEnergy][numHitWall][numHitByBullet];

    static {
        for (int a = 0; a < numEnemyDistance; ++a)
            for (int b = 0; b < numEnemyBearing; ++b)
                for (int c = 0; c < numDirection; ++c)
                    for (int d = 0; d < numEnergy; ++d)
                        for (int e = 0; e < numHitWall; ++e)
                            for (int f = 0; f < numHitByBullet; ++f)
                                stateMap[a][b][c][d][e][f] = statesCount++;
    }

    public static int getDistance(double val) {
        int distance = (int) (val / (1000 / numEnemyDistance));
        return distance >= numEnemyDistance ? distance - 1 : distance;
    }

    public static int calcDistanceState(double eventDistance){
        int distanceRanges = maximumDistanceInGame/numEnemyDistance;
        int currentDistance = (int) eventDistance/distanceRanges;
        return (currentDistance < numEnemyDistance)? currentDistance: numEnemyDistance-1;
    }

    public static int calcEnergyState(double robotEnergyFromEvent){
        // energy is categorized to 4 levels: //low, less than half, more than half, high
        double energyLevelRange = initialEnergy/numEnergy;
        int currentEnergy = (int) (robotEnergyFromEvent/energyLevelRange);
        return (currentEnergy < numEnergy)? currentEnergy: numEnergy-1;
    }

    public static int calcBearingState(double eventBearing){
        // a scan returns a bearing relative to your heading: North = 0
        // TODO
        eventBearing = (eventBearing < 0)? (eventBearing+Math.PI*2):eventBearing;
        double bearingRange = Math.PI*2 / numEnemyBearing;

        return 0;
    }

    //note that if the heading is 340, it's actually the same as 20 (pointing to almost the same direction)
    public static int getEnemyBearing(double val) {
        if (val < 0) val += 360;
        double angleRangePerArea = 360 / numEnemyBearing;
        double newBearing = val + angleRangePerArea / 2;
        return (int) (newBearing > 360 ? ((newBearing - 360) / angleRangePerArea) : (newBearing / angleRangePerArea));
    }

    //note that if the heading is 340, it's actually the same as 20 (pointing to almost the same direction)
    public static int getDirection(double val) {
        double angleRangePerArea = 360 / numDirection;
        double newHeading = val + angleRangePerArea / 2;
        return (int) (newHeading > 360 ? ((newHeading - 360) / angleRangePerArea) : (newHeading / angleRangePerArea));
    }

    public static int getEnergy(double val) {
        return val >= 100 ? numEnergy - 1 : (int) (val / (100 / numEnergy));
    }

    public static int getState(int distance, int enemyDirection, int direction, int energy, int hitWall, int hitByBullet){
        return stateMap[distance][enemyDirection][direction][energy][hitWall][hitByBullet];
    }
}
