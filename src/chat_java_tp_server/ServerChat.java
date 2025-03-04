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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.json.JSONObject;

import com.chat_java_tp.Model.MySqlJava;
import com.chat_java_tp_server.helpers.Helpers;

public class ServerChat extends Application {

	private static final int PORT = 8081;
	private static Set<Socket> clientSockets = Collections.synchronizedSet(new HashSet<>());
	private static ServerSocket serverSocket = null;
	private static AtomicBoolean running = new AtomicBoolean(false);
	private Thread serverThread;
	private Label statusLabel;
	public static MySqlJava mySqlJava;
	private static final String FILE_STORAGE = "server_files";

	// Variables audio
	private static final int PORT_AUDIO = 8082;
	private static Set<Socket> clientSockets_audio = Collections.synchronizedSet(new HashSet<>());
	private static ServerSocket serverSocket_audio = null;
	private static AtomicBoolean running_audio = new AtomicBoolean(false);
	private Thread serverThread_audio;
	private Label statusLabelAudio;
	protected static final int bufferSize = 8048;

	public static void main(String[] args) {
		mySqlJava = new MySqlJava();
//		    mySqlJava.executeSelectQuery("SELECT * FROM message"); 
		Application.launch(args);
	}

	private Scene initView() {
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

		/*
		 * Audio items
		 */

		// Label pour afficher le statut Audio
		statusLabelAudio = new Label("Statut : Serveur Audio arrêté");

		// Bouton pour démarrer le serveur Audio
		Button startBtnAudio = new Button("Démarrer le serveur Audio/Video");
		startBtnAudio.setOnAction(event -> startServer());

		// Bouton pour arrêter le serveur Audio
		Button stopBtnAudio = new Button("Arrêter le serveur Audio/Video");
		stopBtnAudio.setOnAction(event -> stopServer());

		// Désactivation initiale du bouton Audio "Arrêter"
		stopBtnAudio.setDisable(true);

		// Gestion de l'état des boutons Audio
		startBtnAudio.setDisable(running.get());
		stopBtnAudio.setDisable(!running.get());

		startBtnAudio.setOnAction(event -> {
			startServerAudioVideo();
			startBtnAudio.setDisable(true);
			stopBtnAudio.setDisable(false);
		});

		stopBtnAudio.setOnAction(event -> {
			stopServerAudio();
			stopBtnAudio.setDisable(true);
			startBtnAudio.setDisable(false);
		});

		// Disposition de la fenêtre
		VBox layout = new VBox(10, statusLabel, startButton, stopButton, statusLabelAudio, startBtnAudio, stopBtnAudio);
		layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

		// Configuration de la scène
		return new Scene(layout, 300, 200);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Chat Server");

		primaryStage.setScene(initView());
		primaryStage.show();

		// Gestion de la fermeture de la fenêtre
		primaryStage.setOnCloseRequest(event -> {
			stopServer();
			Platform.exit();
			System.exit(0);
		});

		createFolderIfNotExists(FILE_STORAGE);
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
						System.out.println("Client connecté : " + clientSocket);

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

			// Envoyer les anciens messages dès la connexion
//			sendOldMessages(out);

			String message = null;
			while ((message = in.readLine()) != null) {
				System.out.println("Message reçu : " + message);

				if (message != null && !message.isEmpty()) {
					broadcastMessage(message, clientSocket, out);
				} else {
					System.out.println("Message vide reçu. Aucun traitement effectué.");
				}
			}
		} catch (IOException e) {
			System.out.println("Client déconnecté : " + clientSocket);
			clientSockets.remove(clientSocket);
		} finally {
//			clientSockets.remove(clientSocket);
//			try {
//				clientSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
	}

	private void broadcastMessage(String datas, Socket sender, PrintWriter outCurrentClient) {
		try {
			JSONObject currentDatas = new JSONObject(datas);
			String action = currentDatas.optString("action", null);
			System.out.println("Action : " + action);

			if ("get_messages".equals(action)) {
				handleGetMessages(outCurrentClient);
			} else if (Helpers.sendFile.equals(action)) {
				handleSendFile(currentDatas);
			} else if (Helpers.audioType.equals(action)) {
				handleAudioMessageSignal(currentDatas, sender);
			} else if (Helpers.videoType.equals(action)) {
				handleVideoMessageSignal(currentDatas, sender);
			} else if (Helpers.endCallType.equals(action)) {
				handleEndCall(currentDatas, sender);
			} else if (Helpers.login.equals(action)) {
				handleLogin(currentDatas, sender, outCurrentClient);
			} else if (Helpers.logout.equals(action)) {
				handleLogout(currentDatas, sender, outCurrentClient);
			} else {
				handleDefaultMessage(currentDatas, sender);
			}
		} catch (Exception e) {
			System.err.println("Erreur lors du traitement du message : " + e.getMessage());
			e.printStackTrace();
		}
	}

	// méthode pour envoyer les anciens messages
	private void sendOldMessages(PrintWriter out) {
		// Requête SQL pour récupérer les anciens messages
		List<JSONObject> messages = mySqlJava.executeSelectQuery("SELECT * FROM message");
		for (JSONObject message : messages) {
			// Envoyer chaque message au client sous forme de JSON
			out.println(formateResponse(true, "get_messages", message, null));
		}
		System.out.println("Tous les anciens messages ont été envoyés au client : ");
	}

	private void createFolderIfNotExists(String directoryPath) {
		File directory = new File(directoryPath);
		if (!directory.exists()) {
			if (directory.mkdirs()) {
				System.out.println("Répertoire créé : " + directoryPath);
			} else {
				System.err.println("Erreur lors de la création du répertoire : " + directoryPath);
			}
		}
	}

	private JSONObject formateResponse(Boolean success, String action, JSONObject datas, String datasString) {
		JSONObject resp = new JSONObject();
		resp.put("success", success != null ? success : false);
		resp.put("action", action != null ? action : "");
		resp.put("datas", datas != null ? datas : "");
		resp.put("datasString", datasString != null ? datasString : "");

		return resp;
	}

	/*
	 * audio section
	 */

	private void startServerAudioVideo() {
		if (running_audio.get()) {
			System.out.println("Le serveur Audio est déjà en cours d'exécution.");
			return;
		}

		running_audio.set(true);
		serverThread_audio = new Thread(() -> {
			try {
				serverSocket_audio = new ServerSocket(PORT_AUDIO);
				Platform.runLater(
						() -> statusLabelAudio.setText("Statut : Serveur Audio en cours sur le port " + PORT_AUDIO));
				System.out.println("Serveur démarré sur le port " + PORT_AUDIO);

				while (running_audio.get()) {
					try {
						Socket clientSocket = serverSocket_audio.accept();
						clientSockets_audio.add(clientSocket);
						System.out.println("Client audio connecté  : " + clientSocket);

						new Thread(() -> handleClientAudioVideo(clientSocket)).start();
					} catch (IOException e) {
						if (running_audio.get()) {
							e.printStackTrace();
						} else {
							System.out.println("Serveur Audio arrêté.");
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				stopServerAudio();
			}
		});
		serverThread_audio.start();
	}

	private void stopServerAudio() {
		if (!running_audio.get()) {
			System.out.println("Le serveur Audio est déjà arrêté.");
			return;
		}

		running_audio.set(false);
		System.out.println("Arrêt du serveur Audio...");
		Platform.runLater(() -> statusLabelAudio.setText("Statut : Serveur Audio arrêté"));

		try {
			if (serverSocket_audio != null) {
				serverSocket_audio.close();
			}

			synchronized (clientSockets_audio) {
				for (Socket clientSocket : clientSockets_audio) {
					clientSocket.close();
				}
				clientSockets_audio.clear();
			}
			System.out.println("Serveur Audio arrêté avec succès.");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Attendre que le thread du serveur audio se termine
		try {
			if (serverThread_audio != null) {
				serverThread_audio.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void handleClientAudioVideo(Socket sender) {
		try {
			InputStream in = sender.getInputStream();

			byte[] dataBuffer = new byte[bufferSize]; // Taille réduite pour une lecture plus fréquente

			int bytesRead;
			while ((bytesRead = in.read(dataBuffer)) != -1) {
				String dataString = new String(dataBuffer, 0, bytesRead, StandardCharsets.UTF_8);

				synchronized (clientSockets_audio) {
					for (Socket clientSocket : clientSockets_audio) {
						if (!clientSocket.equals(sender)) { // Ne pas renvoyer au client expéditeur
							try {
								OutputStream out = clientSocket.getOutputStream();
								out.write(dataBuffer, 0, bytesRead); // Envoyer les données brutes sans modification
								out.flush(); // Assurez-vous que les données sont envoyées immédiatement
							} catch (IOException e) {
								System.err.println("Erreur lors de l'envoi audio au client : " + clientSocket);
								e.printStackTrace();
							}
						}
						if (dataString.contains(Helpers.endCallType)) {
//							clientSockets_audio.remove(clientSocket);
						}
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Client audio déconnecté : " + sender);
		} finally {
			clientSockets_audio.remove(sender);
			try {
				sender.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleGetMessages(PrintWriter outCurrentClient) {
		List<JSONObject> result = getMessages();
		outCurrentClient.println(formateResponse(true, "get_messages", null, result.toString()));
	}

	private List<JSONObject> getMessages() {
		return mySqlJava.executeSelectQuery(
				"SELECT m.*, u.username AS usernameSend, u.firstname AS firstnameSend, u.lastname AS lastnameSend FROM message AS m  JOIN user AS u on m.idSend=idUser ");
	}

	private void handleSendFile(JSONObject currentDatas) {
		String fileName = currentDatas.getString("fileName");
		String fileContent = currentDatas.getString("fileContent");

		synchronized (clientSockets) {
			for (Socket clientSocket : clientSockets) {
				try {
					PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
					JSONObject fileResponse = new JSONObject();
					fileResponse.put("action", "send_file");
					fileResponse.put("datas", currentDatas);

					out.println(fileResponse.toString());
				} catch (IOException e) {
					System.err.println("Erreur lors de l'envoi du fichier au client.");
					e.printStackTrace();
				}
			}
		}
	}

	private void handleAudioMessageSignal(JSONObject currentDatas, Socket sender) {
		synchronized (clientSockets) {
			for (Socket clientSocket : clientSockets) {
				if (!clientSocket.equals(sender)) {
					try {
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
						JSONObject callResponse = new JSONObject();
						callResponse.put("action", Helpers.audioType);
						callResponse.put("datas", currentDatas);
						out.println(callResponse.toString());
					} catch (IOException e) {
						System.err.println("Erreur lors de l'envoi au client pour l'audio.");
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void handleVideoMessageSignal(JSONObject currentDatas, Socket sender) {
		synchronized (clientSockets) {
			for (Socket clientSocket : clientSockets) {
				if (!clientSocket.equals(sender)) {
					try {
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
						JSONObject callResponse = new JSONObject();
						callResponse.put("action", Helpers.videoType);
						callResponse.put("datas", currentDatas);
						out.println(callResponse.toString());
					} catch (IOException e) {
						System.err.println("Erreur lors de l'envoi au client pour la vidéo.");
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void handleEndCall(JSONObject currentDatas, Socket sender) {
		synchronized (clientSockets) {
			for (Socket clientSocket : clientSockets) {
				if (!clientSocket.equals(sender)) {
					try {
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
						JSONObject callResponse = new JSONObject();
						callResponse.put("action", Helpers.endCallType);
						callResponse.put("datas", currentDatas);
						out.println(callResponse.toString());
					} catch (IOException e) {
						System.err.println("Erreur lors de l'envoi de fin d'appel au client.");
						e.printStackTrace();
					}
				}
			}
		}
		clientSockets_audio.clear();
	}

	private void handleLogin(JSONObject currentDatas, Socket sender, PrintWriter outCurrentClient) {

		JSONObject loginResp = new JSONObject();
		if (currentDatas != null) {
			String username = currentDatas.getString("username");
			String password = currentDatas.getString("password");

			// Vérification si l'utilisateur existe et si le mot de passe est correct
			String selectQuery = "SELECT * FROM `user` WHERE `username` = '" + username + "' AND `password` = '"
					+ password + "'";

			List<JSONObject> resultSet = mySqlJava.executeSelectQuery(selectQuery);

			// Si un utilisateur est trouvé
			if (resultSet != null && !resultSet.isEmpty()) {
				// Mise à jour de isLogged à 1 pour cet utilisateur
				String updateQuery = "UPDATE `user` SET `isLogged` = 1 WHERE `username` = '" + username
						+ "' AND `password` = '" + password + "'";
				mySqlJava.executeUpdateQuery(updateQuery);
				List<JSONObject> all_users = mySqlJava.executeSelectQuery(
						"SELECT idUser, firstname, lastname, username, isLogged, dateAdd, sexe from user");
				loginResp.put("user", resultSet);
				loginResp.put("all_users", all_users);
				loginResp.put("messages", getMessages());
				synchronized (clientSockets) {
					outCurrentClient.println(formateResponse(true, Helpers.login, loginResp, null));
				}
			} else {
				System.out.println("Aucun utilisateur trouvé");
				outCurrentClient.println(formateResponse(false, Helpers.login, loginResp, null));
			}
		}

		synchronized (clientSockets) {
			for (Socket clientSocket : clientSockets) {
				// Vérifiez que le client actuel n'est pas le client émetteur
				if (!clientSocket.equals(sender)) {
					try {
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
						// Envoyer uniquement les informations pertinentes aux autres clients
						System.out.println("Le serveur informe un autre client : " + clientSocket + " sur : "
								+ currentDatas.toString());
						out.println(formateResponse(true, Helpers.otherUserLogged, loginResp, null));
					} catch (IOException e) {
						System.err.println("Erreur lors de l'envoi au client.");
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void handleLogout(JSONObject currentDatas, Socket sender, PrintWriter outCurrentClient) {
		JSONObject loginResp = new JSONObject();
		if (currentDatas != null) {
			int idUser = currentDatas.getInt("idUser");

			// Vérification si l'utilisateur existe et si le mot de passe est correct
			String selectQuery = "SELECT * FROM `user` WHERE `idUser` = " + idUser;

			List<JSONObject> resultSet = mySqlJava.executeSelectQuery(selectQuery);

			// Si un utilisateur est trouvé
			if (resultSet != null && !resultSet.isEmpty()) {
				// Mise à jour de isLogged à 0 pour cet utilisateur
				String updateQuery = "UPDATE `user` SET `isLogged` = 0 WHERE `idUser` = " + idUser;
				mySqlJava.executeUpdateQuery(updateQuery);
				List<JSONObject> all_users = mySqlJava.executeSelectQuery(
						"SELECT idUser, firstname, lastname, username, isLogged, dateAdd, sexe from user");
				loginResp.put("user", resultSet);
				loginResp.put("all_users", all_users);

				synchronized (clientSockets) {
					for (Socket clientSocket : clientSockets) {
						try {
							PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
							// Envoyer uniquement les informations pertinentes aux autres clients
							System.out.println("Le serveur informe un autre client : " + clientSocket + " sur : "
									+ currentDatas.toString());
							out.println(formateResponse(true, Helpers.otherUserLogged, loginResp, null));
						} catch (IOException e) {
							System.err.println("Erreur lors de l'envoi au client.");
							e.printStackTrace();
						}
					}
					clientSockets.remove(sender);
				}

			} else {
				System.out.println("Echec de la deconnexion");
				outCurrentClient.println(formateResponse(false, Helpers.login, loginResp, null));
			}
		}

	}

	private void handleDefaultMessage(JSONObject currentDatas, Socket sender) {
		if (currentDatas != null) {
			String insertQuery = "INSERT INTO message (idSend, idReceive, content) " + "VALUES ("
					+ currentDatas.getInt("idSend") + "," + currentDatas.getInt("idReceive") + ",'"
					+ currentDatas.getString("content") + "')";
			mySqlJava.executeUpdateQuery(insertQuery);
		}

		synchronized (clientSockets) {
			for (Socket clientSocket : clientSockets) {
				if (!clientSocket.equals(sender)) {
					try {
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
						System.out.println("Le serveur a reçu le message : " + currentDatas.toString() + " ::: De : "
								+ sender + "ALL SOCKET::::::::::" + clientSockets);
						out.println(formateResponse(true, null, currentDatas, null));
					} catch (IOException e) {
						System.err.println("Erreur lors de l'envoi au client.");
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public void stop() {
		stopServer();
		stopServerAudio();
		Platform.exit();
		System.exit(0);
	}

}
