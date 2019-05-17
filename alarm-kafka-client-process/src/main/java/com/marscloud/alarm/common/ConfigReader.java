package com.marscloud.alarm.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigReader {
	private final static Logger LOGGER = LoggerFactory.getLogger(ConfigReader.class);
	
	private static Properties properties = new Properties();
	
	private ConfigReader(){}
	
	static {
		boolean isDevelop = Boolean.parseBoolean(System.getProperty("IsDevelop", "false"));
		if (isDevelop) {
			try {
				// 从类路径下读取属性文件
				properties.load(ConfigReader.class.getClassLoader().getResourceAsStream("km-client.properties"));

			} catch (Exception e) {
				// TODO: handle exception
				LOGGER.error("Load km-client.properties failed." + e);
			}
		} else {
			FileInputStream inputStream = null;
			try {
				final String configPath = System.getProperty("alarm.gateway.path", null);
				final File fileConfig = new File(configPath, "conf/km-client.properties");

				inputStream = new FileInputStream(fileConfig);
				properties.load(inputStream);
			} catch (Exception e) {
				LOGGER.error("Load km-client.properties failed." + e);
			}finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		
		// 

	}
	
	public static String readConfig(String key, String defaultValue) {
		return  properties.getProperty(key, defaultValue);
	}
}
