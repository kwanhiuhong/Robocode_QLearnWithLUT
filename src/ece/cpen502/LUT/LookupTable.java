package ece.cpen502.LUT;

import ece.cpen502.Interface.LUTInterface;

import java.io.File;
import java.io.IOException;

public class LookupTable implements LUTInterface {
    public double[][] lookupTable;

    //constructor
    public LookupTable(int numStates, int numActions){
        lookupTable = new double[numStates][numActions];
        initialiseLUT(numStates, numActions);
    }

    public void save(File argFile) {

    }

    public void load(String argFileName) throws IOException {

    }

    @Override
    public void initialiseLUT(int numStates, int numActions) {
        for (int i = 0; i < numStates; ++i)
            for (int j = 0; j < numActions; ++j)
                lookupTable[i][j] = 0.0;
    }
}
