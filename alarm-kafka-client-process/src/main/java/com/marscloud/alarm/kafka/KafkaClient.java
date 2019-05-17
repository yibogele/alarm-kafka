package com.fanwill.alarm.kafka;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fanwill.alarm.activemq.MQSender;
import com.fanwill.alarm.common.CommonConstants;
import com.fanwill.alarm.common.ConfigConstants;
import com.fanwill.alarm.common.ConfigReader;
import com.fanwill.wlw_common.kafka.DeserializeUtil;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

public class KafkaClient {

    public static void main(String[] args) {
        PropertyConfigurator.configure("../conf/log4j.properties");

        String defaultZookeeper = "192.168.1.3:2181";
        String defaultGroupId = "test-alarm-group";
        String defaultTopics =
                "person_info";
//				"xhw_device_data_topic,"+
//				"device_data_topic";
        final String zookeeper = ConfigReader.readConfig(ConfigConstants.KEY_ZK, defaultZookeeper);
        final String groupId = ConfigReader.readConfig(ConfigConstants.KEY_GROUP_ID, defaultGroupId);
        final String[] topics = ConfigReader.readConfig(ConfigConstants.KEY_TOPICS, defaultTopics).split(",");

        KafkaClient kafkaClient = new KafkaClient(zookeeper, groupId, topics);
        kafkaClient.doStartProcessKafkaData();

    }

    // mq
    private final String mqBrokerUrl;
    private final String mqUserName;
    private final String mqPassword;
    private MQSender mqSender;

    // kafka
    private final String zooKeeper;
    private final String groupId;
    private final String topics[];

    private final Logger logger = LoggerFactory.getLogger(KafkaClient.class);
    // topic:connector
    private Map<String, ConsumerConnector> consumerConnectorMap;
    private ScheduledExecutorService sex = null;
    private boolean isInit = false;

    public KafkaClient(String zookeeper, String groupId, String topics[]) {
        this.zooKeeper = zookeeper;
        this.groupId = groupId;
        this.topics = topics;

        String dBrokerUrl = "tcp://192.168.52.224:61616";
        String dUserName = "admin";
        String dPassword = "admin";

        mqBrokerUrl = ConfigReader.readConfig(ConfigConstants.KEY_BROKER_URL, dBrokerUrl);
        mqUserName = ConfigReader.readConfig(ConfigConstants.KEY_USER, dUserName);
        mqPassword = ConfigReader.readConfig(ConfigConstants.KEY_PASSWORD, dPassword);

        this.mqSender = new MQSender(this.mqBrokerUrl, this.mqUserName, this.mqPassword);
    }

    private final void initConsum() {
        if (consumerConnectorMap != null) {
            for (Entry<String, ConsumerConnector> entry : consumerConnectorMap.entrySet()) {
                ConsumerConnector va = entry.getValue();
                if (va != null) {
                    va.shutdown();
                }
                va = null;
            }
        }

        if (sex != null) {
            sex.shutdown();
        }
        sex = null;
        consumerConnectorMap = new HashMap<String, ConsumerConnector>();
        for (String topic : topics) {
            Properties props = new Properties();
            props.put("zookeeper.connect", zooKeeper);
            props.put("group.id", groupId);
            props.put("zookeeper.session.timeout.ms", "10000");
            props.put("zookeeper.sync.time.ms", "200");
            props.put("auto.commit.interval.ms", "1000");
            ConsumerConfig consumerConfig = new ConsumerConfig(props);
            ConsumerConnector consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);
            consumerConnectorMap.put(topic, consumerConnector);
        }
        sex = this.getExecutorServices();
    }

    /**
     * 开始读取数据
     */
    public final void doStartProcessKafkaData() {
        if (!isInit) {
            synchronized (this) {
                if (!isInit) {
                    this.initConsum();
                    isInit = true;
                }
            }
        }
        this.incrementThreadControl();
    }

    private final ScheduledExecutorService getExecutorServices() {
        final ThreadGroup tg = new ThreadGroup("警告处理线程组");
        ThreadFactory tf = new ThreadFactory() {
            int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread th = new Thread(tg, r, "alarm_thread_" + (count++));
                th.setPriority(Thread.MIN_PRIORITY);
                th.setDaemon(true);
                return th;
            }
        };
        final int coreThreadCount = 5;
        sex = Executors.newScheduledThreadPool(coreThreadCount, tf);
        return sex;
    }

    /**
     * 添加任务的数量控制
     */
    private final void incrementThreadControl() {
        Thread taskThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        cc.increment();
                        sex.submit(new Runnable() {
                            @Override
                            public void run() {
                                KafkaClient.this.doConsumeFromKafka();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        taskThread.setDaemon(false);
        taskThread.setPriority(Thread.NORM_PRIORITY);
        taskThread.start();
    }

    private final CustomerCount cc = new CustomerCount();

    private static final class CustomerCount {
        private int count = 0;
        private final int allCount = 2;
        private Lock lock = new ReentrantLock();
        final Condition notFull = lock.newCondition();
        final Condition notEmpty = lock.newCondition();

        /**
         * 递增的数量控制
         *
         * @throws InterruptedException
         */
        private final void increment() throws InterruptedException {
            lock.lock();
            try {
                while (count > allCount) {
                    notFull.await();
                }
                ++count;
                notEmpty.signalAll();
            } finally {
                lock.unlock();
            }
        }

        /**
         * 递减的数量控制
         *
         * @throws InterruptedException
         */
        private final void decrement() throws InterruptedException {
            lock.lock();
            try {
                while (count < 1) {
                    notEmpty.await();
                }
                --count;
                notFull.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    // protected static class KafkaMachineData {
    // private MachineEnum machineEnum;
    // private Map<String, String> data;
    //
    // public KafkaMachineData(MachineEnum machineEnum, Map<String, String>
    // data) {
    // super();
    // this.machineEnum = machineEnum;
    // this.data = data;
    // }
    //
    // public MachineEnum getMachineEnum() {
    // return machineEnum;
    // }
    //
    // public Map<String, String> getData() {
    // return data;
    // }
    //
    // }

    /**
     * 获取数据
     *
     * @return
     */
    // private final Map<String, MachineEnum> machineEnumMap = new
    // ConcurrentHashMap<String, MachineEnum>();
    private final void doConsumeFromKafka() {
        for (Entry<String, ConsumerConnector> entry : consumerConnectorMap.entrySet()) {
            final String kafkaTopicName = entry.getKey();
            ConsumerConnector consumerConnector = entry.getValue();
            Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
            topicCountMap.put(entry.getKey(), 1);
            Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumerConnector
                    .createMessageStreams(topicCountMap);
            List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(entry.getKey());
            for (final KafkaStream<byte[], byte[]> stream : streams) {
                sex.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            ConsumerIterator<byte[], byte[]> it = stream.iterator();
                            while (it.hasNext()) {
                                Map<String, String> kafkaData = null;

                                if (StringUtils.equals(kafkaTopicName, CommonConstants.KAFKA_TOPIC_NAME_CAR) ||
                                        StringUtils.equals(kafkaTopicName, CommonConstants.KAFKA_TOPIC_NAME_HUMAN) ||
                                        StringUtils.equals(kafkaTopicName, "xhw_device_data_topic") ||
                                        StringUtils.equals(kafkaTopicName, "person_info")) {
                                    kafkaData = DeserializeUtil
                                            .deserializeBytes2Map(it.next().message());

                                } else if (StringUtils.equals(kafkaTopicName, CommonConstants.KAFKA_TOPIC_NAME_SM_SZ)) {
                                    kafkaData = deserializeSimulator(it.next().message());
                                } else if (StringUtils.equals(kafkaTopicName, CommonConstants.KAFKA_TOPIC_NAME_SM_GC)) {
                                    kafkaData = deserializeSimulator(it.next().message());
                                }


                                if (null == kafkaData) {
                                    logger.info("Validate Kafka data failed");
                                    continue;
                                }

                                logger.info("Kafka data before validate:[{}]" + kafkaData);
                                System.out.println("=>" + kafkaData);

//								mqSender.sendToMQ(kafkaTopicName, kafkaData);
                            }
                        } catch (Exception e) {
                            try {
                                // 新创建一个队列处理数据
                                KafkaClient.this.cc.decrement();
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                            e.printStackTrace();
                            throw e;
                        }
                    }

                    private Map<String, String> deserializeSimulator(byte[] message) {
                        Map<String, String> ret = new HashMap<>();

                        ByteArrayInputStream byteArrayInputStream = null;
                        ObjectInputStream objectInputStream = null;


                        try {
                            byteArrayInputStream = new ByteArrayInputStream(message);
                            objectInputStream = new ObjectInputStream(byteArrayInputStream);

                            @SuppressWarnings("unchecked")
                            Map<String, Object> msg = (Map<String, Object>) objectInputStream.readObject();
                            String devtype = (String) msg.get(CommonConstants.SM_KEY_NAME_DEVTYPE);
                            Long datetime = (Long) msg.get(CommonConstants.SM_KEY_NAME_DATATIME);
                            String datetimeStr = datetime.toString();
                            logger.info("datatime = {} , devtype = {}", datetime, devtype);
                            byte[] data = (byte[]) msg.get(CommonConstants.SM_KEY_NAME_DATA);
                            if (data == null) {
                                logger.info("data is null");
                            }
                            ret.put(CommonConstants.SM_KEY_NAME_DATA, new String(data));
                        } catch (Exception e) {
                            logger.info("deserializeSimulator exception: ", e);
                        } finally {
                            try {
                                if (objectInputStream != null) {
                                    objectInputStream.close();
                                }
                                if (byteArrayInputStream != null) {
                                    byteArrayInputStream.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        return ret;
                    }
                });
            }
        }
    }
}
