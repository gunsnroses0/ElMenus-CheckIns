package Controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

import Commands.Command;
import Model.CheckIn;
import io.netty.handler.codec.http.multipart.FileUpload;

public class AddCommand extends Command {

	@Override
	protected void execute() throws NoSuchMethodException, SecurityException, ClassNotFoundException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// TODO Auto-generated method stub
				HashMap<String, Object> props = parameters;
				Channel channel = (Channel) props.get("channel");
				JSONParser parser = new JSONParser();
				try {
					
					JSONObject messageBody = (JSONObject) parser.parse((String) props.get("body"));
					System.out.println(messageBody);
					System.out.println(messageBody.get("form"));
					HashMap<String, Object> requestBodyHash = jsonToMap((JSONObject) messageBody.get("form"));
					
					String fileContent = (String) requestBodyHash.get("file");
					String fileName = (String) requestBodyHash.get("fileName");
					String configText = (String)requestBodyHash.get("config");
					updateConfig(configText);
					System.out.println("NAMEEE"+fileName);
					File file = new File("src/Commands/" + fileName);
					file.createNewFile();
					
					writeFile(fileContent,"src/Commands/"+ fileName);
					
					
					AMQP.BasicProperties properties = (AMQP.BasicProperties) props.get("properties");
					AMQP.BasicProperties replyProps = (AMQP.BasicProperties) props.get("replyProps");
					
					Envelope envelope = (Envelope) props.get("envelope");
					
					HashMap<String, Object> createdMessage =  new HashMap<String, Object>();
					createdMessage.put("Message",(Object)"Command was added successfully");
					System.out.println("Reply To" + properties.getReplyTo());					
					JSONObject response = jsonFromMap(createdMessage);
					
					channel.basicPublish("", properties.getReplyTo(), replyProps, response.toString().getBytes("UTF-8"));
				} catch (ParseException e) {
					e.printStackTrace();
					} catch (IOException e) {
					e.printStackTrace();
				}
		
	}
	public static void updateConfig(String string) 
	{ 
		try { 

// Open given file in append mode. 
			BufferedReader br = new BufferedReader(new FileReader("src/config")); 
			BufferedWriter out = new BufferedWriter(new FileWriter("src/config", true));   
			  String st; 
			  boolean found = false;
			  while ((st = br.readLine()) != null) {
				  System.out.println(st+string);
				    if (st.equals(string)) 
				    	found = true;
				    }
			 br.close(); 
			 if (!found) {
				 out.write("\n");
				out.write(string); 
			 }
			out.close(); 
		} 
		catch (IOException e) { 
			System.out.println("exception occoured" + e); 
		} 
	}
	public static void writeFile(String string, String path) 
	{ 
		try { 

// Open given file in append mode. 
			BufferedWriter out = new BufferedWriter( 
					new FileWriter(path, true)); 
			out.write(string); 
			out.close(); 
		} 
		catch (IOException e) { 
			System.out.println("exception occoured" + e); 
		} 
	}


}
