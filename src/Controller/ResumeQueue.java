package Controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import org.json.simple.JSONObject;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;

import Commands.Command;
import Service.CheckinService;

public class ResumeQueue extends Command {
	@Override
	protected void execute() throws NoSuchMethodException, SecurityException, ClassNotFoundException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// TODO Auto-generated method stub
		ConnectionFactory factory = new ConnectionFactory();
		String host = System.getenv("RABBIT_MQ_SERVICE_HOST");
		factory.setHost(host);
		Connection connection = null;
		HashMap<String, Object> props = parameters;
		Channel channel = (Channel) props.get("channel");
		try {
			CheckinService.setRPC_QUEUE_NAME("checkin-request");
			CheckinService.run();
			System.out.println(CheckinService.getRPC_QUEUE_NAME());
			connection = factory.newConnection();
			channel = connection.createChannel();
			channel.queueDeclare("like-request",false,false,false,null);
			AMQP.BasicProperties properties = (AMQP.BasicProperties) props.get("properties");
			AMQP.BasicProperties replyProps = (AMQP.BasicProperties) props.get("replyProps");
			
			Envelope envelope = (Envelope) props.get("envelope");
			
			HashMap<String, Object> createdMessage =  new HashMap<String, Object>();
			createdMessage.put("Message",(Object)"Queue is resumed successfully");
			System.out.println("Reply To" + properties.getReplyTo());					
			JSONObject response = jsonFromMap(createdMessage);
			
			channel.basicPublish("", properties.getReplyTo(), replyProps, response.toString().getBytes("UTF-8"));
		} catch (IOException | TimeoutException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}		
			
		
	}
}
