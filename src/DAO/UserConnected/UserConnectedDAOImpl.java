package DAO.UserConnected;

import DB.MySqlJavaDB;
import com.chat_java_tp.Model.UserConnected;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserConnectedDAOImpl implements UserConnectedDAO {

	private MySqlJavaDB dbManager;

	public UserConnectedDAOImpl(MySqlJavaDB dbManager) {
		this.dbManager = dbManager;
	}

	@Override
	public void addUserConnected(UserConnected user) {
		String sql = "INSERT INTO userconected (lastConnection, idUser, ipAdresse, port, isLogged) VALUES (NOW(), ?, ?, ?, ?)";
		System.out.println(sql);
		dbManager.executeUpdateQuery(sql, user.getIdUser(), user.getIpAdresse(), user.getPort(),
				user.isLogged() ? 1 : 0);
	}

	@Override
	public UserConnected getUserConnected(int id) {
		String query = "SELECT * FROM userconected WHERE id=?";

		try {
			Connection conn = MySqlJavaDB.getInstance();
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setObject(1, id);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				JSONObject json = new JSONObject();
				json.put("id", rs.getInt("id"));
				json.put("idUser", rs.getInt("idUser"));
				json.put("lastConnection", rs.getString("lastConnection"));
				json.put("ipAdresse", rs.getString("ipAdresse"));
				json.put("port", rs.getInt("port"));
				json.put("isLogged", rs.getInt("isLogged"));

				return new UserConnected(json);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<UserConnected> getAllUserConnected() {
		List<UserConnected> list = new ArrayList<>();
		String sql = "SELECT * FROM userconected WHERE isLogged=1";

		try {
			Connection conn = MySqlJavaDB.getInstance();
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject json = new JSONObject();
				json.put("id", rs.getInt("id"));
				json.put("idUser", rs.getInt("idUser"));
				json.put("lastConnection", rs.getString("lastConnection"));
				json.put("ipAdresse", rs.getString("ipAdresse"));
				json.put("port", rs.getInt("port"));
				json.put("isLogged", rs.getInt("isLogged"));

				list.add(new UserConnected(json));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public void updateUserConnected(UserConnected userConnect) {
		String sql = "UPDATE userconected SET idUser=?, lastConnection=?, ipAdresse=?, port=?, isLogged=? WHERE id=?";
		dbManager.executeUpdateQuery(sql, userConnect.getIdUser(), userConnect.getLastConnection(),
				userConnect.getIpAdresse(), userConnect.getPort(), userConnect.isLogged() ? 1 : 0, userConnect.getId());
	}

	@Override
	public void deleteUserConnected(int id) {
		String sql = "DELETE FROM userconected WHERE id=?";
		dbManager.executeUpdateQuery(sql, new Object[] { id });
	}

	@Override
	public JSONObject getUserConnectedFormatted(int id) {
		UserConnected user = getUserConnected(id);
		if (user != null) {
			return ((UserConnected) user).toJson();
		}
		return null;
	}

	@Override
	public List<JSONObject> getAllUserConnectedFormatted() {
		List<JSONObject> list = new ArrayList<>();
		for (UserConnected u : getAllUserConnected()) {
			list.add(((UserConnected) u).toJson());
		}
		return list;
	}
}
