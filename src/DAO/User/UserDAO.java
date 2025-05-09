package DAO.User;

import java.util.List;

import org.json.JSONObject;

import com.chat_java_tp.Model.User;


public interface UserDAO {
	void addUser(User user);

	User getUser(int id);

	List<User> getAllUsers();

	void updateUser(User user);

	void deleteUser(int id);

	JSONObject getUserFormated(int id);

	List<JSONObject> getAllUsersFormated();

	User getUserByUsernamePass(String username, String pass);
}