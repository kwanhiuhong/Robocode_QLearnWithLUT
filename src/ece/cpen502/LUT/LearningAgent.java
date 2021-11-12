package ece.cpen502.LUT;

import ece.cpen502.Interface.CommonInterface;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;

public class LearningAgent implements CommonInterface {

    public double outputFor(double[] X) { return 0; }

    @Override
    public double train(double[] X, double argValue) {
        return 0;
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
}
