package ece.cpen502.LUT;

import ece.cpen502.Interface.CommonInterface;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;

public class LearningAgent implements CommonInterface {
    public enum Algo{QLearn, Sarsa};

    private double learningRate = 0.2;
    private double discountFactor = 0.9;
    private LookupTable lookupTable;
    private int prevState = -1, prevAction = -1;

    public LearningAgent(LookupTable table) {
        this.lookupTable = table;
    }

    @Override
    public double train(double [] X, double argValue) { return 0; };

    public void train(int curState, int curAction, double reward, Algo algo) {
        if (prevState != -1 || prevAction != -1) {
            double Q = this.lookupTable.get(prevState, prevAction);
            switch (algo) {
                case QLearn:
                    Q += this.learningRate * (reward + this.discountFactor * this.lookupTable.getMax(curState) - Q);
                    break;
                case Sarsa:
                    Q += this.learningRate * (reward + this.discountFactor * this.lookupTable.get(curState, curAction) - Q);
                    break;
            }
            this.lookupTable.set(prevState, prevAction, Q);
        }
        prevState = curState;
        prevAction = curAction;
    }

    public int getAction(int curState, double epsilon){
        if (Math.random() > epsilon)
            return this.lookupTable.getOptimalAction(curState);
        return (int) (Math.random() * RobotAction.actionsCount);
    }

    @Override
    public void save(File argFile) {

    }

    @Override
    public void load(String argFileName) throws IOException {

    }
// used in Robots
    public int selectAction(int curState){
        return 0;
    }

    public void learn(Pair<Integer, Integer> stateActionPair, double reward){
        int state = stateActionPair.getKey();
        int action = stateActionPair.getValue();
    }

    public double outputFor(double[] X) { return 0; }
}
