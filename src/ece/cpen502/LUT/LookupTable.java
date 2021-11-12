package ece.cpen502.LUT;

import ece.cpen502.Interface.LUTInterface;

import java.io.File;
import java.io.IOException;

public class LookupTable implements LUTInterface {
    private int numStates = RobotState.statesCount;
    private int numActions = RobotAction.actionsCount;
    public double[][] lookupTable;

    //constructor
    public LookupTable(){
        this.lookupTable = new double[this.numStates][this.numActions];
        this.initialiseLUT();
    }

    public void set(int state, int action, double value){
        this.lookupTable[state][action] = value;
    }

    public double get(int state, int action){
        return this.lookupTable[state][action];
    }

    public double getMax(int state){
        double max = Double.NEGATIVE_INFINITY;
        for (double QValue: this.lookupTable[state])
            max = Math.max(QValue, max);
        return max;
    }

    public int getOptimalAction(int state){
        int idx = 0;
        for (double QValue: this.lookupTable[state]){
            if (QValue == this.getMax(state)) return idx;
            ++idx;
        }
        return 0;
    }

    //TODO
//    public void save(File argFile) {
//
//    }
//
//    public void load(String argFileName) throws IOException {
//
//    }

    @Override
    public void initialiseLUT() {
        for (int i = 0; i < this.numStates; ++i)
            for (int j = 0; j < this.numActions; ++j)
                this.lookupTable[i][j] = 0.0;
    }
}
