package DB;

import java.sql.*;
import java.util.*;
import org.json.JSONObject;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlJavaDB {

	private static final String URL = "jdbc:mysql://localhost:3306/chat_tp_group";
	private static final String USER = "root";
	private static final String PASSWORD = "";
	private static Connection connection = null;

	public MySqlJavaDB() {
	}

	private static void ensureConnection() {
		try {
			if (connection == null || connection.isClosed()) {
				connection = DriverManager.getConnection(URL, USER, PASSWORD);
				System.out.println("Connexion rétablie avec succès.");
			}
		} catch (SQLException e) {
			System.err.println("Erreur lors de la réinitialisation de la connexion : " + e.getMessage());
		}
	}

	public static Connection getInstance() {
		ensureConnection();
		return connection;
	}

	public List<JSONObject> executeSelectQueryFormatted(String query, Object... params) {
		List<JSONObject> resultsList = new ArrayList<>();

		try {
			ResultSet rs = executeSelectQuery(query, params);
			ResultSetMetaData metaData = rs.getMetaData();
			while (rs.next()) {
				resultsList.add(resultSetToJSON(rs));
			}

		} catch (SQLException e) {
			System.err.println("Erreur lors de l'exécution de la requête SELECT : " + e.getMessage());
		}

		return resultsList;
	}

	public JSONObject resultSetToJSON(ResultSet rs) {
		JSONObject rowObject = new JSONObject();
		try {
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();

			for (int i = 1; i <= columnCount; i++) {
				String columnName = metaData.getColumnLabel(i);
				Object columnValue = rs.getObject(i);
				rowObject.put(columnName, columnValue);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rowObject;
	}

	private ResultSet executeSelectQuery(String query, Object... params) {
		ResultSet rs = null;
		try (Connection conn = getInstance(); PreparedStatement stmt = conn.prepareStatement(query)) {
			for (int i = 0; i < params.length; i++) {
				stmt.setObject(i + 1, params[i]);
			}
			rs = stmt.executeQuery();
		} catch (SQLException e) {
			System.err.println("Erreur lors de l'exécution de la requête SELECT : " + e.getMessage());
		}
		return rs;

	}

	// Méthode pour exécuter une requête INSERT/UPDATE/DELETE
	public void executeUpdateQuery(String query, Object... params) {
		try (Connection conn = getInstance(); PreparedStatement stmt = conn.prepareStatement(query)) {

			for (int i = 0; i < params.length; i++) {
				stmt.setObject(i + 1, params[i]);
			}
			int rowsAffected = stmt.executeUpdate();
			System.out.println("Nombre de lignes affectées : " + rowsAffected);

		} catch (SQLException e) {
			System.err.println("Erreur lors de l'exécution de la requête UPDATE : " + e.getMessage());
		}
	}

}