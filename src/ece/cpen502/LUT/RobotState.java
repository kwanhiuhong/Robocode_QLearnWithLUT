package ece.cpen502.LUT;

public class RobotState {

    public static final int numDistance = 10; //each distance is 80 units
    public static final int numBearing = 4; //head, back, right, left
    public static final int numEnergy = 4; //low, less than half, more than half, high
    public static final int numHitWall = 2; //we hit wall or we not
    public static final int numHitByBullet = 2; //we got shot by we not
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
}
