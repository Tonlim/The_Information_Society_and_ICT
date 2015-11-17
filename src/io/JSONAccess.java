package io;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import model.VideoModel;

//This class has to use raw types as the GSON library works with it
//The unchecked warning is taken into account, but isn't a problem as we use data we created ourselves
@SuppressWarnings({"rawtypes","unchecked"})
public class JsonAccess {
	//Gson object so we don't have to make a new one every time
	private static Gson gson = new GsonBuilder().create();
	
	
	/*
	 * Writes the given data to the given .json file (.json must be included in the filename)
	 */
	public static void toJson(Object data, String filename){
		try {
			Writer writer = new FileWriter("resources/json/"+filename);
			gson.toJson(data,writer);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Writes the given data to "database.json"
	 */
	public static void writeDatabase(Object data){
		toJson(data, "database.json");
	}
	
	/*
	 * Returns an ArrayList of Objects that is read from the given .json file (.json must be included in the filename)
	 */
	public static ArrayList fromJson(String filename){
		ArrayList result = null;
		try{
			Reader reader = new FileReader("resources/json/"+filename);
			result = gson.fromJson(reader, ArrayList.class);
			reader.close();
		} catch(IOException e){
			e.printStackTrace();
		}
		return result;
	}
	
	
	/*
	 * Returns an ArrayList of VideoModels that is read from the given .json file (.json must be included in the filename)
	 */
	public static ArrayList<VideoModel> readDatabase(String filename){
		ArrayList<VideoModel> result = new ArrayList<VideoModel>();
		ArrayList rawData = fromJson(filename);
		for(Object i : rawData){
			LinkedHashMap hashData = (LinkedHashMap) i;
			VideoModel modelData = new VideoModel((String)hashData.get("videoId"), ((Double) hashData.get("likes")).intValue(), ((Double) hashData.get("dislikes")).intValue(), ((Double) hashData.get("views")).intValue(), (ArrayList<String>) hashData.get("comments"));
			result.add(modelData);
		}
		if(result.isEmpty()){
			return null;
		}
		return result;
	}
	
	/*
	 * Returns an ArrayList of VideoModels that is read from "database.json".
	 */
	public static ArrayList<VideoModel> readDatabase(){
		return readDatabase("database.json");
	}
	
	/*
	 * Returns an ArrayList of Strings that is read from the given .json file (.json must be included in the filename)
	 */
	public static ArrayList<String> readVideoIds(String filename){
		ArrayList<String> result = new ArrayList<String>();
		ArrayList rawData = fromJson(filename);
		for(Object i : rawData){
			String data = (String) i;
			result.add(data);
		}
		if(result.isEmpty()){
			return null;
		}
		return result;
	}
}
