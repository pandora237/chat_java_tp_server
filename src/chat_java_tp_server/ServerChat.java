package chat_java_tp_server;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean; 

import com.chat_java_tp.Model.MySqlJava;

public class ServerChat extends Application {
	
	    private static final int PORT = 8081;
	    private static Set<Socket> clientSockets = Collections.synchronizedSet(new HashSet<>());
	    private static ServerSocket serverSocket = null;
	    private static AtomicBoolean running = new AtomicBoolean(false);
	    private Thread serverThread;
	    private Label statusLabel;

	    public static void main(String[] args) {
	    	MySqlJava mySqlJava = new MySqlJava(); 
//		    mySqlJava.executeSelectQuery("SELECT * FROM message"); 
	    	Application.launch(args);
	    }

	    @Override
	    public void start(Stage primaryStage) {
	        primaryStage.setTitle("Chat Server");

	        // Label pour afficher le statut
	        statusLabel = new Label("Statut : Serveur arrêté");

	        // Bouton pour démarrer le serveur
	        Button startButton = new Button("Démarrer le serveur");
	        startButton.setOnAction(event -> startServer());

	        // Bouton pour arrêter le serveur
	        Button stopButton = new Button("Arrêter le serveur");
	        stopButton.setOnAction(event -> stopServer());

	        // Désactivation initiale du bouton "Arrêter"
	        stopButton.setDisable(true);

	        // Gestion de l'état des boutons
	        startButton.setDisable(running.get());
	        stopButton.setDisable(!running.get());

	        startButton.setOnAction(event -> {
	            startServer();
	            startButton.setDisable(true);
	            stopButton.setDisable(false);
	        });

	        stopButton.setOnAction(event -> {
	            stopServer();
	            stopButton.setDisable(true);
	            startButton.setDisable(false);
	        });

	        // Disposition de la fenêtre
	        VBox layout = new VBox(10, statusLabel, startButton, stopButton);
	        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

	        // Configuration de la scène
	        Scene scene = new Scene(layout, 300, 200);
	        primaryStage.setScene(scene);
	        primaryStage.show();

	        // Gestion de la fermeture de la fenêtre
	        primaryStage.setOnCloseRequest(event -> {
	            stopServer();
	            Platform.exit();
	            System.exit(0);
	        });
	    }

	    private void startServer() {
	        if (running.get()) {
	            System.out.println("Le serveur est déjà en cours d'exécution.");
	            return;
	        }

	        running.set(true);
	        serverThread = new Thread(() -> {
	            try {
	                serverSocket = new ServerSocket(PORT);
	                Platform.runLater(() -> statusLabel.setText("Statut : Serveur en cours sur le port " + PORT));
	                System.out.println("Serveur démarré sur le port " + PORT);

	                while (running.get()) {
	                    try {
	                        Socket clientSocket = serverSocket.accept();
	                        clientSockets.add(clientSocket);
	                        System.out.println("Client connecté : " + clientSocket.getInetAddress());

	                        new Thread(() -> handleClient(clientSocket)).start();
	                    } catch (IOException e) {
	                        if (running.get()) {
	                            e.printStackTrace();
	                        } else {
	                            System.out.println("Serveur arrêté.");
	                        }
	                    }
	                }
	            } catch (IOException e) {
	                e.printStackTrace();
	            } finally {
	                stopServer();
	            }
	        });
	        serverThread.start();
	    }

	    private void stopServer() {
	        if (!running.get()) {
	            System.out.println("Le serveur est déjà arrêté.");
	            return;
	        }

	        running.set(false);
	        System.out.println("Arrêt du serveur...");
	        Platform.runLater(() -> statusLabel.setText("Statut : Serveur arrêté"));

	        try {
	            if (serverSocket != null) {
	                serverSocket.close();
	            }

	            synchronized (clientSockets) {
	                for (Socket clientSocket : clientSockets) {
	                    clientSocket.close();
	                }
	                clientSockets.clear();
	            }
	            System.out.println("Serveur arrêté avec succès.");
	        } catch (IOException e) {
	            e.printStackTrace();
	        }

	        // Attendre que le thread du serveur se termine
	        try {
	            if (serverThread != null) {
	                serverThread.join();
	            }
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }

	    private void handleClient(Socket clientSocket) {
	        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

	            String message;
	            while ((message = in.readLine()) != null) {
	                System.out.println("Message reçu : " + message);
	                broadcastMessage(message, clientSocket);
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        } finally {
	            clientSockets.remove(clientSocket);
	            try {
	                clientSocket.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    private void broadcastMessage(String message, Socket sender) {
	        synchronized (clientSockets) {
	            for (Socket clientSocket : clientSockets) {
	                if (clientSocket != sender) {
	                    try {
	                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
	                        System.out.println("Le server as recu le message : "+ message + " ::: De :" + sender);
	                        out.println(message);
	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                }
	            }
	        }
	    }
	

}
