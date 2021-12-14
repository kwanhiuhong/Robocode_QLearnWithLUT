package ece.cpen502.LUT;

import ece.cpen502.Interface.LUTInterface;
import robocode.RobocodeFileOutputStream;

import java.io.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class LookupTable implements LUTInterface {
    private int numStates = RobotState.statesCount;
    private int numActions = RobotAction.actionsCount;
    public double[][] lookupTable;
    public  int robotID;

    public LookupTable(){
        this.lookupTable = new double[this.numStates][this.numActions];
        this.initialiseLUT();
        robotID = new Random().nextInt();
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

    public void save(File argFile) {
        PrintStream saveLUT = null;
        try {
            saveLUT = new PrintStream(new RobocodeFileOutputStream(argFile));
            for (int i = 0; i < numStates; i++)
                for (int j = 0; j < numActions; j++)
                    saveLUT.println(lookupTable[i][j]);

            if (saveLUT.checkError())
                System.out.println("Could not save the data!");
            saveLUT.close();
        }
        catch (IOException e)
        {
            System.out.println("IOException trying to write: " + e);
        }
        finally
        {
            try {
                if (saveLUT != null)
                    saveLUT.close();
            }
            catch (Exception e)
            {
                System.out.println("Exception trying to close witer: " + e);
            }
        }
    }

    public void load(String argFileName) throws IOException {
        FileInputStream inputFile = new FileInputStream(argFileName);
        BufferedReader inputReader = null;

        try   {
            inputReader = new BufferedReader(new InputStreamReader(inputFile));
            for (int i = 0; i < RobotState.statesCount; i++)
                for (int j = 0; j < RobotAction.actionsCount; j++)
                    lookupTable[i][j] = Double.parseDouble(inputReader.readLine());
        }
        catch (IOException e)   {
            System.out.println("IOException trying to open reader: " + e);
        }
        catch (NumberFormatException e)   {
        }
        finally {
            try {
                if (inputReader != null)
                    inputReader.close();
            }
            catch (IOException e) {
                System.out.println("IOException trying to close reader: " + e);
            }
        }
    }

    @Override
    public void initialiseLUT() {
        for (int i = 0; i < this.numStates; ++i)
            for (int j = 0; j < this.numActions; ++j)
                this.lookupTable[i][j] = 0.0;
    }

    public double[][] getLutTable(){
        return lookupTable;
    }
}
