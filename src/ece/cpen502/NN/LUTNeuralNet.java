package ece.cpen502.NN;

import ece.cpen502.Interface.NeuralNetInterface;
import ece.cpen502.LUT.LookupTable;
import ece.cpen502.LUT.RobotAction;
import ece.cpen502.LUT.RobotState;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

public class LUTNeuralNet implements NeuralNetInterface {

    private final boolean isBinary; //true = binary training sets used, false = bipolar training sets
    private final double learningRate;
    private final double momentum;
    private final int numHiddenNeurons;

    //hyper-parameters
    private static final double errorThreshold = 0.1;
    private static final int maxSteps = 2000;
    private static final int numInputs = 7; //6 state categories + 1 action
    private static final int numOutputs = 1;
    private static int currentTrainingSet = 0;
    private static int bias = 1;

    //upper and lower bounds for initializing weights
    private double weightMin = -0.5;
    private double weightMax = 0.5;


    //weights
    private static double[][] inputToHiddenWeights, hiddenToOutputWeights; //+1 to accommodate a bias weight
    private double[][] deltaWHiddenToOutput, deltaWInputToHidden;

    //inputs
    private double[][] inputVectors, expectedOutput;

    //for save and load
    private boolean areWeightsLoaded = false;
    private final String separator = "break"; //used to separate multiple groups of weights when outputting/reading to/from a file
    private String savedOutputPath = "";
    private int saveOncePerNoOfEpochs = 50;
    public static String fileNameSaved = "";

    LUTNeuralNet(double[][] input, double[][] output,
                 double lrnRate, double inputMomentum,
                 int noOfHiddenNeurons, boolean isBinaryTraining,
                 String progressOutputPath) {
        inputVectors = input;
        expectedOutput = output;
        learningRate = lrnRate;
        momentum = inputMomentum;
        numHiddenNeurons = noOfHiddenNeurons;
        isBinary = isBinaryTraining;
        savedOutputPath = progressOutputPath;

        inputToHiddenWeights = new double[numInputs + 1][numHiddenNeurons+1];
        hiddenToOutputWeights = new double[numHiddenNeurons + 1][numOutputs];

        deltaWHiddenToOutput = new double[numHiddenNeurons + 1][numOutputs];
        deltaWInputToHidden = new double[inputVectors.length][numHiddenNeurons + 1];
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
    public double[] forwardToHidden() {
        double[] outputsHidden = new double[numHiddenNeurons + 1];
        outputsHidden[0] = bias;

        //outputs from the hidden neurons
        for (int i = 1; i < outputsHidden.length; i++) {
            outputsHidden[i] = 0;
            for (int j = 0; j < inputToHiddenWeights.length; j++) {
                outputsHidden[i] += inputVectors[currentTrainingSet][j] * inputToHiddenWeights[j][i];
            }
            outputsHidden[i] = sigmoid(outputsHidden[i]);  //apply activation function
        }
        return outputsHidden;
    }

    public double[] forwardToOutput(double[] outputsHidden) {
        double[] outputs = new double[numOutputs];
        //outputs from the output neuron
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] = 0;
            for (int j = 0; j < hiddenToOutputWeights.length; j++) {
                outputs[i] += outputsHidden[j] * hiddenToOutputWeights[j][i];
            }
            outputs[i] = sigmoid(outputs[i]); //apply activation function
        }
        return outputs;
    }

    public void backPropagation(double[] outputs, double[] outputsHidden) {

        double[] outputErrorSignals = new double[numOutputs];
        double[] hiddenErrorSignals = new double[numHiddenNeurons + 1];

        //compute the error signals at the outputs neurons
        if (isBinary) {
            for (int i = 0; i < outputs.length; i++) {
                outputErrorSignals[i] = (expectedOutput[currentTrainingSet][i] - outputs[i]) *
                        outputs[i] * (1 - outputs[i]);
            }
        } else {
            for (int i = 0; i < outputs.length; i++) {
                outputErrorSignals[i] = (expectedOutput[currentTrainingSet][i] - outputs[i]) *
                        (1 - outputs[i] * outputs[i]) * 0.5;
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
                                            + learningRate * hiddenErrorSignals[j] * inputVectors[currentTrainingSet][i];
                inputToHiddenWeights[i][j] += deltaWInputToHidden[i][j];
            }
        }
    }

    public ArrayList train() {
        double[] outputsHidden;
        double[] outputs;
        double error;
        int numSuccessfulTraining = 0;
        int avgTrainingEpochs = 0;

        ArrayList<Double> errorList = null;

        for (int trial = 0; trial < 10; trial ++) {
            int epoch = 0;
            errorList = new ArrayList<>();

            if (!areWeightsLoaded) initializeWeights();

            do {
                currentTrainingSet = 0;
                error = 0;

                while (currentTrainingSet < inputVectors.length) {
                    outputsHidden = forwardToHidden();
                    outputs = forwardToOutput(outputsHidden);
                    backPropagation(outputs, outputsHidden);

                    for (int i = 0; i < outputs.length; i++) {
                        error += Math.pow((outputs[i] - expectedOutput[currentTrainingSet][i]),2);
                    }
                    currentTrainingSet++;
                }

                if (savedOutputPath.length() > 0 && epoch % saveOncePerNoOfEpochs == 0) save(new File(savedOutputPath));

                error = Math.sqrt(error / (RobotState.statesCount * RobotAction.actionsCount));
                epoch++;
                errorList.add(error);
            } while (error > errorThreshold && maxSteps > epoch);

            if (epoch < maxSteps) {
                numSuccessfulTraining++;
                avgTrainingEpochs += epoch;
                System.out.println("Successful training with " + epoch + " number of epochs.");
                System.out.println("The RMS error is " + error);
            } else {
                System.out.println("The number of epochs has exceeded "+ maxSteps +
                        ". Unable to reduce the RMS error to below " + errorThreshold);
            }
        }
        System.out.println("On average, it takes " + avgTrainingEpochs / numSuccessfulTraining + " epochs to converge.");
        return errorList;
    }

    public static void textWriter(String fileName, ArrayList<Double> list) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Double aDouble : list) {
            assert writer != null;
            writer.write(aDouble + "\n");
        }
        assert writer != null;
        writer.close();
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
    }

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
    }

    public static double[] normalizeStatesInputs(int[] statesInputs, double lowerBound, double upperBound) {
        double[] normalizedStates = new double[6];
        for(int i = 0; i < 6; i++) {
            switch (i) {
                case 0: //enemy distance
                    normalizedStates[0] = scaleRange(statesInputs[0],0,RobotState.numEnemyDistance-1,lowerBound,upperBound);
                    break;
                case 1: //enemy bearing
                    normalizedStates[1] = scaleRange(statesInputs[1],0,RobotState.numEnemyBearing-1,lowerBound,upperBound);
                    break;
                case 2: //direction
                    normalizedStates[2] = scaleRange(statesInputs[2],0,RobotState.numDirection-1,lowerBound,upperBound);
                    break;
                case 3: //energy
                    normalizedStates[3] = scaleRange(statesInputs[3],0,RobotState.numEnergy-1,lowerBound,upperBound);
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

    public static double findMax(double[] Array) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : Array) {
            max = Math.max(v, max);
        }
        return max;
    }

    public static double findMin(double[] Array) {
        double min = Double.POSITIVE_INFINITY;
        for (double v : Array) {
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
    public double outputFor(double[] X) {
        return 0;
    }

    @Override
    public double train(double[] X, double argValue) {
        return 0;
    }

    public static void fileWriter(double[][] data, String fileType){
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        try{
            FileWriter myWriter = new FileWriter(fileType+".txt");
            for (double[] doubles : data) {
                for (int j = 0; j < data[0].length; j++) {
                    myWriter.write(Double.toString(doubles[j]) + "\r\n");
                }
            }
            myWriter.close();
        }catch (Exception e){
            System.out.println(e);
        }
    }

    public static void main(String[] args) throws IOException {
        double learningRate = 0.1;
        int noOfHiddenNeurons = 15;
        double momentum = 0.2;

        //Get LUT from assignment 2 and normalize to range [-1,1]
        LookupTable lut = new LookupTable();
        lut.load("LUT");
        double[][] botExpectedOutputs = lut.getLutTable();
        double[][] normExpectedOutputs = normalizeOutputs(botExpectedOutputs,-1,1);

        double[][] expectedOutputsLUT = new double[RobotState.statesCount * RobotAction.actionsCount][numOutputs];
        int counter = 0;
        for (int i = 0; i < RobotState.statesCount; i++) {
            for (int j = 0; j < RobotAction.actionsCount; j++) {
                expectedOutputsLUT[counter][0] = normExpectedOutputs[i][j];
                counter++;
            }
        }

        //Normalize actions to range [-1,1]
        double[] normalizedActions = new double[RobotAction.actionsCount];
        for (int i = 0; i < RobotAction.actionsCount; i++) {
            normalizedActions[i] = scaleRange(i,0,RobotAction.actionsCount-1, -1,1);
        }

        double[][] inputs = new double[RobotState.statesCount * RobotAction.actionsCount][numInputs + 1];
        for (int j = 0; j < RobotState.statesCount * RobotAction.actionsCount; j++) {
            int[]states = RobotState.getStates(j / RobotAction.actionsCount);
            double[] normalizedStates = normalizeStatesInputs(states,-1,1); //Normalize states inputs
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

        LUTNeuralNet lutTraining = new LUTNeuralNet(inputs, expectedOutputsLUT, learningRate, momentum,noOfHiddenNeurons,false, "LUTNN_Weights.txt");

        ArrayList lutRMSError = lutTraining.train();
        textWriter("lutRMSError.txt", lutRMSError);
        fileWriter(inputToHiddenWeights, "inputToHiddenWeights");
        fileWriter(hiddenToOutputWeights, "hiddenToOutputWeights");
    }
}

