package Commands;

import Model.CheckIn;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mongodb.DBObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public class GetCheckIn extends Command {

	@Override
    public void execute() {
        HashMap < String, Object > props = parameters;
        Channel channel = (Channel) props.get("channel");
        JSONParser parser = new JSONParser();

        try {
        	
			JSONObject messageBody = (JSONObject) parser.parse((String) props.get("body"));
//			System.out.println( ((JSONObject) parser.parse((String) props.get("body"))).get("uri").toString());
//			HashMap<String, Object> paramsHashMap = jsonToMap((JSONObject) messageBody.get("uri"));
			String url = ((JSONObject) parser.parse((String) props.get("body"))).get("uri").toString();
//			System.out.println(paramsHashMap.toString());
			url = url.substring(1);
			System.out.println(Arrays.toString(url.split("/")));
			
			String[] parametersArray = url.split("/");
            AMQP.BasicProperties properties = (AMQP.BasicProperties) props.get("properties");
            AMQP.BasicProperties replyProps = (AMQP.BasicProperties) props.get("replyProps");
            Envelope envelope = (Envelope) props.get("envelope");
			
			HashMap < String, Object > message = CheckIn.get(parametersArray[1]);
            JSONObject response = jsonFromMap(message);
            channel.basicPublish(
                "",
                properties.getReplyTo(),
                replyProps,
                response.toString().getBytes("UTF-8"));
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }

}
