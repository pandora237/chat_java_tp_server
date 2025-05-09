package com.chat_java_tp.Model;

import org.json.JSONObject;

public class User {
	enum Sexe {
		M, F
	}

	private int idUser;
	private int idConnection;  
	private String firstname;
	private String lastname;
	private String username;
	private String password;
	private boolean isLogged;
	private String picture;
	private Sexe sexe;
	private String phone;
	private String dateAdd;

	public User(JSONObject json) {
		this.idUser = json.optInt("idUser", 0);
		this.firstname = json.optString("firstname");
		this.lastname = json.optString("lastname");
		this.username = json.optString("username");
		this.password = json.optString("password");
		this.isLogged = json.optInt("isLogged", 0) == 1;
		this.picture = json.optString("picture", null);
		this.sexe = Sexe.valueOf(json.optString("sexe", "M"));
		this.phone = json.optString("phone", null);
		this.dateAdd = json.optString("dateAdd", null);
	}

	// Getters et Setters
	public int getIdUser() {
		return idUser;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public boolean isLogged() {
		return isLogged;
	}

	public String getPicture() {
		return picture;
	}

	public Sexe getSexe() {
		return sexe;
	}

	public String getPhone() {
		return phone;
	}

	public String getDateAdd() {
		return dateAdd;
	}

	public int getIdConnection() {
		return idConnection;
	}

	public void setIdConnection(int idConnection) {
		this.idConnection = idConnection;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("idUser", idUser);
		json.put("firstname", firstname);
		json.put("lastname", lastname);
		json.put("username", username);
		json.put("password", password);
		json.put("isLogged", isLogged ? 1 : 0);
		json.put("picture", picture);
		json.put("sexe", sexe.name());
		json.put("phone", phone);
		json.put("dateAdd", dateAdd);
		return json;
	}
}
