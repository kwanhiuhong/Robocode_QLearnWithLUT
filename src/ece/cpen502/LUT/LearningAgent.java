package ece.cpen502.LUT;

import ece.cpen502.Interface.CommonInterface;

import java.io.File;
import java.io.IOException;

public class LearningAgent implements CommonInterface {
    private double learningRate = 0.2;
    private double discountFactor = 0.9;
    private double explorationRate = 0.8; //this will be reduced by 0.1 after each 1000 rounds of games
    private LookupTable lookupTable;

    public LearningAgent(LookupTable table) {
        this.lookupTable = table;
    }

    @Override
    public double train(double[] X, double argValue) {
        return 0;
    }

//    public void qLearn(int nextState, int nextAction, double reward) {
//        double QValue;
//        if (firstRound) {
//            firstRound = false;
//        } else {
//            QValue = this.lookupTable.get(currentState, currentAction);
//            QValue += this.learningRate * (reward + this.discountFactor * this.lookupTable.getMax(nextState) - QValue);
//            this.lookupTable.set(currentState, currentAction, QValue);
//
//        }
//        currentState = nextState;
//        currentAction = nextAction;
//    }

//    public void sarsa(int nextState, int nextAction, double reward) {
//        double oldQ;
//        double newQ;
//        if (firstRound) {
//            firstRound = false;
//        } else {
//            oldQ = table.getQValue(currentState, currentAction);
//            newQ = oldQ + learningRate*(reward + discountRate * table.getQValue(nextState, nextAction)-oldQ);
//            table.setQvalue(currentState, currentAction, newQ);
//
//        }
//        currentState = nextState;
//        currentAction = nextAction;
//    }

    @Override
    public void save(File argFile) {

    }

    @Override
    public void load(String argFileName) throws IOException {

    }

    public double outputFor(double[] X) { return 0; }
}
