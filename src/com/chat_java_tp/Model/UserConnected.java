package com.chat_java_tp.Model;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONObject;

public class UserConnected {

	private int id;
	private int idUser;
	private String lastConnection;
	private InetAddress ipAdresse;
	private int port;
	private boolean isLogged;

	public UserConnected(int id, int idUser, InetAddress inetAddress, int port, boolean isLogged,
			String lastConnection) {
		this.id = id;
		this.idUser = idUser;
		this.lastConnection = lastConnection;
		this.ipAdresse = inetAddress;
		this.port = port;
		this.isLogged = isLogged;
	}

	public UserConnected(JSONObject json) {
		this.id = json.optInt("id", 0);
		this.idUser = json.optInt("idUser");
		this.lastConnection = json.optString("lastConnection", null);
		try {
			this.ipAdresse = InetAddress.getByName(json.optString("ipAdresse", ""));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.port = json.optInt("port", 0);
		this.isLogged = json.optInt("isLogged", 0) == 1;
	}

	// Getters et Setters
	public int getId() {
		return id;
	}

	public int getIdUser() {
		return idUser;
	}

	public String getLastConnection() {
		return lastConnection;
	}

	public String getIpAdresse() {
		return ipAdresse != null ? ipAdresse.getHostAddress() : null;
	}

	public int getPort() {
		return port;
	}

	public boolean isLogged() {
		return isLogged;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("id", this.id);
		json.put("idUser", this.idUser);
		json.put("lastConnection", this.lastConnection);
		json.put("ipAdresse", this.ipAdresse);
		json.put("port", this.port);
		json.put("isLogged", this.isLogged ? 1 : 0);
		return json;
	}
}
