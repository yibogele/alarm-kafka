package com.fanwill.alarm.common;

public final class CommonConstants {
	
	/**
	 * kafka topic name
	 */
	public static final String KAFKA_TOPIC_NAME_CAR = "device_data_topic";
	
	public static final String KAFKA_TOPIC_NAME_HUMAN = "person_info_t";
	
	public static final String KAFKA_TOPIC_NAME_SM_SZ = "sz_data_topic";
	
	public static final String KAFKA_TOPIC_NAME_SM_GC = "gc_data_topic";
	
	/**
	 * simulator key - sz
	 */
	public static final String SM_KEY_NAME_DEVIDSTR = "devidStr";
	
	public static final String SM_KEY_NAME_DEVTYPE = "devTypeStr";
	
	public static final String SM_KEY_NAME_DATATIME = "dataTime";
	
	public static final String SM_KEY_NAME_DATA = "data";
	
	
	/**
	 * ActiveMQ topic name
	 */
	public static final String AMQ_TOPIC_NAME_CAR = "deviceInfo";
	
	public static final String AMQ_TOPIC_NAME_HUMAN = "personInfo";
	
	public static final String AMQ_TOPIC_NAME_SM_SZ = "szInfo";
	
	public static final String AMQ_TOPIC_NAME_SM_GC = "gcInfo";
	
	
	//
	private CommonConstants(){}
}
