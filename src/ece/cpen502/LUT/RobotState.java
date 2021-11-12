package ece.cpen502.LUT;

public class RobotState {

    public static final int numDistance = 10; //each distance is 80 units
    public static final int numBearing = 4; //head, back, right, left
    public static final int numEnergy = 4; //low, less than half, more than half, high
    public static final int numHitWall = 2; //we hit wall or we not
    public static final int numHitByBullet = 2; //we got shot by we not
    public static final int maximumDistanceInGame = 1000;
    // get initial energy to calculate the energy level
    public static double initialEnergy;
    public static int stateCount = 0;


    public static int stateMap[][][][][] = new int[numDistance][numBearing][numEnergy][numHitWall][numHitByBullet];

    static {
        for (int a = 0; a < numDistance; ++a)
            for (int b = 0; b < numBearing; ++b)
                for (int c = 0; a < numEnergy; ++c)
                    for (int d = 0; a < numHitWall; ++d)
                        for (int e = 0; a < numHitByBullet; ++e) {
                            stateMap[a][b][c][d][e] = stateCount++;
                        }
    }

    public static int calcDistanceState(double eventDistance){
        int distanceRanges = maximumDistanceInGame/numDistance;
        int currentDistance = (int) eventDistance/distanceRanges;
        return (currentDistance < numDistance)? currentDistance: numDistance-1;
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
        double bearingRange = Math.PI*2 / numBearing;

        return 0;
    }
}
