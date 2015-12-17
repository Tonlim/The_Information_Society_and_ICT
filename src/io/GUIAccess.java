package io;

import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class GUIAccess {
	private static void displayTextOnFXContext(String text, Text statusMessage){
		statusMessage.setText(text);
	}
	
	public static void displayText(String text,Text statusMessage){
		Platform.runLater(new Runnable(){
			@Override
			public void run(){
				displayTextOnFXContext(text,statusMessage);
			}
		});
	}
	
	public static void displayTextAndFocus(String text, Text statusMessage, Stage primaryStage){
		Platform.runLater(new Runnable(){
			@Override
			public void run(){
				displayTextOnFXContext(text,statusMessage);
				primaryStage.requestFocus();
			}
		});
	}
}
