package io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.text.Text;
import model.VideoModel;
import application.Auth;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentListResponse;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

public class YoutubeAccess {
	//hardcoding the API_KEY is not good practice, but it's still the easiest option
	private final String API_KEY = "AIzaSyBB16aIP-SlnWAsD3JFCI1aKBRBbdWF0sc";
	private YouTube youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
        public void initialize(HttpRequest request) throws IOException {
        }
    }).setApplicationName("Data Science Course Project").build();
	private Text statusText;
	
	public YoutubeAccess(Text statusText){
		this.statusText = statusText;
	}
	
	public ArrayList<String> getVideoIdsFromChannel(String channelName){
		ArrayList<String> result = new ArrayList<String>();
		try {
			YouTube.Channels.List search = youtube.channels().list("id,contentDetails");
			
			search.setKey(API_KEY);			
			search.setForUsername(channelName);	
			
			ChannelListResponse searchResponse = search.execute();
			List<Channel> results = searchResponse.getItems();
			String uploadsID = results.get(0).getContentDetails().getRelatedPlaylists().getUploads();

			//get videoIDs in playlist
			//get first page
			YouTube.PlaylistItems.List playlistitems = youtube.playlistItems().list("id,contentDetails");
			playlistitems.setKey(API_KEY);
			playlistitems.setPlaylistId(uploadsID);
			playlistitems.setMaxResults((long) 50);
			
			PlaylistItemListResponse playlistResponse = playlistitems.execute();
			List<PlaylistItem> items = playlistResponse.getItems();
			
			for(PlaylistItem i : items){
				result.add(i.getContentDetails().getVideoId());
			}
			
			int i2=0;
			//send more requests using the nextPageToken until there are no more items to get
			while(playlistResponse.getNextPageToken() != null){
				final int i2d=i2;
				Platform.runLater(new Runnable(){
					@Override
					public void run() {
						statusText.setText("Getting videoIDs for "+channelName+": page "+i2d);
					}
            	});
				i2++;
				String token = playlistResponse.getNextPageToken();
				playlistitems.setPageToken(token);
				playlistResponse = playlistitems.execute();
				items = playlistResponse.getItems();
				for(PlaylistItem i : items){
					result.add(i.getContentDetails().getVideoId());
				}
			}
			
		} catch(Exception e){
			//good practice: check which exception it is and handle according to that
			e.printStackTrace();
		}
		
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				statusText.setText("Done getting videos for "+channelName);
			}
    	});
		
		return result;
	}
	
	public VideoModel getVideoModel(String videoID){
		VideoModel result = null;
		try {
			int likes;
			int dislikes;
			int views;
			ArrayList<String> comments = new ArrayList<String>();
			
			//get global data of the video
			YouTube.Videos.List vidSearch = youtube.videos().list("id,statistics");
			vidSearch.setKey(API_KEY);
			vidSearch.setId(videoID);
			
			VideoListResponse vidResponse = vidSearch.execute();
			List<Video> vidList = vidResponse.getItems();
			Video rawVid = vidList.get(0);
			
			likes = rawVid.getStatistics().getLikeCount().intValueExact();
			dislikes = rawVid.getStatistics().getDislikeCount().intValueExact();
			views = rawVid.getStatistics().getViewCount().intValueExact();
			

			//get top level comments
			YouTube.CommentThreads.List commentsListSearch = youtube.commentThreads().list("id,snippet");
			commentsListSearch.setVideoId(videoID);
			commentsListSearch.setTextFormat("plainText");
			commentsListSearch.setKey(API_KEY);
			commentsListSearch.setMaxResults((long) 50);
			
			CommentThreadListResponse videoCommentsListResponse = commentsListSearch.execute();
			
            List<CommentThread> videoComments = videoCommentsListResponse.getItems();
            
            ArrayList<String> topLevelCommentsIDs = new ArrayList<String>();
            
            for(CommentThread videoComment : videoComments){
            	comments.add(videoComment.getSnippet().getTopLevelComment().getSnippet().getTextDisplay());
            	topLevelCommentsIDs.add(videoComment.getId());
            }
            
            //send more requests using the nextPageToken until there are no more items to get
            int i2=0;
            while(videoCommentsListResponse.getNextPageToken() != null){
				final int i2d=i2;
				Platform.runLater(new Runnable(){
					@Override
					public void run() {
						statusText.setText("Getting top level comments for "+videoID+": page"+i2d);
					}
            	});
				i2++;
				String token = videoCommentsListResponse.getNextPageToken();
				commentsListSearch.setPageToken(token);
				videoCommentsListResponse = commentsListSearch.execute();
				videoComments = videoCommentsListResponse.getItems();
				for(CommentThread i : videoComments){
					comments.add(i.getSnippet().getTopLevelComment().getSnippet().getTextDisplay());
	            	topLevelCommentsIDs.add(i.getId());
				}
			}
            
            //get comments for each top level comment
            for(String parentCommentID : topLevelCommentsIDs){
            	Platform.runLater(new Runnable(){
					@Override
					public void run() {
						statusText.setText("Getting child comments for "+parentCommentID);
					}
            	});
            	
            	YouTube.Comments.List commentsListChildSearch = youtube.comments().list("snippet");
            	commentsListChildSearch.setParentId(parentCommentID);
                commentsListChildSearch.setTextFormat("plainText");
    			commentsListChildSearch.setKey(API_KEY);
    			commentsListChildSearch.setMaxResults((long) 50);
    			
    			CommentListResponse videoCommentsChild = commentsListChildSearch.execute();
    			List<Comment> videoCommentsChildList = videoCommentsChild.getItems();
    			
    			for(Comment i : videoCommentsChildList){
    				comments.add(i.getSnippet().getTextDisplay());
    			}
    			
    			//send more requests using the nextPageToken until there are no more items to get
                int i3=0;
                while(videoCommentsChild.getNextPageToken() != null){
                	final int i3d=i3;
                	Platform.runLater(new Runnable(){
						@Override
						public void run() {
							statusText.setText("Getting child comments for "+parentCommentID+": page"+i3d);
						}
                	});
    				
    				i3++;
    				String token = videoCommentsChild.getNextPageToken();
    				commentsListChildSearch.setPageToken(token);
    				videoCommentsChild = commentsListChildSearch.execute();
    				videoCommentsChildList = videoCommentsChild.getItems();
    				for(Comment i : videoCommentsChildList){
    					comments.add(i.getSnippet().getTextDisplay());
    				}
    			}
            }
            
            
            //put all this data in the model
            result = new VideoModel(videoID, likes, dislikes, views, comments);

		} catch(Exception e){
			e.printStackTrace();
		}
		
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				statusText.setText("Done getting comments for "+videoID);
			}
    	});
		
		return result;
	}
	
	
}
