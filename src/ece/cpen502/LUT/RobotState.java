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

    public static int calcDistanceState(double eventDistance) {
        int distanceRanges = maximumDistanceInGame / numEnemyDistance; //  1000 / numEnemyDistance
        int currentDistance = (int) eventDistance / distanceRanges;
        return (currentDistance < numEnemyDistance) ? currentDistance: numEnemyDistance - 1;
    }

    public static int calcEnergyState(double robotEnergyFromEvent) {
        double energyLevelRange = initialEnergy / numEnergy;
        int currentEnergy = (int) (robotEnergyFromEvent / energyLevelRange);
        return (currentEnergy < numEnergy) ? currentEnergy : numEnergy - 1;
    }

    //note that if the heading is 340, it's actually the same as 20 (pointing to almost the same direction)
    public static int getEnemyBearing(double val) {
        if (val < 0) val += 360;
        double angleRangePerArea = 360 / numEnemyBearing;
        double newBearing = val + angleRangePerArea / 2;
        return (int) (newBearing > 360 ? ((newBearing - 360) / angleRangePerArea) : (newBearing / angleRangePerArea));
    }

    public static int getDirection(double val) {
        double angleRangePerArea = 360 / numDirection;
        double newHeading = val + angleRangePerArea / 2;
        return (int) (newHeading > 360 ? ((newHeading - 360) / angleRangePerArea) : (newHeading / angleRangePerArea));
    }


    public static int getStateIndex(int distance, int enemyDirection, int direction, int energy, int hitWall, int hitByBullet) {
        return stateMap[distance][enemyDirection][direction][energy][hitWall][hitByBullet];
    }

    public static int[] getStates(int stateIndex) {
        int distance, bearing, direction, energy, hitWall, hitByBullet, remainder;

        distance = stateIndex/(numEnemyBearing*numDirection*numEnergy*numHitWall*numHitByBullet);
        remainder = stateIndex % (numEnemyBearing*numDirection*numEnergy*numHitWall*numHitByBullet);
        bearing = remainder/(numDirection*numEnergy*numHitWall*numHitByBullet);
        remainder = remainder % (numDirection*numEnergy*numHitWall*numHitByBullet);
        direction = remainder/(numEnergy*numHitWall*numHitByBullet);
        remainder = remainder % (numEnergy*numHitWall*numHitByBullet);
        energy = remainder/(numHitWall*numHitByBullet);
        remainder = remainder % (numHitWall*numHitByBullet);
        hitWall = remainder / (numHitByBullet);
        hitByBullet = remainder % (numHitByBullet);

        int[] states = new int[6];
        states[0] = distance;
        states[1] = bearing;
        states[2] = direction;
        states[3] = energy;
        states[4] = hitWall;
        states[5] = hitByBullet;
        return states;
    }
}
