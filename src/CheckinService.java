import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import Commands.Command;
import Commands.CreateCheckIn;
import Commands.GetCheckIn;

public class CheckinService {
	private static final String RPC_QUEUE_NAME = "checkin-request";
	public static  MongoDatabase database;
	public static void main(String[] argv) {

		MongoClientURI uri = new MongoClientURI(
				"mongodb://localhost");

		MongoClient mongoClient = new MongoClient(uri);
		database = mongoClient.getDatabase("El-Menus");
		// initialize thread pool of fixed size
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

		ConnectionFactory factory = new ConnectionFactory();
		String host = System.getenv("RABBIT_MQ_SERVICE_HOST");
		factory.setHost(host);
		Connection connection = null;
		try {
			connection = factory.newConnection();
			final Channel channel = connection.createChannel();

			channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);

			System.out.println(" [x] Awaiting RPC requests");

			Consumer consumer = new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
						byte[] body) throws IOException {
					AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
							.correlationId(properties.getCorrelationId()).build();
					System.out.println("Responding to corrID: " + properties.getCorrelationId());

					try {
						String message = new String(body, "UTF-8");
						JSONParser parser = new JSONParser();
						JSONObject messageBody = (JSONObject) parser.parse(message);
						String command = (String) messageBody.get("command");
						Command cmd = null;
						
//						String paramsUri = messageBody.get("uri").toString().substring(1); // gets route/params
//						String[] params = paramsUri.split("/");
						
						System.out.println("CMD :  " + command);
						
						switch (command) {
						case "CreateCheckin":
							cmd = new CreateCheckIn();
							break;
						case "RetrieveCheckin":
								cmd = new GetCheckIn();
							break;
//                            case "UpdateMessages":   cmd = new UpdateMessage();
//                                break;
//                            case "DeleteMessages":   cmd = new DeleteMessage();
//                                break;
						}

						HashMap<String, Object> props = new HashMap<String, Object>();
						props.put("channel", channel);
						props.put("properties", properties);
						props.put("replyProps", replyProps);
						props.put("envelope", envelope);
						props.put("body", message);

						cmd.init(props);
						executor.submit(cmd);
					} catch (RuntimeException e) {
						System.out.println(" [.] " + e.toString());
					} catch (ParseException e) {
						e.printStackTrace();
					} finally {
						synchronized (this) {
							this.notify();
						}
					}
				}
			};

			channel.basicConsume(RPC_QUEUE_NAME, true, consumer);
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}

	}

	public static String getCommand(String message) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject messageJson = (JSONObject) parser.parse(message);
		String result = messageJson.get("command").toString();
		return result;
	}
	public static MongoDatabase getDb() {
		return database;
	}
}
