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

public class DeleteCommand extends Command {

	@Override
	protected void execute() throws NoSuchMethodException, SecurityException, ClassNotFoundException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// TODO Auto-generated method stub
		HashMap<String, Object> props = parameters;
		Channel channel = (Channel) props.get("channel");
		JSONParser parser = new JSONParser();
		try {
			
			JSONObject messageBody = (JSONObject) parser.parse((String) props.get("body"));
			HashMap<String, Object> requestBodyHash = jsonToMap((JSONObject) messageBody.get("form"));
			String command = (String) requestBodyHash.get("command");
			
			updateConfig(command);
			File file = new File ("src/Commands/"+command+".java");
//			writeFile("", "src/Commands/"+command+".java");
			file.delete();			
			AMQP.BasicProperties properties = (AMQP.BasicProperties) props.get("properties");
			AMQP.BasicProperties replyProps = (AMQP.BasicProperties) props.get("replyProps");
			Envelope envelope = (Envelope) props.get("envelope");
			HashMap<String, Object> createdMessage =  new HashMap<String, Object>();
			createdMessage.put("Message:",(Object)"Command was deleted successfully");
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
			boolean firstLine = true;
			BufferedReader br = new BufferedReader(new FileReader("src/config")); 
			File tempFile = new File("src/tmp");
			File inputFile = new File("src/config");
			BufferedWriter out = new BufferedWriter(new FileWriter("src/tmp", true));  
		
			  String st; 
			  while ((st = br.readLine()) != null) {
				  String[] array = st.split(",");
				    if (array[2].equals(string)) continue;
				    
				   if (firstLine) {
					   out.write(st);
					   firstLine = false;
				   } else {
					   out.write("\n"+st);
				   }
			  }
			  br.close(); 
			  out.close(); 
			  tempFile.renameTo(inputFile);
		} 
		catch (IOException e) { 
			System.out.println("exception occoured" + e); 
		} 
	}

}
