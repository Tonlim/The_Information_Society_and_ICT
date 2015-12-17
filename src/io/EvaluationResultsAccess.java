package io;

import java.io.FileWriter;
import java.io.Writer;

import weka.classifiers.Evaluation;

public class EvaluationResultsAccess {
	public static void writeResults(Evaluation evaluation, String fileName){
		try {
			Writer writer = new FileWriter("resources/results/"+fileName);
			writer.write(evaluation.toSummaryString()+"\n\n");
			writer.write(evaluation.toClassDetailsString()+"\n\n");			//comment for linear regression
			writer.write(evaluation.toSummaryString(true)+"\n\n");
			writer.write("Errorrate: "+evaluation.errorRate()+"\n\n");
			writer.write("Mean absolute error: "+evaluation.meanAbsoluteError()+"\n\n");
			writer.write("Root mean squared error: "+evaluation.rootMeanSquaredError()+"\n\n");
			writer.write("Recall: "+evaluation.recall(0)+"\n\n");				//comment for linear regression
			writer.write("Precision: "+evaluation.precision(0)+"\n\n");		//comment for linear regression
			writer.flush();
			writer.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void writeLinearResults(Evaluation evaluation, String fileName){
		try {
			Writer writer = new FileWriter("resources/results/"+fileName);
			writer.write(evaluation.toSummaryString()+"\n\n");
			writer.write(evaluation.toSummaryString(true)+"\n\n");
			writer.write("Errorrate: "+evaluation.errorRate()+"\n\n");
			writer.write("Mean absolute error: "+evaluation.meanAbsoluteError()+"\n\n");
			writer.write("Root mean squared error: "+evaluation.rootMeanSquaredError()+"\n\n");
			writer.flush();
			writer.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
