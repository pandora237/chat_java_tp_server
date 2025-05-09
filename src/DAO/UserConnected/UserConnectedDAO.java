package DAO.UserConnected;

import java.util.List;
import org.json.JSONObject;

import com.chat_java_tp.Model.UserConnected;

public interface UserConnectedDAO {
    void addUserConnected(UserConnected userConnected);
    UserConnected getUserConnected(int id);
    List<UserConnected> getAllUserConnected();
    void updateUserConnected(UserConnected userConnected);
    void deleteUserConnected(int id);
    JSONObject getUserConnectedFormatted(int id);
    List<JSONObject> getAllUserConnectedFormatted(); 
}
