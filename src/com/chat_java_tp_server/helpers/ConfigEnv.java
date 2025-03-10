package com.chat_java_tp_server.helpers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigEnv {
	private static final String ENV_FILE = "config.env";
	private Properties properties;

	public ConfigEnv() {
		properties = new Properties();
		loadProperties();
	}

	private void loadProperties() {
		try (FileInputStream fis = new FileInputStream(ENV_FILE)) {
			properties.load(fis);
		} catch (IOException e) {
			System.err.println("Could not load environment file: " + e.getMessage());
		}
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public void set(String key, String value) {
		properties.setProperty(key, value);
		saveProperties();
	}

	private void saveProperties() {
		try (FileOutputStream fos = new FileOutputStream(ENV_FILE)) {
			properties.store(fos, "Environment Variables");
		} catch (IOException e) {
			System.err.println("Could not save environment file: " + e.getMessage());
		}
	}

}
