package Controller;

import java.io.File;
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
import Service.CheckinService;

public class UpdateDBPoolCount extends Command {

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
			
			int count = (Integer) requestBodyHash.get("DBPoolCount");
			CheckIn.setDbPoolCount(count);
			AMQP.BasicProperties properties = (AMQP.BasicProperties) props.get("properties");
			AMQP.BasicProperties replyProps = (AMQP.BasicProperties) props.get("replyProps");
			
			Envelope envelope = (Envelope) props.get("envelope");
			
			HashMap<String, Object> createdMessage =  new HashMap<String, Object>();
			createdMessage.put("Message",(Object)"DB pool count was updated successfully");
			System.out.println("Reply To" + properties.getReplyTo());					
			JSONObject response = jsonFromMap(createdMessage);
			
			channel.basicPublish("", properties.getReplyTo(), replyProps, response.toString().getBytes("UTF-8"));
		} catch (ParseException e) {
			e.printStackTrace();
			} catch (IOException e) {
			e.printStackTrace();
		}

}
}

