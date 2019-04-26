package Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;

import Commands.Command;

public class CheckIn {

	private static final String COLLECTION_NAME = "checkins";

	private static MongoCollection<Document> collection = null;
	private static int DbPoolCount = 4;
	public static int getDbPoolCount() {
		return DbPoolCount;
	}
	public static void setDbPoolCount(int dbPoolCount) {
		DbPoolCount = dbPoolCount;
	}

	public static HashMap<String, Object> create(HashMap<String, Object> atrributes) throws ParseException {

		MongoClientURI uri = new MongoClientURI(
				"mongodb://localhost");

		MongoClient mongoClient = new MongoClient(uri);
		MongoDatabase database = mongoClient.getDatabase("El-Menus");
//    	Method method =   Class.forName("PlatesService").getMethod("getDB", null);
//    	MongoDatabase database = (MongoDatabase) method.invoke(null, null);

		// Retrieving a collection
		MongoCollection<Document> collection = database.getCollection("checkins");
		Document newCheckin = new Document();

		for (String key : atrributes.keySet()) {
			newCheckin.append(key, atrributes.get(key));
		}
		collection.insertOne(newCheckin);

		JSONParser parser = new JSONParser();
		HashMap<String, Object> returnValue = Command.jsonToMap((JSONObject) parser.parse(newCheckin.toJson()));
		return returnValue;
	}

	public static HashMap<String, Object> get(String messageId) {

		MongoClientURI uri = new MongoClientURI(
				"mongodb://localhost");

		MongoClient mongoClient = new MongoClient(uri);
		MongoDatabase database = mongoClient.getDatabase("El-Menus");
//    	Method method =   Class.forName("PlatesService").getMethod("getDB", null);
//    	MongoDatabase database = (MongoDatabase) method.invoke(null, null);
		
		System.out.println(messageId.getClass().getName());
		System.out.println(messageId);

		// Retrieving a collection
		MongoCollection<Document> collection = database.getCollection("checkins");
		System.out.println("Inside Get");
		BasicDBObject query = new BasicDBObject();
		query.put("user_id", messageId);

		System.out.println(query.toString());
		HashMap<String, Object> message = null;
		Document doc = collection.find(query).first();
		JSONParser parser = new JSONParser(); 
		try {
			JSONObject json = (JSONObject) parser.parse(doc.toJson());
		
			message = Command.jsonToMap(json);
			
			System.out.println(message.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return message;
	}
}
