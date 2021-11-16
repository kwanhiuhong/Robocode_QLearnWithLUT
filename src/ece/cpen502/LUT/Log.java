package ece.cpen502.LUT;

import robocode.RobocodeFileWriter;
import java.io.*;

public class Log {
    public void writeToFile(File fileToWrite, double winRate, int roundCount) {
        try{
            RobocodeFileWriter fileWriter = new RobocodeFileWriter(fileToWrite.getAbsolutePath(), true);
            fileWriter.write(roundCount + " " + Double.toString(winRate) + "\r\n");
            fileWriter.close();
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
}
