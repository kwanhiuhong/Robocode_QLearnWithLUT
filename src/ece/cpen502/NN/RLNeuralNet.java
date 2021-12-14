package ece.cpen502.NN;

import ece.cpen502.Interface.NeuralNetInterface;
import ece.cpen502.LUT.RobotAction;
import ece.cpen502.LUT.RobotState;

import java.io.*;
import java.util.*;

public class RLNeuralNet implements NeuralNetInterface {

    private final boolean isBinary; //true = binary training sets used, false = bipolar training sets
    private final double learningRate;
    private final double momentum;
    private final int numHiddenNeurons;

    //hyper-parameters
    public static final int numInputs = 7; //6 state categories + 1 action
    public static final int numOutputs = 1;

    //upper and lower bounds for initializing weights
    private double weightMin = -0.5;
    private double weightMax = 0.5;

    //weights
    private double[][] inputToHiddenWeights, hiddenToOutputWeights; //+1 to accommodate a bias weight
    private double[][] deltaWHiddenToOutput, deltaWInputToHidden;
    int bias = 1;

    //for save and load
    public boolean areWeightsLoaded = false;
    private final String separator = "break"; //used to separate multiple groups of weights when outputting/reading to/from a file
    private String savedOutputPath = "";
    private int saveOncePerNoOfEpochs = 50;
    public RLNeuralNet(double lrnRate, double inputMomentum,
                       int noOfHiddenNeurons, boolean isBinaryTraining,
                       String progressOutputPath) {
        learningRate = lrnRate;
        momentum = inputMomentum;
        numHiddenNeurons = noOfHiddenNeurons;
        isBinary = isBinaryTraining;
        savedOutputPath = progressOutputPath;

        inputToHiddenWeights = new double[numInputs + 1][numHiddenNeurons+1];
        hiddenToOutputWeights = new double[numHiddenNeurons + 1][numOutputs];

        deltaWHiddenToOutput = new double[numHiddenNeurons + 1][numOutputs];
        deltaWInputToHidden = new double[numInputs + 1][numHiddenNeurons + 1];
    }

    public double[][] getInputToHiddenWeights(){
        return this.inputToHiddenWeights;
    }
    public void setInputToHiddenWeights(double[][] weights){
        this.inputToHiddenWeights = weights;
    }

    //    public double[][] getHiddenToOutputWeights(){
//        return this.hiddenToOutputWeights;
//    }

    public void setHiddenToOutputWeights(double[][] weights){
        this.hiddenToOutputWeights = weights;
    }

    //Initialize weights to random values in the range [weightMin, weightMax]
    @Override
    public void initializeWeights() {
        //Initialize weights from the inputs to the neurons at the hidden layer
        for (int i = 0; i < inputToHiddenWeights.length; i++) {
            for (int j = 0; j < inputToHiddenWeights[i].length; j++) {
                inputToHiddenWeights[i][j] = weightMin + (new Random().nextDouble() * (weightMax - weightMin));
            }
        }

        for (int i = 0; i < hiddenToOutputWeights.length; i++) {
            for (int j = 0; j < hiddenToOutputWeights[i].length; j++) {
                hiddenToOutputWeights[i][j] = weightMin + (new Random().nextDouble() * (weightMax - weightMin));
            }
        }
    }

    //The activation function
    @Override
    public double sigmoid(double x) {
        if (isBinary) {
            return 1 / (1 + Math.pow(Math.E, -x)); //sigmoid function for binary training sets
        } else {
            return -1 + 2 / (1 + Math.pow(Math.E, -x)); //sigmoid function for bipolar training sets
        }
    }

    //Forward propagation to calculate the outputs from the hidden neurons and the output neuron(s)
    public double[] forwardToHidden(double[][] inputVectors, int actionID) {
        double[] outputsHidden = new double[numHiddenNeurons + 1];
        outputsHidden[0] = bias;
        //outputs from the hidden neurons
        for (int i = 1; i < outputsHidden.length; i++) {
            outputsHidden[i] = 0;
            for (int j = 0; j < inputToHiddenWeights.length; j++) {
                outputsHidden[i] += inputVectors[actionID][j] * inputToHiddenWeights[j][i];
            }
            outputsHidden[i] = sigmoid(outputsHidden[i]);  //apply activation function
        }
        return outputsHidden;
    }

    public double[] forwardToOutput(double[] outputsHidden) {
        double[] outputs = new double[numOutputs];
        //outputs from the output neuron
        // outputs.length = 1 since we only have one output
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] = 0;
            for (int j = 0; j < hiddenToOutputWeights.length; j++) {
                outputs[i] += outputsHidden[j] * hiddenToOutputWeights[j][i];
            }
            outputs[i] = sigmoid(outputs[i]); //apply activation function
        }
        return outputs;
    }

    public void backPropagation(double[] state, int action, double Q) {

        double[] outputErrorSignals = new double[numOutputs];
        double[] hiddenErrorSignals = new double[numHiddenNeurons + 1];

        double[] outputsHidden = forwardToHidden(adjustInputs(state), action);
        double[] outputs = forwardProp(state);

        //compute the error signals at the outputs neurons
        if (isBinary) {
            for (int i = 0; i < numOutputs; i++) {
                outputErrorSignals[i] = (Q - outputs[i]) *
                        outputs[i] * (1 - outputs[i]);
            }
        } else {
            for (int i = 0; i < numOutputs; i++) {
                outputErrorSignals[i] = (Q - outputs[action]) *
                        (1 - outputs[action] * outputs[action]) * 0.5;
            }
        }

        //update weights from the hidden layer to the outputs
        for (int i = 0; i < hiddenToOutputWeights.length; i++) {
            for (int j = 0; j < hiddenToOutputWeights[i].length; j++) {
                deltaWHiddenToOutput[i][j] = momentum * deltaWHiddenToOutput[i][j]
                        + learningRate * outputErrorSignals[j] * outputsHidden[i];
                hiddenToOutputWeights[i][j] += deltaWHiddenToOutput[i][j];
            }
        }

        //compute the error signals at the hidden neurons
        for (int i = 0; i < hiddenErrorSignals.length; i++) {
            hiddenErrorSignals[i] = 0;
            for (int j = 0; j < numOutputs; j++) {
                hiddenErrorSignals[i] += hiddenToOutputWeights[i][j] * outputErrorSignals[j];
            }
            if (isBinary) {
                hiddenErrorSignals[i] *= outputsHidden[i] * (1 - outputsHidden[i]);
            } else {
                hiddenErrorSignals[i] *= (1 - outputsHidden[i] * outputsHidden[i]) / 2.0;
            }
        }

        //update weights from the inputs to the hidden layers
        for (int i = 0; i < inputToHiddenWeights.length; i++) {
            for (int j = 0; j < inputToHiddenWeights[i].length; j++) {
                deltaWInputToHidden[i][j] = momentum * deltaWInputToHidden[i][j]
                        + learningRate * hiddenErrorSignals[j] * adjustInputs(state)[action][i];
                inputToHiddenWeights[i][j] += deltaWInputToHidden[i][j];
            }
        }
    }

    /**
     * Given a state, returns the q values for all 6 state action pairs
     */
    public double[] forwardProp(double[] state) {
        double[] outputs = new double[RobotAction.actionsCount];
        double[][] inputs = adjustInputs(state);

        if (!areWeightsLoaded) {
            initializeWeights();
            areWeightsLoaded = true;
        }
        for (int i = 0; i < RobotAction.actionsCount; i++) {
            double[] outputsHidden = forwardToHidden(inputs, i);
            outputs[i] = forwardToOutput(outputsHidden)[0];
        }
        return outputs;
    }

    public double getQ (double[] state, int actionID) {
        double[] outputs = forwardProp(state);
        return outputs[actionID];
    }

    public double getMaxQ(double[] state) {
        double[] outputs = forwardProp(state);
        return findMax(outputs);
    }

    public int getOptimalAction (double[] state) {
        int optimalAction = -1;
        double[] outputs = forwardProp(state);
        double maxQ = getMaxQ(state);

        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i] == maxQ) {
                optimalAction = i;
            }
        }
        return optimalAction;
    }

    @Override
    public void save(File argFile) {
        PrintStream saveFile = null;
        try{
            saveFile = new PrintStream(new FileOutputStream(argFile,false) );
            String inputToHiddenWeightsString = "";
            String hiddenToOutputWeightsString = "";

            for (double[] eachSetOfWeights : inputToHiddenWeights) {
                for (double eachWeight: eachSetOfWeights) {
                    inputToHiddenWeightsString += eachWeight + ",";
                }
                inputToHiddenWeightsString += "\n";
            }
            for (double[] eachSetOfWeights : hiddenToOutputWeights) {
                for (double eachWeight: eachSetOfWeights) {
                    hiddenToOutputWeightsString += eachWeight + ",";
                }
                hiddenToOutputWeightsString += "\n";
            }
            saveFile.println(inputToHiddenWeightsString + separator + "\n" + hiddenToOutputWeightsString);
            saveFile.flush();
            saveFile.close();
        }
        catch(IOException e){
            System.out.println("Cannot save the weight table.");
        }

    };

    @Override
    public void load(String argFileName){
        try {
            File myFile = new File(argFileName);
            Scanner reader = new Scanner(myFile);

            int i = 0;
            int subArraySize = numHiddenNeurons+1;
            //read weights for inputToHiddenWeights
            while (reader.hasNextLine()) {
                String setOfWeights = reader.nextLine();
                if (setOfWeights.contains(separator)) {
                    subArraySize = numOutputs;
                    i = 0;
                    break;
                }
                String[] parts = setOfWeights.split(",");
                for (int idx = 0; idx < subArraySize; ++idx) {
                    this.inputToHiddenWeights[i][idx] = Double.parseDouble(parts[idx]);
                }
                i++;
            }

            //read weights for hiddenToOutputWeights
            while (reader.hasNextLine()) {
                String setOfWeights = reader.nextLine();
                String[] parts = setOfWeights.split(",");
                for (int idx = 0; idx < subArraySize; ++idx) {
                    this.hiddenToOutputWeights[i][idx] = Double.parseDouble(parts[idx]);
                }
                i++;
                if (i == numHiddenNeurons + 1) {
                    break;
                }
            }

            areWeightsLoaded = true;
            reader.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    };

    public double[][] adjustInputs(double[] state) {
        //Normalize actions to range [-1,1]
        double[] normalizedActions = new double[RobotAction.actionsCount];
        for (int i = 0; i < RobotAction.actionsCount; i++) {
            normalizedActions[i] = scaleRange(i,0,RobotAction.actionsCount-1, -1,1);
        }

        double[][] inputs = new double[RobotAction.actionsCount][numInputs + 1];
        for (int j = 0; j < RobotAction.actionsCount; j++) {
            double[] normalizedStates = normalizeStatesInputs(state,-1,1); //Normalize states inputs
            inputs[j][0] = bias;

            for (int k = 1; k < numInputs - 1; k++) {
                inputs[j][k] = normalizedStates[k];
            }
            switch (j % RobotAction.actionsCount) {
                case 0:
                    inputs[j][numInputs-1] = normalizedActions[0];
                    break;
                case 1:
                    inputs[j][numInputs-1] = normalizedActions[1];
                    break;
                case 2:
                    inputs[j][numInputs-1] = normalizedActions[2];
                    break;
                case 3:
                    inputs[j][numInputs-1] = normalizedActions[3];
                    break;
                case 4:
                    inputs[j][numInputs-1] = normalizedActions[4];
                    break;
                case 5:
                    inputs[j][numInputs-1] = normalizedActions[5];
                    break;
            }
        }
        return inputs;
    }

    public static double[] normalizeStatesInputs(double[] statesInputs, double lowerBound, double upperBound) {
        double[] normalizedStates = new double[6];
        for(int i = 0; i < 6; i++) {
            switch (i) {
                case 0: //enemy distance
                    normalizedStates[0] = scaleRange(statesInputs[0],0,1000,lowerBound,upperBound);
                    break;
                case 1: //enemy bearing
                    normalizedStates[1] = scaleRange(statesInputs[1],-180,180,lowerBound,upperBound);
                    break;
                case 2: //direction
                    normalizedStates[2] = scaleRange(statesInputs[2],0,360,lowerBound,upperBound);
                    break;
                case 3: //energy
                    normalizedStates[3] = scaleRange(statesInputs[3],0,RobotState.initialEnergy,lowerBound,upperBound);
                    break;
                case 4: //hit wall
                    normalizedStates[4] = scaleRange(statesInputs[4],0,RobotState.numHitWall-1,lowerBound,upperBound);
                    break;
                case 5: //hit by bullet
                    normalizedStates[5] = scaleRange(statesInputs[5],0,RobotState.numHitByBullet-1,lowerBound,upperBound);
            }
        }
        return normalizedStates;
    }

    public static double[][] normalizeOutputs(double[][] initialOutputs,double lowerBound, double upperBound) {
        double[][] normOutputs = new double[RobotState.statesCount][RobotAction.actionsCount];
        double[] maxQ = new double[RobotAction.actionsCount]; //maximum Q value for each action
        double[] minQ = new double[RobotAction.actionsCount]; //minimum Q value for each action

        for (int actionID = 0; actionID < RobotAction.actionsCount; actionID++) {
            maxQ[actionID] = findMax(getColumn(initialOutputs,actionID));
            minQ[actionID] = findMin(getColumn(initialOutputs,actionID));
        }

        for (int i = 0; i < RobotState.statesCount; i++) {
            for (int j = 0; j < RobotAction.actionsCount; j++) {
                normOutputs[i][j] = scaleRange(initialOutputs[i][j],minQ[j],maxQ[j],lowerBound,upperBound);
            }
        }
        return normOutputs;
    }

    public static double scaleRange( double val, double minIn, double maxIn, double minOut, double maxOut){
        double result;
        result = minOut + ((maxOut - minOut) * (val - minIn) / (maxIn - minIn));
        return result;
    }

    public static double findMax(double[] arr) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : arr) {
            max = Math.max(v, max);
        }
        return max;
    }

    public static double findMin(double[] arr) {
        double min = Double.POSITIVE_INFINITY;
        for (double v : arr) {
            min = Math.min(v, min);
        }
        return min;
    }

    public static double[] getColumn(double[][] array, int columnIndex) {
        double[] column = new double[RobotState.statesCount];
        for(int i = 0; i< column.length; i++ ) {
            column[i] = array[i][columnIndex];
        }
        return column;
    }

    @Override
    public double outputFor(double[] x) {
        return 0;
    }

    @Override
    public double train(double[] x, double argValue) {
        return 0;
    }
}
