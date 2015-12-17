package application;
	
import io.ArffAccess;
import io.EvaluationResultsAccess;
import io.GUIAccess;
import io.JsonAccess;
import io.YoutubeAccess;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.VideoModel;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.stemmers.SnowballStemmer;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import com.google.gson.JsonIOException;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		//standard javaFX initalization
		BorderPane root = new BorderPane();
		Scene scene = new Scene(root,1600,820);	//400, 400 default               1600,820 fullscreen kinda
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.show();
		
		//statusMessage at bottom
		Text statusMessage = new Text();
		root.setBottom(statusMessage);
		
		//VBox for center content
		VBox center = new VBox();
		root.setCenter(center);;
		
		//menubars at top
		VBox top = new VBox();
		root.setTop(top);
		setUpMenuBars(top,center,statusMessage, primaryStage);
		
		//bit of layout
		BorderPane.setMargin(center, new Insets(10));
		center.setStyle("-fx-border-color: gray; -fx-border-width: 2");
		center.setPadding(new Insets(10));		
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	
	/*
	 * sets up the needed menubars in the given Vbox
	 */
	private void setUpMenuBars(VBox top, VBox center, Text statusMessage, Stage primaryStage){
		//menubar to download videoIDs
		HBox downloadVideoIDButtons = new HBox();
		top.getChildren().add(downloadVideoIDButtons);
		Text downloadVideoIDDescription = new Text("Download videoIDs to file of channel: ");
		downloadVideoIDButtons.getChildren().add(downloadVideoIDDescription);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDtalksDirector",statusMessage,primaryStage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDxTalks",statusMessage,primaryStage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDEducation",statusMessage,primaryStage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDxYouth",statusMessage,primaryStage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDFellowsTalks",statusMessage,primaryStage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDPartners",statusMessage,primaryStage);
		
		HBox extractCommentsBar = new HBox();
		top.getChildren().add(extractCommentsBar);
		addExtractCommentsButtonToHBox(extractCommentsBar,center,statusMessage,primaryStage);
		
		HBox databaseBar = new HBox();
		top.getChildren().add(databaseBar);
		addDatabaseOperationButtonToHBox(databaseBar,center,statusMessage,primaryStage);
		
		HBox machineLearningFileBar = new HBox();
		top.getChildren().add(machineLearningFileBar);
		addMachineLearningFilePreppingButtonsToHBox(machineLearningFileBar,center,statusMessage,primaryStage);
		
		HBox machineLearningBar = new HBox();
		top.getChildren().add(machineLearningBar);
		addBinairyMachineLearningButtonToHBox(machineLearningBar,center,statusMessage,primaryStage);
		addLinearMachineLearningButtonToHBox(machineLearningBar,center,statusMessage,primaryStage);
		
	}
	
	/*
	 * opens a section in the main window to ask for the name of a file that contains video id's to extract all data from the video
	 */
	private void addExtractCommentsButtonToHBox(HBox menuBox, VBox centerBox, Text statusMessage, Stage primaryStage){
		Button button = new Button("Extract data based on videoID in file");
		button.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File("resources/json"));
				fileChooser.setTitle("Please select the .json file to process and press 'open'");
				File databaseFile = fileChooser.showOpenDialog(primaryStage);
				String fileName = databaseFile.getName();	//this will throw an unhandled NullPointerException when 'cancel' is chosen.
				
				ArrayList<String> in = new ArrayList<String>();
				in = JsonAccess.readVideoIds(fileName);
				
				//final copy for use in new thread
				final String fileNameFinal = fileName;
				final ArrayList<String> inFinal = in;
				//heavy works starts here: new thread so it won't block the GUI
				new Thread(){
					@Override
					public void run(){
						//get the video models    --- very time-consuming piece of code
						//we are using a ThreadPool of 100 threads for this. Each request for a videoID will run in it's own thread.
						//This speeds up the program as each thread waits for an answer of google. Parallel requests => less time waiting
						GUIAccess.displayText("Video data download started.", statusMessage);
						
						long startTime = System.currentTimeMillis();
						YoutubeAccess youtube = new YoutubeAccess(statusMessage);
						ArrayList<VideoModel> videoData = new ArrayList<VideoModel>();
						ExecutorService threadPool = Executors.newFixedThreadPool(100);
						
						for(String i : inFinal){
							threadPool.execute(new Runnable(){
								@Override
								public void run(){
									VideoModel temp = youtube.getVideoModel(i);
									if(temp != null) {
										//make sure only 1 thread can change videoData at any given time
										synchronized(videoData){
											videoData.add(temp);
										}
									}
								}
							});
						}
						threadPool.shutdown();
						try {
							//wait until all threads are finished with a timeout of 6 hours
							threadPool.awaitTermination(6, TimeUnit.HOURS);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
							System.err.println("-------- TimeOut of ThreadPool --------");
						}
						
						//get the content of the old database.json
						ArrayList<VideoModel> database = new ArrayList<VideoModel>();
						database = JsonAccess.readDatabase();
						database.addAll(videoData);
						
						//write the new database to database.json
						JsonAccess.writeDatabase(database);
						
						//calculate the time used
						long endTime = System.currentTimeMillis();
						long timeDif = endTime-startTime;
						long timeDifMin = timeDif/60000;
						System.out.println("Minutes used for "+fileNameFinal+" : "+timeDifMin + " || Videos added: "+videoData.size());

						GUIAccess.displayTextAndFocus("Video data download complete.", statusMessage, primaryStage);
					}
				}.start();
			}
		});
		menuBox.getChildren().add(button);
	}
	
	
	
	/*
	 * adds a button for the given channelname to the given HBOX with the "download videoID" functionality
	 */
	private void addDownloadVideoIdsToHBox(HBox box, String channelName, Text statusMessage, Stage primaryStage){
		Button button = new Button(channelName);
		button.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				new Thread(){
					public void run(){
						try{
							GUIAccess.displayText("VideoIDs download started.", statusMessage);
							
							YoutubeAccess youtube = new YoutubeAccess(statusMessage);
							ArrayList<String> temp = youtube.getVideoIdsFromChannel(channelName);
							int size = temp.size();
							int max_size = 10000;
							int amount = size/max_size + 1;
							for(int i = 0;i<amount;i++){
								JsonAccess.toJson(temp.subList(i*max_size, ( (i+1)*max_size ) > size ? size : (i+1)*max_size ), ""+channelName+i+".json");				
							}
							System.out.println(channelName + "   " + size);
							
							GUIAccess.displayTextAndFocus("VideoIDs download complete.", statusMessage, primaryStage);
							
						} catch (JsonIOException e1) {
							e1.printStackTrace();
						}
					}
				}.start();
			}
		});
		box.getChildren().add(button);
	}
	

	/*
	 * adds a button that allows certain operations on the database. The given VBox is used for some more inputfields and the given Stage is used as parent-window for a file picker
	 */
	private void addDatabaseOperationButtonToHBox(HBox box, VBox centerBox, Text statusMessage, Stage primaryStage){
		Button button = new Button("Database operations");
		button.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				//open the database
				ArrayList<VideoModel> database = JsonAccess.readDatabase();
				
				Button duplicatesButton = new Button("Eliminate duplicates");
				duplicatesButton.setOnAction(new EventHandler<ActionEvent>(){
					@Override
					public void handle(ActionEvent e){
						//lots of work, so new thread
						new Thread(){
							@Override
							public void run(){
								//inform user we are starting the action
								GUIAccess.displayText("Eliminating duplicates from database is starting.", statusMessage);
								
								ArrayList<VideoModel> old = JsonAccess.readDatabase();
								ArrayList<VideoModel> newOne = new ArrayList<VideoModel>();
								final int totalAmount = old.size();
								int amountDone = 0;
								for(VideoModel i : old){
									String id = i.getVideoId();
									boolean found = false;
									//search new list
									for(VideoModel j : newOne){
										if(id.equals(j.getVideoId())){
											found = true;
											break;
										}
									}
									//if not in new list and certain limits are satisfied, add to new list
									if(!found && i.getComments().size()>=5 && i.getLikes()>=10 && i.getViews()>=100){
										//deep copy for safety reasons
										VideoModel temp = new VideoModel(i.getVideoId(),i.getLikes(),i.getDislikes(),i.getViews(),i.getComments());
										newOne.add(temp);
									}
									//give the user some feedback using the GUI
									final int finalAmountDone = amountDone;
									GUIAccess.displayText("Eliminating duplicates: "+finalAmountDone+"/"+totalAmount, statusMessage);
									amountDone++;
								}
								
								JsonAccess.writeDatabase(newOne);
								//inform user the action is complete
								GUIAccess.displayTextAndFocus("Eliminating duplicates from database is complete.", statusMessage, primaryStage);
								
							}
						}.start();
					}
				});
				centerBox.getChildren().clear();
				centerBox.getChildren().add(duplicatesButton);
				
				
				
				//make a copy of the database with 10% of the videos (for performance reasons)
				Button smallCopyButton = new Button("Make a copy of the first 10% of videos.");
				smallCopyButton.setOnAction(new EventHandler<ActionEvent>(){
					@Override
					public void handle(ActionEvent e){
						ArrayList<VideoModel> smallDatabase = new ArrayList<VideoModel>();
						for(int i=0;i<database.size()/10;i++){
							smallDatabase.add(database.get(i));
						}
						JsonAccess.toJson(smallDatabase, "database_small.json");
					}
				});
				
				//get random 100 videos from database for manual labeling
				Button randomVideosButton = new Button("Get 100 random videos.");
				randomVideosButton.setOnAction(new EventHandler<ActionEvent>(){
					@Override
					public void handle(ActionEvent e){
						for(int i =0;i<100;i++){
							int r = (int) (Math.random()*database.size());
							System.out.println(database.get(r).getVideoId());
							//TODO: write to file
						}
					}
				});
				
				centerBox.getChildren().addAll(smallCopyButton,randomVideosButton);
			
				
				
				//display metadata of the database
				long[] amountOfComments = new long[database.size()];
				long totalAmountOfComments = 0;
				long[] lengthOfComments = new long[database.size()];
				long totalLengthOfComments = 0;
				long[] amountOfWords = new long[database.size()];
				long totalAmountOfWords = 0;
				
				int size = database.size();
				int amountOver95=0;
				int amountOver90=0;
				int amountOver70=0;
				int amountOver50=0;
				int amountOver30=0;
				
				for(int i=0;i<database.size();i++){
					VideoModel vid = database.get(i);
					amountOfComments[i] = vid.getComments().size();
					totalAmountOfComments += vid.getComments().size();
					lengthOfComments[i]=0;
					for(String j : vid.getComments()){
						lengthOfComments[i] += j.length();
						totalLengthOfComments += j.length();
					}
					amountOfWords[i]=amountOfWords(vid.getComments());
					totalAmountOfWords += amountOfWords[i];
					
					if(vid.isOver95()){
						amountOver95++;
					}
					if(vid.isOver90()){
						amountOver90++;
					}
					if(vid.isOver70()){
						amountOver70++;
					}
					if(vid.isOver50()){
						amountOver50++;
					}
					if(vid.isOver30()){
						amountOver30++;
					}
				}
				
				
				Text amountOfVideosText = new Text(""+database.size()+" videos");
				Text totalAmountOfCommentsText = new Text(""+totalAmountOfComments+" total comments");
				Text averageAmountOfCommentsText = new Text("~"+((double)totalAmountOfComments)/((double)database.size())+" comments per video");
				Text totalAmountOfWordsText = new Text("~"+totalAmountOfWords+" total words in al  comments");
				Text averageAmountOfWordsText = new Text("~"+((double)totalAmountOfWords)/((double)totalAmountOfComments)+" words per comment");
				Text totalLengthOfCommentsText = new Text(""+totalLengthOfComments+" total characters in all comments");
				Text averageLengthOfCommentsText = new Text("~"+((double)totalLengthOfComments)/((double)totalAmountOfComments)+" characters per comment");
				centerBox.getChildren().addAll(amountOfVideosText,totalAmountOfCommentsText,averageAmountOfCommentsText,totalAmountOfWordsText,averageAmountOfWordsText,totalLengthOfCommentsText,averageLengthOfCommentsText);
				
				Text amountOver95Text = new Text(""+amountOver95+" of "+size+" videos are over 95% liked.");
				Text amountOver90Text = new Text(""+amountOver90+" of "+size+" videos are over 90% liked.");
				Text amountOver70Text = new Text(""+amountOver70+" of "+size+" videos are over 70% liked.");
				Text amountOver50Text = new Text(""+amountOver50+" of "+size+" videos are over 50% liked.");
				Text amountOver30Text = new Text(""+amountOver30+" of "+size+" videos are over 30% liked.");
				centerBox.getChildren().addAll(amountOver95Text,amountOver90Text,amountOver70Text,amountOver50Text,amountOver30Text);
			}
		});
		box.getChildren().add(button);
	}
	
	/*
	 * very roughly calculates the amount of words in an Arraylist of Strings
	 */
	private long amountOfWords(ArrayList<String> data){
		long amountOfWords = 1; //the last word is usually not followed by whitespace
		for(String i : data){
			boolean lastCharWhiteSpace = false;
			for(int pointer = 0;pointer<i.length()-1;pointer++){
				if(i.substring(pointer, pointer+1).equals(" ") || i.substring(pointer, pointer+1).equals("\n")){
					if(!lastCharWhiteSpace){
						amountOfWords++;	
					}
					lastCharWhiteSpace = true;
				} else {
					lastCharWhiteSpace = false;
				}
			}
		}
		return amountOfWords;
	}
	
	/*
	 * adds buttons to related to machine learning file processing to the given HBox
	 */
	private void addMachineLearningFilePreppingButtonsToHBox(HBox box, VBox centerBox, Text statusMessage, Stage primaryStage){
		arffFileGenerationButton(box,"over95",statusMessage,primaryStage);
		arffFileGenerationButton(box,"over90",statusMessage,primaryStage);
		arffFileGenerationButton(box,"over70",statusMessage,primaryStage);
		arffFileGenerationButton(box,"over50",statusMessage,primaryStage);
		arffFileGenerationButton(box,"over30",statusMessage,primaryStage);
		
		//buttons for likesDislikes and likesViews separately because they are slightly different
		Button likesDislikesButton = new Button("Generate likesDislikes.arff file");
		likesDislikesButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				new Thread(){
					@Override
					public void run(){
						long startTime = System.currentTimeMillis();
						GUIAccess.displayText("Generation of likesDislikes.arff file started", statusMessage);
						FastVector atts = new FastVector();
						atts.addElement(new Attribute("comments", (FastVector) null));
						atts.addElement(new Attribute("likesDislikes"));
						
						Instances data = new Instances("likesDislikes",atts,0);
						data.setClassIndex(data.numAttributes()-1);
						
						//fill with database
						ArrayList<VideoModel> database = JsonAccess.readDatabase();
						for(VideoModel i : database){
							//put all comments in 1 string
							StringBuilder commentsBuilder = new StringBuilder();
							for(String j : i.getComments()){
								commentsBuilder.append(j);
							}
							String comments = commentsBuilder.toString();
							//add values to Instances
							double[] values = new double[data.numAttributes()];
							values[0] = data.attribute(0).addStringValue(comments);
							values[1] = i.getLikesDislikesRatio();
							data.add(new Instance(1.0, values));	//1.0 is the weight. Has no influence if we keep it constant
						}
						Instances output = null;
						try {
							//tokenizer
							NGramTokenizer tokenizer = new NGramTokenizer();
							tokenizer.setNGramMinSize(1);
							tokenizer.setNGramMaxSize(1);
							tokenizer.setDelimiters("[^A-Za-z]"); //everything that isn't a roman letter is a delimiter

							//filter
							StringToWordVector bagOfWordMaker = new StringToWordVector();
							bagOfWordMaker.setInputFormat(data);
							bagOfWordMaker.setWordsToKeep(3500);	//TODO: play with words to keep
							bagOfWordMaker.setLowerCaseTokens(true);
							bagOfWordMaker.setDoNotOperateOnPerClassBasis(true);
							bagOfWordMaker.setUseStoplist(true);	
							//The following option gives a nice database driver error.
							//Plot twist: This code doesn't invoke a database and runs just fine.
							//Real problem with using a stemmer: certain stopwords get stemmed and are not getting removed anymore (eg.: this -> thi)
							//'Solution': We don't care about it as TF-IDF has reduced the significance of common used words by a decent amount.
							bagOfWordMaker.setStemmer(new SnowballStemmer());	//aka Porter-stemmer
							bagOfWordMaker.setTokenizer(tokenizer);
							//the next 2 options together activate TF-IDF
							bagOfWordMaker.setTFTransform(true); 	//use log of term frequency (TF)
							bagOfWordMaker.setIDFTransform(true);	//use inverted document frequency (IDF) 


							//apply
							GUIAccess.displayText("Starting filtering of data. This will take a long time (up to 2 hours).", statusMessage);
							output = Filter.useFilter(data, bagOfWordMaker);
						} catch (Exception e2) {
							e2.printStackTrace();
						}


						//save file
						ArffAccess.write(output, "likesDislikes.arff");
						long endTime = System.currentTimeMillis();
						long seconds = (endTime-startTime)/1000;
						long minutes = seconds/60;
						System.out.println(minutes + " minutes");
						GUIAccess.displayTextAndFocus("Generation of likesDislikes.arff file completed.", statusMessage, primaryStage);


					}
				}.start();
			}
		});
		
		//idem as the one above likesDislikes replaced with likesViews
		Button likesViewsButton = new Button("Generate likesViews.arff file");
		likesViewsButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				new Thread(){
					@Override
					public void run(){
						long startTime = System.currentTimeMillis();
						GUIAccess.displayText("Generation of likesViews.arff file started", statusMessage);
						FastVector atts = new FastVector();
						atts.addElement(new Attribute("comments", (FastVector) null));
						atts.addElement(new Attribute("likesViews"));
						
						Instances data = new Instances("likesViews",atts,0);
						data.setClassIndex(data.numAttributes()-1);
						
						//fill with database
						ArrayList<VideoModel> database = JsonAccess.readDatabase();
						for(VideoModel i : database){
							//put all comments in 1 string
							StringBuilder commentsBuilder = new StringBuilder();
							for(String j : i.getComments()){
								commentsBuilder.append(j);
							}
							String comments = commentsBuilder.toString();
							//add values to Instances
							double[] values = new double[data.numAttributes()];
							values[0] = data.attribute(0).addStringValue(comments);
							values[1] = i.getLikesViewsRatio();
							data.add(new Instance(1.0, values));	//1.0 is the weight. Has no influence if we keep it constant
						}	
						Instances output = null;
						try {
							//tokenizer
							NGramTokenizer tokenizer = new NGramTokenizer();
							tokenizer.setNGramMinSize(1);
							tokenizer.setNGramMaxSize(1);
							tokenizer.setDelimiters("[^A-Za-z]"); //everything that isn't a roman letter is a delimiter

							//filter
							StringToWordVector bagOfWordMaker = new StringToWordVector();
							bagOfWordMaker.setInputFormat(data);
							bagOfWordMaker.setWordsToKeep(1000); //TODO: copy words to keep from above
							bagOfWordMaker.setLowerCaseTokens(true);
							bagOfWordMaker.setDoNotOperateOnPerClassBasis(true);
							bagOfWordMaker.setUseStoplist(true);	
							//The following option gives a nice database driver error.
							//Plot twist: This code doesn't invoke a database and runs just fine.
							//Real problem with using a stemmer: certain stopwords get stemmed and are not getting removed anymore (eg.: this -> thi)
							//'Solution': We don't care about it as TF-IDF has reduced the significance of common used words by a decent amount.
							bagOfWordMaker.setStemmer(new SnowballStemmer());	//aka Porter-stemmer
							bagOfWordMaker.setTokenizer(tokenizer);
							//the next 2 options together activate TF-IDF
							bagOfWordMaker.setTFTransform(true); 	//use log of term frequency (TF)
							bagOfWordMaker.setIDFTransform(true);	//use inverted document frequency (IDF) 


							//apply
							GUIAccess.displayText("Starting filtering of data. This will take a long time (up to 2 hours).", statusMessage);
							output = Filter.useFilter(data, bagOfWordMaker);
						} catch (Exception e2) {
							e2.printStackTrace();
						}


						//save file
						ArffAccess.write(output, "likesViews.arff");
						long endTime = System.currentTimeMillis();
						long seconds = (endTime-startTime)/1000;
						long minutes = seconds/60;
						System.out.println(minutes + " minutes");
						GUIAccess.displayTextAndFocus("Generation of likesViews.arff file completed.", statusMessage, primaryStage);


					}
				}.start();
			}
		});
		box.getChildren().addAll(likesDislikesButton,likesViewsButton);
	}
	
	/*
	 * adds a single button for <arg description>.arff file generation to the given HBox
	 */
	private void arffFileGenerationButton(HBox box,String description, Text statusMessage, Stage primaryStage){
		Button button = new Button("Generate "+description+".arff file");
		button.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				new Thread(){
					@Override
					public void run(){
						long startTime = System.currentTimeMillis();
						GUIAccess.displayText("Generation of "+description+".arff file started", statusMessage);
						//info about how the file looks like
						FastVector atts = new FastVector();
						atts.addElement(new Attribute("comments", (FastVector) null));
						FastVector attVals = new FastVector();
						attVals.addElement("true");
						attVals.addElement("false");
						atts.addElement(new Attribute(description, attVals));


						Instances data = new Instances(description,atts,0);
						data.setClassIndex(data.numAttributes()-1);	//the class is the last attribute

						//fill with database
						ArrayList<VideoModel> database = JsonAccess.readDatabase();
						for(VideoModel i : database){
							//put all comments in 1 string
							StringBuilder commentsBuilder = new StringBuilder();
							for(String j : i.getComments()){
								commentsBuilder.append(j);
							}
							String comments = commentsBuilder.toString();
							//add values to Instances
							double[] values = new double[data.numAttributes()];
							values[0] = data.attribute(0).addStringValue(comments);
							if(descriptionToBoolean(description,i)){
								values[1] = attVals.indexOf("true");
							} else {
								values[1] = attVals.indexOf("false");
							}

							data.add(new Instance(1.0, values));	//1.0 is the weight. Has no influence if we keep it constant
						}

						Instances output = null;
						try {
							//tokenizer
							NGramTokenizer tokenizer = new NGramTokenizer();
							tokenizer.setNGramMinSize(1);
							tokenizer.setNGramMaxSize(1);
							tokenizer.setDelimiters("[^A-Za-z]"); //everything that isn't a roman letter is a delimiter

							//filter
							StringToWordVector bagOfWordMaker = new StringToWordVector();
							bagOfWordMaker.setInputFormat(data);
							bagOfWordMaker.setWordsToKeep(1000000);
							bagOfWordMaker.setLowerCaseTokens(true);
							bagOfWordMaker.setDoNotOperateOnPerClassBasis(true);
							bagOfWordMaker.setUseStoplist(true);	
							//The following option gives a nice database driver error.
							//Plot twist: This code doesn't invoke a database and runs just fine.
							//Real problem with using a stemmer: certain stopwords get stemmed and are not getting removed anymore (eg.: this -> thi)
							//'Solution': We don't care about it as TF-IDF has reduced the significance of common used words by a decent amount.
							bagOfWordMaker.setStemmer(new SnowballStemmer());	//aka Porter-stemmer
							bagOfWordMaker.setTokenizer(tokenizer);
							//the next 2 options together activate TF-IDF
							bagOfWordMaker.setTFTransform(true); 	//use log of term frequency (TF)
							bagOfWordMaker.setIDFTransform(true);	//use inverted document frequency (IDF) 


							//apply
							GUIAccess.displayText("Starting filtering of data. This will take a long time (up to 2 hours).", statusMessage);
							output = Filter.useFilter(data, bagOfWordMaker);
						} catch (Exception e2) {
							e2.printStackTrace();
						}


						//save file
						ArffAccess.write(output, description+".arff");
						long endTime = System.currentTimeMillis();
						long seconds = (endTime-startTime)/1000;
						long minutes = seconds/60;
						System.out.println(minutes + " minutes");
						GUIAccess.displayTextAndFocus("Generation of "+description+".arff file completed.", statusMessage, primaryStage);
					}
				}.start();
			}
		});
		box.getChildren().add(button);
	}
	
	//helper function for the one above
	private boolean descriptionToBoolean(String description, VideoModel vid){
		if(description.equals("over95")){
			return vid.isOver95();
		}
		if(description.equals("over90")){
			return vid.isOver90();
		}
		if(description.equals("over70")){
			return vid.isOver70();
		}
		if(description.equals("over50")){
			return vid.isOver50();
		}
		if(description.equals("over30")){
			return vid.isOver30();
		}
		return false;
	}
	
	/*
	 * adds a button related to machine learning to the given HBox
	 */
	public void addBinairyMachineLearningButtonToHBox(HBox box, VBox centerBox, Text statusMessage, Stage primaryStage){
		Button button = new Button("Start binairy machine learning");
		button.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File("resources/arff"));
				fileChooser.setTitle("Please select the .arff file to process and press 'open'");
				File databaseFile = fileChooser.showOpenDialog(primaryStage);
				String fileName = databaseFile.getName();	//this will throw an unhandled NullPointerException when 'cancel' is chosen.
				
				new Thread(){
					@Override
					public void run(){
						GUIAccess.displayText("Machine learning started.", statusMessage);
						long beginTime = System.currentTimeMillis();
						//read data
						Instances data = ArffAccess.read(fileName);	
						data.setClassIndex(0);

						//train and cross-validate classifier
						//J48 classifier = new J48();	//TODO: choose a classifier
						//classifier.setUnpruned(true);
						
						//NaiveBayes classifier = new NaiveBayes();
						
						RandomForest classifier = new RandomForest();
						
						
						try {
							Evaluation eval = new Evaluation(data);
							StringBuffer predictions = new StringBuffer();
							//train tree with output for a 10 fold crossValidation, using random seed generator Random(1)
							//and writing predictions to the StringBuffer predictions
							eval.crossValidateModel(classifier, data, 10, new Random(1),predictions,new Range(), true);	
							EvaluationResultsAccess.writeResults(eval, fileName+".txt");
							//write the predictions to a file
							Writer writer2 = new FileWriter("resources/results/"+fileName+"_predictions.txt");			
							writer2.write(predictions.toString());
							writer2.flush();
							writer2.close();
							
						} catch (Exception e) {
							e.printStackTrace();
						}
						long endTime = System.currentTimeMillis();
						long seconds = (endTime-beginTime)/1000;
						long minutes = seconds/60;
						System.out.println(minutes + " minutes");
						GUIAccess.displayTextAndFocus("MachineLearning of "+fileName+" completed.", statusMessage, primaryStage);	
					}
				}.start();
			}
		});	
		box.getChildren().add(button);
	}
	
	private void addLinearMachineLearningButtonToHBox(HBox box, VBox centerBox, Text statusMessage, Stage primaryStage){
		Button button = new Button("Start linear machine learning");
		button.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File("resources/arff"));
				fileChooser.setTitle("Please select the .arff file to process and press 'open'");
				File databaseFile = fileChooser.showOpenDialog(primaryStage);
				String fileName = databaseFile.getName();	//this will throw an unhandled NullPointerException when 'cancel' is chosen.
				
				new Thread(){
					@Override
					public void run(){
						GUIAccess.displayText("Machine learning started.", statusMessage);
						long beginTime = System.currentTimeMillis();
						//read data
						Instances data = ArffAccess.read(fileName);	
						data.setClassIndex(0);

						//train and cross-validate classifier
						LinearRegression classifier = new LinearRegression();
						
						
						try {
							Evaluation eval = new Evaluation(data);
							StringBuffer predictions = new StringBuffer();
							//train tree with output for a 10 fold crossValidation, using random seed generator Random(1)
							//and writing predictions to the StringBuffer predictions
							eval.crossValidateModel(classifier, data, 10, new Random(1),predictions,new Range(), true);	
							EvaluationResultsAccess.writeLinearResults(eval, fileName+".txt");
							//write the predictions to a file
							Writer writer2 = new FileWriter("resources/results/"+fileName+"_predictions.txt");			
							writer2.write(predictions.toString());
							writer2.flush();
							writer2.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
						long endTime = System.currentTimeMillis();
						long seconds = (endTime-beginTime)/1000;
						long minutes = seconds/60;
						System.out.println(minutes + " minutes");
						GUIAccess.displayTextAndFocus("MachineLearning of "+fileName+" completed.", statusMessage, primaryStage);	
					}
				}.start();
			}
		});	
		box.getChildren().add(button);
	}
}
