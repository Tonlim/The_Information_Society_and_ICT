package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import weka.core.converters.ArffSaver;

public class ArffAccess {	
	/*
	 * Writes the given data to the given .arff file (".arff" must be included in the filename)
	 */
	public static void write(Instances data,String filename){
		try {
			ArffSaver saver = new ArffSaver();
			saver.setInstances(data);
			saver.setFile(new File("resources/arff/"+filename));
			saver.writeBatch();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	/*
	 * Returns the Instances of the given .arff file (".arff" must be included in the filename)
	 */
	public static Instances read(String filename){
		Instances result = null;
		try{
			BufferedReader reader = new BufferedReader(new FileReader("resources/arff/"+filename));
			ArffReader arff = new ArffReader(reader);
			result = arff.getData();
			reader.close();
		} catch(IOException e){
			e.printStackTrace();
		}
		return result;
	}
}
