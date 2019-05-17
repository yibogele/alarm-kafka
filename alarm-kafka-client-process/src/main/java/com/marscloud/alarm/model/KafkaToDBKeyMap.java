package com.fanwill.alarm.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaToDBKeyMap {
	private static final ConcurrentHashMap<String, String> map;
	
	private static final Set<String> keys;
	static{
		map = new ConcurrentHashMap<>();
		map.put("phoneNo", "carNum");
//		map.put("devidStr", "carNum");
		map.put("chassis_speed", "speed");
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
		
		keys = Collections.unmodifiableSet(new HashSet<>(map.keySet()));
	}
	
	public static String getMappedKey(String key) {
		return map.get(key);
	}
	
	public static Set<String> getKeys() {
//		return Collections.unmodifiableSet(map.keySet());
		return keys;
	}
}
