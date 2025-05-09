package com.chat_java_tp.Model;

import java.util.Date;

import org.json.JSONObject;

import com.chat_java_tp_server.helpers.Helpers;
import com.chat_java_tp_server.helpers.Helpers.StatusMess;

public class Message {

	private String dateAdd;
	private String dateUpd;
	private int idMessage;
	private int idSend;
	private int idReceive;
	private String type;
	private String content;
	private String fileName;
	private StatusMess status;

	public Message() {
	}

	public Message(String dateAdd, String dateUpd, int idMessage, int idSend, int idReceive, String type,
			String content, String fileName, String fileName1, StatusMess status) {
		this.dateAdd = dateAdd;
		this.dateUpd = dateUpd;
		this.idMessage = idMessage;
		this.idSend = idSend;
		this.idReceive = idReceive;
		this.type = type;
		this.content = content;
		this.fileName = fileName1;
		this.status = status;
	}

	public Message(JSONObject message) {
		this.dateAdd = message.optString("dateAdd", new Date().toString());
		this.dateUpd = message.optString("dateUpd", new Date().toString());
		this.idMessage = message.optInt("idMessage", 0);
		this.idSend = message.optInt("idSend", 0);
		this.idReceive = message.optInt("idReceive", 0);
		this.type = message.optString("type", Helpers.sendSimpleMess);
		this.content = message.optString("content", "");
		this.fileName = message.optString("fileName", "");
		this.status = StatusMess.valueOf(message.optString("status", StatusMess.SENT.name()));
	}

	// Getters
	public String getDateAdd() {
		return dateAdd;
	}

	public String getDateUpd() {
		return dateUpd;
	}

	public int getIdMessage() {
		return idMessage;
	}

	public int getIdSend() {
		return idSend;
	}

	public int getIdReceive() {
		return idReceive;
	}

	public String getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public String getFileName() {
		return fileName;
	}

	public StatusMess getStatus() {
		return status;
	}

	// Setters
	public void setDateUpd(String dateUpd) {
		this.dateUpd = dateUpd;
	}

	public void setStatus(StatusMess status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Message{" + "idMessage=" + idMessage + ", idSend=" + idSend + ", idReceive=" + idReceive + ", type='"
				+ type + '\'' + ", content='" + content + '\'' + ", dateAdd='" + dateAdd + '\'' + ", dateUpd='"
				+ dateUpd + '\'' + ", status=" + status + ", urlMedia='" + fileName + '\'' + '}';
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
