package ece.cpen502.LUT;

public class RobotAction {
    public static final int moveForward = 0, moveBack = 1;
    public static final int headRight = 2, headLeft = 3;
    public static final int backRight = 4, backLeft = 5;
    // add one advance action
    public static final int tryFire = 6;
    public static final int goToCenter = 7;

    public static final int actionsCount = 8;
    public static final double moveDistance = 100.0, moveDegree = 20.0;
}
