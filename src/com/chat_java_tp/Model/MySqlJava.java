package com.chat_java_tp.Model;

import java.sql.*;
import java.util.*;
import org.json.JSONObject;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MySqlJava {

	private static final String URL = "jdbc:mysql://localhost:3306/chat_tp_group";
	private static final String USER = "root";
	private static final String PASSWORD = "";
	private static Connection connection = null;

	public MySqlJava() {
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

	public List<JSONObject> executeSelectQuery(String query) {
		List<JSONObject> resultsList = new ArrayList<>();

		try (Connection conn = getInstance();
				PreparedStatement stmt = conn.prepareStatement(query);
				ResultSet rs = stmt.executeQuery()) {

			// Récupérer les métadonnées pour gérer les colonnes dynamiquement
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();

			while (rs.next()) {
				JSONObject rowObject = new JSONObject();
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metaData.getColumnName(i);
					Object columnValue = rs.getObject(i); // Obtenir la valeur de la colonne
					rowObject.put(columnName, columnValue); // Ajouter la colonne à l'objet JSON
				}
				resultsList.add(rowObject); // Ajouter l'objet JSON à la liste
			}

		} catch (SQLException e) {
			System.err.println("Erreur lors de l'exécution de la requête SELECT : " + e.getMessage());
		}

		return resultsList; // Retourner la liste des résultats
	}

	// Méthode pour exécuter une requête INSERT/UPDATE/DELETE
	public void executeUpdateQuery(String query) {
		try (Connection conn = getInstance(); PreparedStatement stmt = conn.prepareStatement(query)) {

			int rowsAffected = stmt.executeUpdate();
			System.out.println("Nombre de lignes affectées : " + rowsAffected);

		} catch (SQLException e) {
			System.err.println("Erreur lors de l'exécution de la requête UPDATE : " + e.getMessage());
		}
	}
}