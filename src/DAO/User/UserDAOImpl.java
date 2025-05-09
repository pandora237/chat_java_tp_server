package DAO.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.chat_java_tp.Model.User; 

import DB.MySqlJavaDB;

public class UserDAOImpl implements UserDAO {
	private MySqlJavaDB dbManager;

	public UserDAOImpl(MySqlJavaDB dbManager) {
		this.dbManager = dbManager;
	}

	@Override
	public void addUser(User user) {
		String sql = "INSERT INTO user (firstname, lastname, username, password, picture, sexe, phone, dateAdd) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
		dbManager.executeUpdateQuery(sql, user.getFirstname(), user.getLastname(), user.getUsername(),
				user.getPassword(), user.getPicture(), user.getSexe(), user.getPhone());
	}

	@Override
	public User getUser(int id) {
		String sql = "SELECT * FROM user WHERE idUser = ?";
		Connection conn = MySqlJavaDB.getInstance();
		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setObject(1, id);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return new User(dbManager.resultSetToJSON(rs)); 
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public User getUserByUsernamePass(String username, String pass) {
		String sql = "SELECT  u.*,uc.isLogged, uc.lastConnection,uc.ipAdresse,uc.port, uc.id AS idConnection "
				+ "FROM `user` u "
				+ "LEFT JOIN (SELECT * FROM userconected uc1 WHERE uc1.lastConnection =(SELECT MAX(uc2.lastConnection) FROM userconected uc2 "
				+ "					   WHERE uc2.idUser = uc1.idUser )) uc "
				+ "ON u.idUser = uc.idUser WHERE u.username = ? AND u.password = ? LIMIT 1; ";

		Connection conn = MySqlJavaDB.getInstance();
		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setObject(1, username);
			stmt.setObject(2, pass);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return new User(dbManager.resultSetToJSON(rs));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<User> getAllUsers() {
		String sql = "SELECT  u.*,uc.isLogged, uc.lastConnection,uc.ipAdresse,uc.port, uc.id AS idConnection "
				+ "FROM `user` u "
				+ "LEFT JOIN (SELECT * FROM userconected uc1 WHERE uc1.lastConnection =(SELECT MAX(uc2.lastConnection) FROM userconected uc2 "
				+ "					   WHERE uc2.idUser = uc1.idUser )) uc "
				+ "ON u.idUser = uc.idUser ORDER BY uc.lastConnection DESC; ";
		List<User> list = new ArrayList<>();

		try {
			Connection conn = MySqlJavaDB.getInstance();
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				list.add(new User(dbManager.resultSetToJSON(rs)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public void updateUser(User user) {
		String sql = "UPDATE user SET firstname = ?, lastname = ?, username = ?, password = ?, picture = ?, sexe = ?, phone = ? WHERE idUser = ?";
		dbManager.executeUpdateQuery(sql, user.getFirstname(), user.getLastname(), user.getUsername(),
				user.getPassword(), user.getPicture(), user.getSexe(), user.getPhone(), user.getIdUser());
	}

	@Override
	public void deleteUser(int id) {
		dbManager.executeUpdateQuery("DELETE FROM user WHERE idUser = ?", id);
	}

	@Override
	public JSONObject getUserFormated(int id) {
		User user = getUser(id);
		return user != null ? user.toJson() : null;
	}

	@Override
	public List<JSONObject> getAllUsersFormated() {
		List<JSONObject> list = new ArrayList<>();
		for (User u : getAllUsers()) {
			list.add(u.toJson());
		}
		return list;
	}
}