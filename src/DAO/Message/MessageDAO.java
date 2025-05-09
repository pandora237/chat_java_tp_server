package DAO.Message;

import java.util.List;

import org.json.JSONObject;

import com.chat_java_tp.Model.Message;

public interface MessageDAO {
	void addMessage(Message message);

	Message getMessage(int id);

	JSONObject getMessageFormated(int id);

	List<Message> getAllMessage();

	List<JSONObject> getAllMessageFormated();

	void updateMessage(MessageDAO message);

	void deleteMessage(int id);

	Message getLastMessage();
}
