package application;
	
import io.YoutubeAccess;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import model.VideoModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			
			/*
			final String API_KEY = "AIzaSyBB16aIP-SlnWAsD3JFCI1aKBRBbdWF0sc";
			final String videoID = "fmI_Ndrxy14";
			
			
			YouTube youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
	            public void initialize(HttpRequest request) throws IOException {
	            }
	        }).setApplicationName("Data Science Course Project").build();
			
			
			//get all videos from 1 channel
			
			YouTube.Channels.List search = youtube.channels().list("id,snippet,contentDetails");
			
			search.setKey(API_KEY);			
			search.setForUsername("TEDtalksDirector");		//2008
			//search.setForUsername("TEDxTalks");				//63411
			
			ChannelListResponse searchResponse = search.execute();
			List<Channel> results = searchResponse.getItems();
			String uploadsID = results.get(0).getContentDetails().getRelatedPlaylists().getUploads();

			//get videoIDs in playlist
			//get first page
			YouTube.PlaylistItems.List playlistitems = youtube.playlistItems().list("id,snippet,contentDetails");
			playlistitems.setKey(API_KEY);
			playlistitems.setPlaylistId(uploadsID);
			playlistitems.setMaxResults((long) 50);
			
			List<String> videoIDs = new ArrayList<String>();
			
			PlaylistItemListResponse playlistResponse = playlistitems.execute();
			List<PlaylistItem> items = playlistResponse.getItems();
			
			for(PlaylistItem i : items){
				videoIDs.add(i.getContentDetails().getVideoId());
			}
			
			int i2=0;
			//loop for other pages
			while(playlistResponse.getNextPageToken() != null){
				System.out.println(i2);
				i2++;
				String token = playlistResponse.getNextPageToken();
				playlistitems.setPageToken(token);
				playlistResponse = playlistitems.execute();
				items = playlistResponse.getItems();
				for(PlaylistItem i : items){
					videoIDs.add(i.getContentDetails().getVideoId());
				}
			}
			
			System.out.println(videoIDs.size());
			
			
			*/
			
			
			
			
			
			//get comments on 1 video
			/*
			YouTube.Videos.List vidSearch = youtube.videos().list("id,snippet,contentDetails,statistics");
			vidSearch.setKey(API_KEY);
			vidSearch.setId(videoID);
			
			VideoListResponse vidResponse = vidSearch.execute();
			List<Video> vidList = vidResponse.getItems();
			Video rawVid = vidList.get(0);
			
			Text filler = new Text(rawVid.getSnippet().getTitle() + "\n\n\n" + rawVid.getSnippet().getDescription());
			
			

			
			YouTube.CommentThreads.List commentsListSearch = youtube.commentThreads().list("snippet");
			commentsListSearch.setVideoId(videoID);
			commentsListSearch.setTextFormat("plainText");
			commentsListSearch.setKey(API_KEY);
			commentsListSearch.setMaxResults((long) 50);
			
			CommentThreadListResponse videoCommentsListResponse = commentsListSearch.execute();
			
			//TODO: use nextpage token to get all results
			
            List<CommentThread> videoComments = videoCommentsListResponse.getItems();
            
            if(videoComments.isEmpty()){
            	System.out.println("No comments found");
            } else {
            	for(CommentThread videoComment : videoComments){
            		System.out.println(videoComment.getSnippet().getTopLevelComment().getSnippet().getTextDisplay());
            		
            	}
            }
            
            System.out.println("\n\n\n\n");
            
            //TODO: get comments for each parent
            //TODO: use nextpage token to get all results
            
            YouTube.Comments.List commentsListChildSearch = youtube.comments().list("snippet");
            commentsListChildSearch.setParentId(videoComments.get(0).getId());
            commentsListChildSearch.setTextFormat("plainText");
			commentsListChildSearch.setKey(API_KEY);
			commentsListChildSearch.setMaxResults((long) 50);
			
			CommentListResponse videoCommentsChild = commentsListChildSearch.execute();
			List<Comment> blabla = videoCommentsChild.getItems();
			
			if(blabla.isEmpty()){
				System.out.println("No comments found");
			} else {
				for(Comment i : blabla){
					System.out.println(i.getSnippet().getTextDisplay());
				}
			}
			*/
			
			//standard javaFX initalization
			BorderPane root = new BorderPane();
			Scene scene = new Scene(root,1600,820);	//400, 400 default               1600,820 fullscreen kinda
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
			
			//statusMessage at bottom
			Text statusMessage = new Text();
			root.setBottom(statusMessage);
			
			//menubars at top
			VBox top = new VBox();
			root.setTop(top);
			setUpMenuBars(top,statusMessage);
			
			/*
			
			new Thread(){
				@Override
				public void run(){
					//------------- test code
					YoutubeAccess youtube = new YoutubeAccess(statusMessage);
					
					ArrayList<String> res1 = youtube.getVideoIdsFromChannel("TEDEducation");
					System.out.println(res1.size());
					
					
					ArrayList<VideoModel> res3 = new ArrayList<VideoModel>();
					for(String i : res1){
						VideoModel res2 = youtube.getVideoModel(i);
						res3.add(res2);
					}
					
					
					 try {
						Writer writer = new FileWriter("Output.json");
						Gson gson = new GsonBuilder().create();
						gson.toJson(res3, writer);
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					//---------------- end test code
				}
			}.start();
			
			*/
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	
	/*
	 * sets up the needed menubars in the given Vbox
	 */
	private void setUpMenuBars(VBox top, Text statusMessage){
		//menubar to download videoIDs
		HBox downloadVideoIDButtons = new HBox();
		top.getChildren().add(downloadVideoIDButtons);
		
		Text downloadVideoIDDescription = new Text("Download video IDs of: ");
		downloadVideoIDButtons.getChildren().add(downloadVideoIDDescription);
		
		
		
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDtalksDirector",statusMessage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDxTalks",statusMessage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDEducation",statusMessage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDxYouth",statusMessage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDFellowsTalks",statusMessage);
		addDownloadVideoIdsToHBox(downloadVideoIDButtons,"TEDPartners",statusMessage);
		
		
	}
	
	/*
	 * adds a button for the given given channelname to the given HBOX with the "download videoID" functionality
	 */
	private void addDownloadVideoIdsToHBox(HBox box, String channelName, Text statusMessage){
		Button button = new Button(channelName);
		button.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){
				new Thread(){
					public void run(){
						try{
							YoutubeAccess youtube = new YoutubeAccess(statusMessage);
							ArrayList<String> temp = youtube.getVideoIdsFromChannel(channelName);
							int size = temp.size();
							int amount = size/5000 + 1;
							for(int i = 0;i<amount;i++){
								Writer writer = new FileWriter("resources/json/"+channelName+i+".json");
								Gson gson = new GsonBuilder().create();
								gson.toJson(temp.subList(i*5000, ( (i+1)*5000 ) > size ? size : (i+1)*5000 ), writer);
								writer.close();						
							}
							System.out.println(channelName + "   " + size);
						} catch (JsonIOException | IOException e1) {
							e1.printStackTrace();
						}
					}
				}.start();
			}
		});
		box.getChildren().add(button);
	}
}
