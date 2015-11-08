package model;

import java.util.ArrayList;

public class VideoModel {
	//values are used when writing to JSON using the GSON library
	private String videoId;
	private int likes;
	private int dislikes;
	private boolean over95 = false;
	private boolean over90 = false;
	private boolean over70 = false;
	private boolean over50 = false;
	private boolean over30 = false;
	private int views;
	ArrayList<String> comments;
	
	public VideoModel(String videoId, int likes, int dislikes, int views, ArrayList<String> comments){
		this.videoId = videoId;
		this.likes = likes;
		this.dislikes = dislikes;
		this.views = views;
		this.comments = comments;
		double totalRatings = (double) likes + (double) dislikes;
		if( ( ( (double)likes)/totalRatings) > 0.95 ){
			over95 = true;
		}
		if( ( ( (double)likes)/totalRatings) > 0.9 ){
			over90 = true;
		}
		if( ( ( (double)likes)/totalRatings) > 0.7 ){
			over70 = true;
		}
		if( ( ( (double)likes)/totalRatings) > 0.5 ){
			over50 = true;
		}
		if( ( ( (double)likes)/totalRatings) > 0.3 ){
			over30 = true;
		}
	}
	
	public String getVideoId(){
		return videoId;
	}
	
	public int getLikes(){
		return likes;
	}
	
	public int getDislikes(){
		return dislikes;
	}
	
	public int getViews(){
		return views;
	}
	
	public ArrayList<String> getComments(){
		return comments;
	}
}
