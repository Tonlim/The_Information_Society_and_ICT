package application;
	
import io.ArffAccess;
import io.EvaluationResultsAccess;
import io.JsonAccess;
import io.YoutubeAccess;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import model.VideoModel;
import javafx.application.Application;
import javafx.application.Platform;
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

import com.google.gson.JsonIOException;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.stemmers.SnowballStemmer;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;


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
		addMachineLearningButtonToHBox(machineLearningBar,center,statusMessage,primaryStage);
		
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
						Platform.runLater(new Runnable(){
							@Override
							public void run(){
								statusMessage.setText("Video data download started.");
							}
						});
						
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
						System.out.println("Minutes used for "+fileNameFinal+" : "+timeDifMin + " || Entries added: "+videoData.size());
						
						Platform.runLater(new Runnable(){
							@Override
							public void run(){
								statusMessage.setText("Video data download complete.");
								primaryStage.requestFocus();
							}
						});
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
							Platform.runLater(new Runnable(){
								@Override
								public void run(){
									statusMessage.setText("VideoIDs download started.");
								}
							});
							
							YoutubeAccess youtube = new YoutubeAccess(statusMessage);
							ArrayList<String> temp = youtube.getVideoIdsFromChannel(channelName);
							int size = temp.size();
							int max_size = 10000;
							int amount = size/max_size + 1;
							for(int i = 0;i<amount;i++){
								JsonAccess.toJson(temp.subList(i*max_size, ( (i+1)*max_size ) > size ? size : (i+1)*max_size ), ""+channelName+i+".json");				
							}
							System.out.println(channelName + "   " + size);
							
							Platform.runLater(new Runnable(){
								@Override
								public void run(){
									statusMessage.setText("VideoIDs download complete.");
									primaryStage.requestFocus();
								}
							});
							
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
				Button duplicatesButton = new Button("Eliminate duplicates");
				duplicatesButton.setOnAction(new EventHandler<ActionEvent>(){
					@Override
					public void handle(ActionEvent e){
						//lots of work, so new thread
						new Thread(){
							@Override
							public void run(){
								//inform user we are starting the action
								Platform.runLater(new Runnable(){
									@Override
									public void run(){
										statusMessage.setText("Eliminating duplicates from database is starting.");
									}
								});
								
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
									//if not in new list, add to new list
									if(!found){
										//deep copy for safety reasons
										VideoModel temp = new VideoModel(i.getVideoId(),i.getLikes(),i.getDislikes(),i.getViews(),i.getComments());
										newOne.add(temp);
									}
									//give the user some feedback using the GUI
									final int finalAmountDone = amountDone;
									Platform.runLater(new Runnable(){
										@Override
										public void run(){
											statusMessage.setText("Eliminating duplicates: "+finalAmountDone+"/"+totalAmount);
										}
									});
									amountDone++;
								}
								
								JsonAccess.writeDatabase(newOne);
								//inform user the action is complete
								Platform.runLater(new Runnable(){
									@Override
									public void run(){
										statusMessage.setText("Eliminating duplicates from database is complete.");
										primaryStage.requestFocus();
									}
								});
								
							}
						}.start();
					}
				});
				centerBox.getChildren().clear();
				centerBox.getChildren().add(duplicatesButton);
				
				//TODO: add other database data in Text nodes
				//ex.: amount of vid's, amount of comments, avg comments, amount of words in comments, ...
			}
		});
		box.getChildren().add(button);
	}
	
	private void addMachineLearningFilePreppingButtonsToHBox(HBox box, VBox centerBox, Text statusMessage, Stage primaryStage){
		arffFileGenerationButton(box,"over95",statusMessage,primaryStage);
		arffFileGenerationButton(box,"over90",statusMessage,primaryStage);
		arffFileGenerationButton(box,"over70",statusMessage,primaryStage);
		arffFileGenerationButton(box,"over50",statusMessage,primaryStage);
		arffFileGenerationButton(box,"over30",statusMessage,primaryStage);
		
	}
	
	private void arffFileGenerationButton(HBox box,String description, Text statusMessage, Stage primaryStage){
		Button button = new Button("Generate "+description+".arff file");
		button.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				new Thread(){
					@Override
					public void run(){
						Platform.runLater(new Runnable(){
							public void run(){
								statusMessage.setText("Generation of "+description+".arff file started");
							}
						});
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
							bagOfWordMaker.setUseStoplist(true);	//TODO: play with enabling/disabling this
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
							Platform.runLater(new Runnable(){
								public void run(){
									statusMessage.setText("Starting filtering of data. This will take a long time (up to 2 hours).");
								}
							});
							output = Filter.useFilter(data, bagOfWordMaker);
						} catch (Exception e2) {
							e2.printStackTrace();
						}


						//save file
						ArffAccess.write(output, description+".arff");
						Platform.runLater(new Runnable(){
							public void run(){
								statusMessage.setText("Generation of "+description+".arff file completed.");
								primaryStage.requestFocus();
							}
						});
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
	
	public void addMachineLearningButtonToHBox(HBox box, VBox centerBox, Text statusMessage, Stage primaryStage){
		Button button = new Button("Start machine learning");
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
						Platform.runLater(new Runnable(){
							public void run(){
								statusMessage.setText("Machine learning started.");
							}
						});		
						
						//read data
						Instances data = ArffAccess.read(fileName);	
						data.setClassIndex(0);

						//train and cross-validate classifier
						//J48 classifier = new J48();	//TODO: choose a classifier
						//classifier.setUnpruned(true);
						
						NaiveBayes classifier = new NaiveBayes();
						
						try {
							Evaluation eval = new Evaluation(data);
							eval.crossValidateModel(classifier, data, 10, new Random(1));	//train tree with output for a 10 fold crossValidation, using random seed generator Random(1)
							EvaluationResultsAccess.writeResults(eval, fileName+".txt");
						} catch (Exception e) {
							e.printStackTrace();
						}
						Platform.runLater(new Runnable(){
							public void run(){
								statusMessage.setText("MachineLearning of "+fileName+" completed.");
								primaryStage.requestFocus();
							}
						});		
					}
				}.start();
			}
		});	
		box.getChildren().add(button);
	}
}
