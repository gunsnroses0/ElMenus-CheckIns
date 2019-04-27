package Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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
	private static String RPC_QUEUE_NAME = "checkin-request";
	public static String getRPC_QUEUE_NAME() {
		return RPC_QUEUE_NAME;
	}

	public static void setRPC_QUEUE_NAME(String rPC_QUEUE_NAME) {
		System.out.println("RENAMING");
		RPC_QUEUE_NAME = rPC_QUEUE_NAME;
	}
	public static  MongoDatabase database;
	public static HashMap<String, String> config;
	public static void main(String[] argv) {
		run();
		ServiceController.run();
	}
	private static int threadPoolCount=4;
	public static int getThreadPoolCount() {
		return threadPoolCount;
	}

	public static void setThreadPoolCount(int threadPoolCount) {
		CheckinService.threadPoolCount = threadPoolCount;
	}
	public static void run() {
		try {
			updateHashMap();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

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
//						String service = StringUtils.substringsBetween((String) messageBody.get("uri"), "/", "/");
						String[] URI = ((String) messageBody.get("uri")).split(Pattern.quote("/"));
						String service = "";
						for (int i = 0; i < URI.length; i++) {
							if (!(StringUtils.containsAny(URI[i],
									new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' }))) {
								service += URI[i] + "/";
							} else {
								service += "id";

							}
						}
//						System.out.println((String) messageBody.get("uri"));
//						StringUtils.containsAny(str, searchChars)
						System.out.println("URI" + URI[0]);
						String key = (String) messageBody.get("request_method") + service;
						System.out.println("KEY" + key);
						System.out.println("config" + config.get(key));
						String command = (String) config.get(key);
						Command cmd = (Command) Class.forName("Commands." + command).newInstance();
						System.out.println(cmd);

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
					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
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
	public static void updateHashMap() throws IOException {
		config = new HashMap<String, String>();
		System.out.println("X");
		File file = new File("src/config");
		BufferedReader br = new BufferedReader(new FileReader(file));

		String st;

		while ((st = br.readLine()) != null) {
			System.out.println(st);
			String[] array = st.split(",");
			config.put(array[0] + array[1], array[2]);
		}
		System.out.println(config);
		br.close();

	}
}
