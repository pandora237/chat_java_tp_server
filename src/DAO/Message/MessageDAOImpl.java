package DAO.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.chat_java_tp.Model.Message;
import com.chat_java_tp_server.helpers.Helpers.StatusMess;

import DB.MySqlJavaDB;

public class MessageDAOImpl implements MessageDAO {
	private MySqlJavaDB managerDB;

	public MessageDAOImpl(MySqlJavaDB managerDB) {
		this.managerDB = managerDB;
	}

	@Override
	public void addMessage(Message message) {
		String query = "INSERT INTO message (dateAdd, idSend, idReceive, type, content, fileName, status) "
				+ "VALUES (NOW(), ?, ?, ?, ?, ?, ?)";

		managerDB.executeUpdateQuery(query, message.getIdSend(), message.getIdReceive(), message.getType(),
				message.getContent(), message.getFileName(), message.getStatus().toString());
	}

	@Override
	public Message getMessage(int id) {
		String query = "SELECT * FROM message WHERE idMessage = ?";

		try {
			Connection conn = MySqlJavaDB.getInstance();
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setObject(1, id);
			ResultSet rs = stmt.executeQuery();
			if (rs != null && rs.next()) {
				return buildMessageFromResultSet(rs);
			}
			;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public Message getLastMessage() {
		String query = "SELECT * FROM message ORDER BY idMessage DESC LIMIT 1";

		try {
			Connection conn = MySqlJavaDB.getInstance();
			PreparedStatement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery();
			if (rs != null && rs.next()) {
				return buildMessageFromResultSet(rs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public List<Message> getAllMessage() {
		List<Message> list = new ArrayList<>();
		String query = "SELECT * FROM message";

		try {
			Connection conn = MySqlJavaDB.getInstance();
			PreparedStatement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery();
			while (rs != null && rs.next()) {
				list.add(buildMessageFromResultSet(rs));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	public List<Message> getAllMessageById(int idSend, int idReceive) {
		List<Message> list = new ArrayList<>();
		String query = "SELECT * FROM message WHERE ( idSend=? AND idReceive=? ) OR ( idSend=? AND idReceive=? )";

		try {
			Connection conn = MySqlJavaDB.getInstance();
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setObject(1, idSend);
			stmt.setObject(2, idReceive);
			stmt.setObject(3, idReceive);
			stmt.setObject(4, idSend);
			ResultSet rs = stmt.executeQuery();
			while (rs != null && rs.next()) {
				list.add(buildMessageFromResultSet(rs));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public void updateMessage(MessageDAO messageDao) {
		if (!(messageDao instanceof Message))
			return;
		Message message = (Message) messageDao;

		String query = String.format(
				"UPDATE message SET idSend = %d, idReceive = %d, type = '%s', content = '%s', "
						+ "fileName = '%s', status = '%s', dateUpd = NOW() WHERE idMessage = %d",
				message.getIdSend(), message.getIdReceive(), message.getType(), message.getContent(),
				message.getFileName(), message.getStatus().toString(), message.getIdMessage());

		managerDB.executeUpdateQuery(query);
	}

	@Override
	public void deleteMessage(int id) {
		String query = "DELETE FROM message WHERE idMessage = " + id;
		managerDB.executeUpdateQuery(query);
	}

	@Override
	public JSONObject getMessageFormated(int id) {
		Message msg = getMessage(id);
		if (msg != null) {
			return buildJsonFromMessage(msg);
		}
		return null;
	}

	public List<JSONObject> getAllMessagesByIdFormated(int idSend, int idReceive) {
		List<JSONObject> list = new ArrayList<>();
		for (Message msg : getAllMessageById(idSend, idReceive)) {
			list.add(buildJsonFromMessage(msg));
		}
		return list;
	}

	@Override
	public List<JSONObject> getAllMessageFormated() {
		List<JSONObject> list = new ArrayList<>();
		for (Message msg : getAllMessage()) {
			list.add(buildJsonFromMessage(msg));
		}
		return list;
	}

	private Message buildMessageFromResultSet(ResultSet rs) throws SQLException {
		Message msg = new Message(rs.getString("dateAdd"), null, rs.getInt("idMessage"), rs.getInt("idSend"),
				rs.getInt("idReceive"), rs.getString("type"), rs.getString("content"), rs.getString("fileName"), null,
				null);
		msg.setFileName(rs.getString("fileName"));
		msg.setStatus(StatusMess.valueOf(rs.getString("status")));
		return msg;
	}

	public JSONObject buildJsonFromMessage(Message msg) {
		JSONObject obj = new JSONObject();
		obj.put("idMessage", msg.getIdMessage());
		obj.put("idSend", msg.getIdSend());
		obj.put("idReceive", msg.getIdReceive());
		obj.put("type", msg.getType());
		obj.put("content", msg.getContent());
		obj.put("fileName", msg.getFileName());
		obj.put("status", msg.getStatus().toString());
		obj.put("dateAdd", msg.getDateAdd());
		return obj;
	}
}
