package com.chat_java_tp.Model;

import java.sql.*;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlJava {
     
    private static final String URL = "jdbc:mysql://localhost:3306/chat_tp_group";  
    private static final String USER = "root";  
    private static final String PASSWORD = ""; 
    private static Connection connection = null;
    
    public MySqlJava() {
    	
    }
    
 
    public static Connection getInstance() {
        if (connection == null) {
            synchronized (MySqlJava.class) { // Bloc synchronized pour gérer les accès concurrents
                if (connection == null) {  
                    try {
                        connection = DriverManager.getConnection(URL, USER, PASSWORD);
                        System.out.println("Connexion établie avec succès.");
                    } catch (SQLException e) {
                        System.err.println("Erreur lors de la connexion à la base de données : " + e.getMessage());
                    }
                }
            }
        }
        return connection;
    }
 
    public ResultSet  executeSelectQuery(String query) {
        try (Connection conn = getInstance();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            // Parcourir les résultats
            while (rs.next()) {
                System.out.println("Résultat : " + rs.getString(2)); // Ajustez en fonction de vos colonnes
            }
            return rs;

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'exécution de la requête SELECT : " + e.getMessage());
        }
		return null;
    }

//    // Exemple de méthode pour exécuter une requête INSERT/UPDATE/DELETE
//    public void executeUpdateQuery(String query) {
//        try (Connection conn = getInstance();
//             PreparedStatement stmt = conn.prepareStatement(query)) {
//
//            int rowsAffected = stmt.executeUpdate();
//            System.out.println("Nombre de lignes affectées : " + rowsAffected);
//
//        } catch (SQLException e) {
//            System.err.println("Erreur lors de l'exécution de la requête UPDATE : " + e.getMessage());
//        }
//    }
//
//    public static void main(String[] args) {
//        MySqlJava mySqlJava = new MySqlJava();
//
//        // Exemple d'utilisation : Exécuter une requête SELECT
//        String selectQuery = "SELECT * FROM votre_table"; // Remplacez par votre requête
//        mySqlJava.executeSelectQuery(selectQuery);
//
//        // Exemple d'utilisation : Exécuter une requête INSERT
//        String insertQuery = "INSERT INTO votre_table (colonne1, colonne2) VALUES ('valeur1', 'valeur2')";
//        mySqlJava.executeUpdateQuery(insertQuery);
//    }
}