package model;

import java.util.ArrayList;

public class VideoModel {
	private String videoId;
	private int likes;
	private int dislikes;
	private double likesDislikesRatio;	//number of likes divided by total number of ratings
	private double likesViewsRatio;
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
		this.likesViewsRatio = ((double) likes)/((double) views);
		double totalRatings = (double) likes + (double) dislikes;
		if(totalRatings == 0){	//division by zero check
			likesDislikesRatio = 0;
		} else {
			likesDislikesRatio = ((double) likes)/totalRatings;
		}
		if( likesDislikesRatio > 0.95 ){
			over95 = true;
		}
		if( likesDislikesRatio > 0.9 ){
			over90 = true;
		}
		if( likesDislikesRatio > 0.7 ){
			over70 = true;
		}
		if( likesDislikesRatio > 0.5 ){
			over50 = true;
		}
		if( likesDislikesRatio > 0.3 ){
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
	
	public double getLikesDislikesRatio(){
		return likesDislikesRatio;
	}
	
	public double getLikesViewsRatio(){
		return likesViewsRatio;
	}
	
	public ArrayList<String> getComments(){
		return comments;
	}
	
	public boolean isOver95(){
		return over95;
	}
	
	public boolean isOver90(){
		return over90;
	}
	
	public boolean isOver70(){
		return over70;
	}
	
	public boolean isOver50(){
		return over50;
	}
	
	public boolean isOver30(){
		return over30;
	}
}
