package com.marscloud.alarm.activemq;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.StreamMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.commons.lang3.StringUtils;

import com.marscloud.alarm.common.CommonConstants;
import com.marscloud.alarm.model.KafkaToDBKeyMap;

import messagePackage.MessageModel;
import messagePackage.MessageObject;

//import sendPackage.SendMsg;

public class MQSender {
//	private static final String USERNAME = ActiveMQConnectionFactory.DEFAULT_USER;
//	private static final String PASSWORD = ActiveMQConnectionFactory.DEFAULT_PASSWORD;
//	private static final String BROKER_URL = "tcp://192.168.52.224:61616";
	// 设置连接的最大连接数
	public final static int DEFAULT_MAX_CONNECTIONS = 5;
	private int maxConnections = DEFAULT_MAX_CONNECTIONS;
	// 设置每个连接中使用的最大活动会话数
	private int maximumActiveSessionPerConnection = DEFAULT_MAXIMUM_ACTIVE_SESSION_PER_CONNECTION;
	public final static int DEFAULT_MAXIMUM_ACTIVE_SESSION_PER_CONNECTION = 300;
	// 线程池数量
	private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
	public final static int DEFAULT_THREAD_POOL_SIZE = 50;
	// 强制使用同步返回数据的格式
	private boolean useAsyncSendForJMS = DEFAULT_USE_ASYNC_SEND_FOR_JMS;
	public final static boolean DEFAULT_USE_ASYNC_SEND_FOR_JMS = true;
	// 是否持久化消息
	private boolean isPersistent = DEFAULT_IS_PERSISTENT;
	public final static boolean DEFAULT_IS_PERSISTENT = false;

	// 连接地址
	private String brokerUrl;

	private String userName;

	private String password;

	private ExecutorService threadPool;

	private PooledConnectionFactory connectionFactory;

	public MQSender(String brokerUrl, String userName, String password) {
		this(brokerUrl, userName, password, DEFAULT_MAX_CONNECTIONS, DEFAULT_MAXIMUM_ACTIVE_SESSION_PER_CONNECTION,
				DEFAULT_THREAD_POOL_SIZE, DEFAULT_USE_ASYNC_SEND_FOR_JMS, DEFAULT_IS_PERSISTENT);
	}

	public MQSender(String brokerUrl, String userName, String password, int maxConnections,
			int maximumActiveSessionPerConnection, int threadPoolSize, boolean useAsyncSendForJMS,
			boolean isPersistent) {
		this.useAsyncSendForJMS = useAsyncSendForJMS;
		this.isPersistent = isPersistent;
		this.brokerUrl = brokerUrl;
		this.userName = userName;
		this.password = password;
		this.maxConnections = maxConnections;
		this.maximumActiveSessionPerConnection = maximumActiveSessionPerConnection;
		this.threadPoolSize = threadPoolSize;
		init();
	}

	private void init() {
		this.threadPool = Executors.newFixedThreadPool(this.threadPoolSize);
		// ActiveMQ的连接工厂
		ActiveMQConnectionFactory actualConnectionFactory = new ActiveMQConnectionFactory(this.userName, this.password,
				this.brokerUrl);
		actualConnectionFactory.setUseAsyncSend(this.useAsyncSendForJMS);
		// Active中的连接池工厂
		this.connectionFactory = new PooledConnectionFactory(actualConnectionFactory);
		this.connectionFactory.setCreateConnectionOnStartup(true);
		this.connectionFactory.setMaxConnections(this.maxConnections);
		this.connectionFactory.setMaximumActiveSessionPerConnection(this.maximumActiveSessionPerConnection);

	}

	private Map<String, String> getDevReflectionMap(){
		Map<String, String> map = new HashMap<>();
		
		map.put("phoneNo", "carNum");
//		map.put("devidStr", "carNum");
//		map.put("chassis_speed", "speed");
		map.put("gps_speed", "speed");
		map.put("acc", "accState");
//		map.put("work_status", "workStatus");
		map.put("status", "workStatus");
		map.put("longitude", "lon");
		map.put("latitude", "lat");
		map.put("engine_speed", "engineRpm");
		map.put("verSpeed", "verSpeed");
		map.put("average_oil", "currentOil");	
		map.put("nvecFault", "nvecFault");
		map.put("nveopAlarm", "nveopAlarm");
		map.put("hFault", "hFault");
		map.put("owAlarm", "owAlarm");
		map.put("tcAlarm", "tcAlarm");
		map.put("toAlarm", "toAlarm");
		map.put("tluAlarm", "tluAlarm");
		map.put("tuluAlarm", "tuluAlarm");
		map.put("turAlarm", "turAlarm");
		map.put("trAlarm", "trAlarm");
		map.put("time", "collectTime");
		
		return map;
	}
	
	private Map<String, String> getHumanReflectionMap(){
		Map<String, String> ret = new HashMap<>();
		
		ret.put("imei", "workerNum");
		// devId是在imei前加"00"
//		ret.put("devId", "workerNum"); 
		
		// 手环上传数据没有status 和 speed
		// {timestamp=1526917507000, info_type=1, flag=ad, gps_time=1526917507000, convertedLatitude=39.06647, imei=1453858076, convertedLongitude=115.795204, longitude=115.795204, latitude=39.06647}
		ret.put("status", "workStatus");
		ret.put("speed", "speed");
		
		ret.put("longitude", "lon");
		ret.put("latitude", "lat");

		ret.put("gps_time", "collectTime");
		
		return ret;
	}
	public void sendToMQ(String kafkaTopicName, Map<String, String> data) {
		//
		Map<String, String> map = null;

		
		///
		boolean isDevelop = Boolean.parseBoolean(System.getProperty("IsDevelop", "false"));
		
		// 数据分类发往不同的topic
		if (kafkaTopicName.equals("xhw_device_data_topic") ||
				kafkaTopicName.equals(CommonConstants.KAFKA_TOPIC_NAME_CAR)) {
			
			MessageModel messageModel = null;
			if (isDevelop) {
				messageModel = MessageModel.createInstance("messageModel_setup-kf.json");

			} else {
				final String configPath = System.getProperty("alarm.gateway.path", null);
				String jsonFileName = configPath + "conf/messageModel_setup-kf.json";
				messageModel = MessageModel.createInstance(jsonFileName);

			}
			
			
			MessageObject messageObject = messageModel.getMessageObjectByID("KFMSG10001");
			//
//			for (String key : KafkaToDBKeyMap.getKeys()) {
			map = getDevReflectionMap();
			for (String key : map.keySet()){
				String value = data.get(key);
				if(value != null)
				{
//					String mappedKey = KafkaToDBKeyMap.getMappedKey(key);
					String mappedKey = map.get(key);
					messageObject.setPropertyValue(mappedKey,value);
					System.out.println("\t" + mappedKey + "=>" + value);
				}
			}
			//
			byte[] senddata = messageObject.toStream();
			// TODO, change topic to deviceInfo
			sendThread(CommonConstants.AMQ_TOPIC_NAME_CAR, senddata);
		} else if(kafkaTopicName.equals(CommonConstants.KAFKA_TOPIC_NAME_HUMAN) || 
				kafkaTopicName.equals("person_info")) {
			MessageModel messageModel = null;
			if (isDevelop) {
				messageModel = MessageModel.createInstance("messageModel_setup.json");

			} else {
				final String configPath = System.getProperty("alarm.gateway.path", null);
				String jsonFileName = configPath + "conf/messageModel_setup.json";
				messageModel = MessageModel.createInstance(jsonFileName);

			}
			
			
			MessageObject messageObject = messageModel.getMessageObjectByID("KFMSG10002");
			
			map = getHumanReflectionMap();
			for (String key : map.keySet()){
				String value = data.get(key);
				if(value != null)
				{
//					String mappedKey = KafkaToDBKeyMap.getMappedKey(key);
					String mappedKey = map.get(key);
					messageObject.setPropertyValue(mappedKey,value);
					System.out.println("\t" + mappedKey + "=>" + value);
				}
			}
			
			byte[] senddata = messageObject.toStream();
			sendThread(CommonConstants.AMQ_TOPIC_NAME_HUMAN, senddata);
		} else if (StringUtils.equals(kafkaTopicName, CommonConstants.KAFKA_TOPIC_NAME_SM_SZ)) {
//			sendThread(CommonConstants.AMQ_TOPIC_NAME_SM_SZ, 
//					data.get(CommonConstants.SM_KEY_NAME_DATA).getBytes());
		} else if(StringUtils.equals(kafkaTopicName, CommonConstants.KAFKA_TOPIC_NAME_SM_GC)){
			String dataStr = data.get(CommonConstants.SM_KEY_NAME_DATA);
			if(!StringUtils.isEmpty(dataStr))
				sendThread(CommonConstants.AMQ_TOPIC_NAME_SM_GC, 
					dataStr.getBytes());
		}
		//
		
	}

	private void sendThread(final String topicName, final byte[] data) {
		this.threadPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					send(topicName, data);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void send(final String topicName, final byte[] data) {

		Connection connection = null;
		Session session = null;
		try {
			// 从连接池中获取一个连接
			connection = this.connectionFactory.createConnection();
			session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createTopic(topicName);
			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(this.isPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
			Message message = getMessage(session, data);
			producer.send(message);
		} catch (JMSException e) {
		}catch (UnsupportedEncodingException e) {
		} 
		finally {
			closeSession(session);
			closeConnection(connection);
		}
	}

	private Message getMessage(Session session, byte[] data) throws JMSException, UnsupportedEncodingException {
		StreamMessage streamMessage = session.createStreamMessage();
		streamMessage.writeString(new String(data, "ISO-8859-1"));

		return streamMessage;
	}

	private void closeSession(Session session) {
		try {
			if (session != null) {
				session.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void closeConnection(Connection connection) {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
