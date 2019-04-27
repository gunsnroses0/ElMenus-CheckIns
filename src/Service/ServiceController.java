package Service;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public abstract class ServiceController {

	private static final String RPC_QUEUE_NAME = "checkin-controller";
	public static  MongoDatabase database;
	public static HashMap<String, String> config;
	
	public static void run() {
		try {
			updateHashMap();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
						String [] URI = ((String) messageBody.get("uri")).split(Pattern.quote("/"));
						String service= "";
						for (int i =0; i<URI.length;i++) {
							if (!(StringUtils.containsAny(URI[i],  new char []{'0','1','2','3','4','5','6','7','8','9'}))) {
								service += URI[i]+"/";
							}else {
								service += "id";

							}
						}
//						System.out.println((String) messageBody.get("uri"));
//						StringUtils.containsAny(str, searchChars)
						System.out.println("URI"+ URI[0]);
						String key = (String) messageBody.get("request_method")+ service;
						System.out.println("KEY"+ key );
						System.out.println("config"+config.get(key));
						String command = (String)config.get(key);
						Command cmd = (Command) Class.forName("Controller."+command).newInstance();
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

   

    public static void addCommand(String readFile, String path){
    	System.out.println(readFile+path);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(readFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter("target/classes/Commands/"+path));
            String line = "";
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
            reader.close();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void updateCommand(String path, String readFile, String writeFile){
        try {
            Files.deleteIfExists(Paths.get("target/classes/Commands/"+path));
            BufferedReader reader = new BufferedReader(new FileReader(readFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter("target/classes/Commands/"+writeFile));
            String line = "";
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteCommand(String path){
        try {
            Files.deleteIfExists(Paths.get("target/classes/Commands/"+path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void freeze(){
//        try {
//            channel.basicCancel(consumerTag);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    void resume(){
//        try {
//            channel.basicConsume(RPC_QUEUE_NAME, false, consumer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    void error(){

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
			  config.put(array[0]+array[1],array[2]);
		  }
		  System.out.println(config);
		 br.close(); 

	}

}
