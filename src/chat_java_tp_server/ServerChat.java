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
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

import com.chat_java_tp.Model.Message;
import com.chat_java_tp.Model.User;
import com.chat_java_tp.Model.UserConnected;
import com.chat_java_tp_server.helpers.ConfigEnv;
import com.chat_java_tp_server.helpers.Helpers;
import com.chat_java_tp_server.helpers.Helpers.StatusMess;

import DAO.Message.MessageDAOImpl;
import DAO.User.UserDAOImpl;
import DAO.UserConnected.UserConnectedDAOImpl;
import DB.MySqlJavaDB;

public class ServerChat extends Application {

	private static int PORT_MESSAGE;
	private static Set<Socket> clientSockets = Collections.synchronizedSet(new HashSet<>());
	private static Map<Integer, Socket> onlineClients = Collections.synchronizedMap(new HashMap<>());
	private static ServerSocket serverSocket = null;
	private static AtomicBoolean running = new AtomicBoolean(false);
	private Thread serverThread;
	private Label statusLabel;
	private static MySqlJavaDB dbManager;
	private static MessageDAOImpl messageDAOImpl;
	private static UserDAOImpl userDAOImpl;
	private static UserConnectedDAOImpl userConnectedDAOImpl;
	private static final String FILE_STORAGE = "server_files";

	// Variables audio

	private static Map<Integer, AudioServer> listAudioServer = Collections.synchronizedMap(new HashMap<>());

	private static Map<Integer, VideoServer> listVideoServer = Collections.synchronizedMap(new HashMap<>());

	// Video
	protected static final int bufferSize = 8048;
	private static ConfigEnv config_env;

	Button startButton;
	Button stopButton;

	public static void main(String[] args) {
		dbManager = new MySqlJavaDB();
		messageDAOImpl = new MessageDAOImpl(dbManager);
		userDAOImpl = new UserDAOImpl(dbManager);
		userConnectedDAOImpl = new UserConnectedDAOImpl(dbManager);
		config_env = new ConfigEnv();
		PORT_MESSAGE = Integer.parseInt(config_env.get("PORT_MESSAGE"));
//		    dbManager.executeSelectQuery("SELECT * FROM message"); 
		Application.launch(args);
	}

	private void updateBtn(int type, boolean is_not_running) {
		switch (type) {
		case 1: {
			startButton.setDisable(is_not_running);
			stopButton.setDisable(!is_not_running);
		}
		default:
			stopButton.setDisable(is_not_running);
			startButton.setDisable(!is_not_running);

		}
	}

	private Scene initView() {
		statusLabel = new Label("Statut : Serveur arrêté");

		startButton = createServerButton("Démarrer le serveur", this::startServer);
		stopButton = createServerButton("Arrêter le serveur", this::stopServer);

		updateBtn(1, true);

		VBox layout = new VBox(10, statusLabel, startButton, stopButton);
		layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
		return new Scene(layout, 400, 300);
	}

	private Button createServerButton(String text, Runnable action) {
		Button button = new Button(text);
		button.setOnAction(event -> {
			action.run();
		});
		return button;
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
		updateBtn(1, false);
		if (running.get()) {
			System.out.println("Le serveur est déjà en cours d'exécution.");
			return;
		}

		running.set(true);
		serverThread = new Thread(() -> {
			try {
				serverSocket = new ServerSocket(PORT_MESSAGE);
				Platform.runLater(() -> statusLabel.setText("Statut : Serveur en cours sur le port " + PORT_MESSAGE));
				System.out.println("Serveur démarré sur le port " + PORT_MESSAGE);

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
			updateBtn(1, false);
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
		updateBtn(1, true);
	}

	private void handleClient(Socket clientSocket) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

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
			int idUser = currentDatas.optInt("idUser", 0);
			int idReceive = currentDatas.optInt("idReceive", 0);
			System.out.println("Action : " + action);

			if ("get_messages".equals(action)) {
				handleGetMessages(outCurrentClient);
			} else if (Helpers.sendFile.equals(action)) {
				handleSendFile(currentDatas, idUser, idReceive);
			} else if (Helpers.askFile.equals(action)) {
				handleAskFile(currentDatas, idUser, idReceive);
			} else if (Helpers.emoji.equals(action)) {
				handleEmoji(currentDatas, sender, idUser, idReceive);
			} else if (Helpers.audioType.equals(action)) {
				handleAudioMessageSignal(currentDatas, sender, idUser, idReceive);
			} else if (Helpers.audioTypeReceiver.equals(action)) {
				handleAudioMessageSignalReceive(currentDatas, sender, idUser, idReceive);
			} else if (Helpers.videoType.equals(action)) {
				handleVideoMessageSignal(currentDatas, sender, idUser, idReceive);
			} else if (Helpers.videoTypeReceiver.equals(action)) {
				handleVideoMessageSignalReceive(currentDatas, sender, idUser, idReceive);
			} else if (Helpers.endCallType.equals(action)) {
				handleEndCall(currentDatas, sender, idUser, idReceive);
			} else if (Helpers.login.equals(action)) {
				handleLogin(currentDatas, sender, outCurrentClient);
			} else if (Helpers.logout.equals(action)) {
				handleLogout(currentDatas, sender, outCurrentClient);
			} else if (Helpers.getMessUserSendReceive.equals(action)) {
				handleGetMessUserSendReceive(currentDatas, idUser, idReceive, outCurrentClient);
			} else {
				handleDefaultMessage(currentDatas, sender, idUser, idReceive);
			}
		} catch (Exception e) {
			System.err.println("Erreur lors du traitement du message : " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void handleGetMessUserSendReceive(JSONObject currentDatas, int idUser, int idReceive,
			PrintWriter outCurrentClient) {
		List<JSONObject> mess = messageDAOImpl.getAllMessagesByIdFormated(idUser, idReceive);
		System.out.println(mess);
		currentDatas.put("messages", mess);
		synchronized (onlineClients) {
			outCurrentClient.println(formateResponse(true, Helpers.getMessUserSendReceive, currentDatas, null));
		}
	}

	// méthode pour envoyer les anciens messages
	private void sendOldMessages(PrintWriter out) {
		// Requête SQL pour récupérer les anciens messages
//		List<JSONObject> messages = dbManager.executeSelectQuery("SELECT * FROM message");
		List<JSONObject> messages = messageDAOImpl.getAllMessageFormated();
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

	private void handleGetMessages(PrintWriter outCurrentClient) {
		List<JSONObject> result = messageDAOImpl.getAllMessageFormated();
		outCurrentClient.println(formateResponse(true, "get_messages", null, result.toString()));
	}

	private void handleSendFile(JSONObject currentDatas, int idUser, int idReceive) {
		String fileName = currentDatas.getString("fileName");
		byte[] fileContent = Base64.getDecoder().decode(currentDatas.getString("fileContent"));

		/* save */
		File downloadedFile = new File(Helpers.FILE_DOWNLOAD + fileName);
		downloadedFile.getParentFile().mkdirs();// Créer le dossier si nécessaire

		try (FileOutputStream fos = new FileOutputStream(downloadedFile)) {
			fos.write(fileContent);
		} catch (IOException e) {
			e.printStackTrace();
		}

		currentDatas.put("status", StatusMess.DELIVERED);
		if (currentDatas != null) {
			messageDAOImpl.addMessage(new Message(currentDatas));
		}
		JSONObject mess = messageDAOImpl.buildJsonFromMessage(messageDAOImpl.getLastMessage());

		boolean isSent = false;
		currentDatas.remove("fileContent");

		synchronized (onlineClients) {
			JSONObject fileResponse = new JSONObject();
			fileResponse.put("action", Helpers.sendFile);
			fileResponse.put("datas", currentDatas);

			Socket receive = onlineClients.get(idReceive);
			Socket sender = onlineClients.get(idUser);
			try {
				if (receive != null) {
					PrintWriter out = new PrintWriter(receive.getOutputStream(), true);
					out.println(formateResponse(true, Helpers.sendFile, mess, null));
				}

				if (sender != null) {
					PrintWriter out_sender = new PrintWriter(sender.getOutputStream(), true);
					out_sender.println(formateResponse(true, Helpers.sendFile, mess, null));
				}
				isSent = true;
			} catch (IOException e) {
				System.err.println("Erreur lors de l'envoi du fichier au client.");
				e.printStackTrace();
			}
		}
	}

	private void handleAskFile(JSONObject currentDatas, int idUser, int idReceive) {
		int idMessage = currentDatas.getInt("idMessage");
		JSONObject mess = messageDAOImpl.getMessageFormated(idMessage);
		String fileName = mess.getString("fileName");

		File fileToSend = new File(Helpers.FILE_DOWNLOAD + fileName);

		if (!fileToSend.exists()) {
			System.err.println("Fichier non trouvé : " + fileToSend.getAbsolutePath());
			return;
		}

		try {
			byte[] fileBytes = Files.readAllBytes(fileToSend.toPath());

			String encodedContent = Base64.getEncoder().encodeToString(fileBytes);

			mess.put("fileContent", encodedContent);
			mess.put("status", StatusMess.SENT);

			JSONObject response = new JSONObject();
			response.put("action", Helpers.askFile);
			response.put("datas", mess);

			boolean isSent = false;

			synchronized (onlineClients) {
				Socket receive = onlineClients.get(idReceive);
				try {
					PrintWriter out = new PrintWriter(receive.getOutputStream(), true);
					out.println(response.toString());
					isSent = true;
				} catch (IOException e) {
					System.err.println("Erreur lors de l'envoi du fichier au client.");
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			System.err.println("Erreur lors de l'envoi du fichier " + fileName);
			e.printStackTrace();
		}

	}

	private void handleAudioMessageSignal(JSONObject currentDatas, Socket sender, int idUser, int idReceive) {
		broadcastSignal(currentDatas, sender, idUser, idReceive, Helpers.audioType);

		currentDatas.put("status", StatusMess.DELIVERED);
		if (currentDatas != null) {
			messageDAOImpl.addMessage(new Message(currentDatas));
		}
	}

	private void handleEmoji(JSONObject currentDatas, Socket sender, int idUser, int idReceive) {
		broadcastSignal(formateResponse(true, Helpers.emoji, currentDatas, null), sender, idUser, idReceive,
				Helpers.emoji);

		currentDatas.put("status", StatusMess.DELIVERED);
		if (currentDatas != null) {
			messageDAOImpl.addMessage(new Message(currentDatas));
		}
	}

	private void handleAudioMessageSignalReceive(JSONObject currentDatas, Socket sender, int idUser, int idReceive) {
		broadcastSignal(currentDatas, sender, idUser, idReceive, Helpers.audioTypeReceiver);
	}

	private void handleVideoMessageSignalReceive(JSONObject currentDatas, Socket sender, int idUser, int idReceive) {
		broadcastSignal(currentDatas, sender, idUser, idReceive, Helpers.videoTypeReceiver);
	}

	private void handleVideoMessageSignal(JSONObject currentDatas, Socket sender, int idUser, int idReceive) {
		broadcastSignal(currentDatas, sender, idUser, idReceive, Helpers.videoType);
		currentDatas.put("status", StatusMess.DELIVERED);
		if (currentDatas != null) {
			messageDAOImpl.addMessage(new Message(currentDatas));
		}
	}

	private void handleEndCall(JSONObject currentDatas, Socket sender, int idUser, int idReceive) {
		broadcastSignal(currentDatas, sender, idUser, idReceive, Helpers.endCallType);
	}

	private void broadcastSignal(JSONObject currentDatas, Socket sender, int idUser, int idReceive, String type) {
		synchronized (onlineClients) {
			Socket clientSocket = onlineClients.get(idReceive);

			boolean isSent = false;
			try {
				if (clientSocket != null) {
					PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
					out.println(currentDatas);
					isSent = true;
				}
			} catch (IOException e) {
				System.err.println("Erreur lors de l'envoi au client pour l'audio.");
				e.printStackTrace();
			}

//			// callback
//			try {
//				PrintWriter outSender = new PrintWriter(sender.getOutputStream(), true);
//				if (isSent) {
//					currentDatas.put("status", StatusMess.DELIVERED);
//				} else {
//					currentDatas.put("status", StatusMess.SENT);
//				}
//				messageDAOImpl.addMessage(new Message(currentDatas));
//				outSender.println(currentDatas);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
	}

	private void handleLogin(JSONObject currentDatas, Socket sender, PrintWriter outCurrentClient) {

		JSONObject loginResp = new JSONObject();
		if (currentDatas != null) {
			String username = currentDatas.getString("username");
			String password = currentDatas.getString("password");

			// Vérification si l'utilisateur existe et si le mot de passe est correct
			User user = userDAOImpl.getUserByUsernamePass(username, password);

			// Si un utilisateur est trouvé
			if (user != null) {
				// Mise à jour de isLogged à 1 pour cet utilisateur
				userConnectedDAOImpl.addUserConnected(
						new UserConnected(0, user.getIdUser(), sender.getLocalAddress(), sender.getPort(), true, null));
				List<JSONObject> all_users = userDAOImpl.getAllUsersFormated();
				List<JSONObject> conected_users = userConnectedDAOImpl.getAllUserConnectedFormatted();
				loginResp.put("user", user.toJson());
				loginResp.put("all_users", all_users);
				loginResp.put("conected_users", conected_users);
				loginResp.put("messages", messageDAOImpl.getAllMessageFormated());
				int idUser = user.getIdUser();

				synchronized (onlineClients) {
					onlineClients.put(idUser, sender);
					outCurrentClient.println(formateResponse(true, Helpers.login, loginResp, null));
				}

				synchronized (clientSockets) {
					clientSockets.remove(sender);
				}
			} else {
				System.out.println("Aucun utilisateur trouvé");
				outCurrentClient.println(formateResponse(false, Helpers.login, loginResp, null));
			}
		}

		// informe les autres client d'une nouvelle connexion
		synchronized (onlineClients) {
			for (Map.Entry<Integer, Socket> entry : onlineClients.entrySet()) {
				Socket clientSocket = entry.getValue();
				if (!clientSocket.equals(sender)) {
					try {
						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
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

//		synchronized (onlineClients) {
//			for (Map<Integer, Socket> clientSocket : onlineClients) {
//				// Vérifiez que le client actuel n'est pas le client émetteur
//				if (!clientSocket.equals(sender)) {
//					try {
//						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
//						// Envoyer uniquement les informations pertinentes aux autres clients
//						System.out.println("Le serveur informe un autre client : " + clientSocket + " sur : "
//								+ currentDatas.toString());
//						out.println(formateResponse(true, Helpers.otherUserLogged, loginResp, null));
//					} catch (IOException e) {
//						System.err.println("Erreur lors de l'envoi au client.");
//						e.printStackTrace();
//					}
//				}
//			}
//		}
	}

	private void handleLogout(JSONObject currentDatas, Socket sender, PrintWriter outCurrentClient) {
		JSONObject loginResp = new JSONObject();
		if (currentDatas != null) {
			int idUser = currentDatas.getInt("idUser");

			// Vérification si l'utilisateur existe
			User user = userDAOImpl.getUser(idUser);

			// Si un utilisateur est trouvé
			if (user != null) {
				// Mise à jour de isLogged à 0 pour cet utilisateur
				userConnectedDAOImpl.updateUserConnected((new UserConnected(user.getIdConnection(), user.getIdUser(),
						sender.getLocalAddress(), sender.getPort(), false, null)));

//				List<JSONObject> all_users = userConnectedDAOImpl.getAllUserConnectedFormatted();
				List<JSONObject> all_users = userDAOImpl.getAllUsersFormated();
				loginResp.put("user", user.toJson());
				loginResp.put("all_users", all_users);

				synchronized (onlineClients) {
					for (Map.Entry<Integer, Socket> entry : onlineClients.entrySet()) {
						Socket clientSocket = entry.getValue();
						try {
							PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
							// Envoyer les informations aux autres clients
							System.out.println("Le serveur informe un autre client : " + clientSocket + " sur : "
									+ currentDatas.toString());
							out.println(formateResponse(true, Helpers.otherUserLogged, loginResp, null));
						} catch (IOException e) {
							System.err.println("Erreur lors de l'envoi au client.");
							e.printStackTrace();
						}
					}
					onlineClients.remove(idUser);
				}

			} else {
				System.out.println("Echec de la deconnexion");
				outCurrentClient.println(formateResponse(false, Helpers.login, loginResp, null));
			}
		}

	}

	private void handleDefaultMessage(JSONObject currentDatas, Socket sender, int idUser, int idReceive) {
		synchronized (onlineClients) {
			Socket clientSocket = onlineClients.get(idReceive);
			boolean isSent = false;
			if (clientSocket != null && sender != null && !clientSocket.equals(sender)) {
				try {
					PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
					System.out.println("Le serveur a reçu le message : " + currentDatas.toString() + " ::: De : "
							+ sender + "ALL SOCKET::::::::::" + clientSockets);
					out.println(formateResponse(true, null, currentDatas, null));
					isSent = true;
				} catch (IOException e) {
					System.err.println("Erreur lors de l'envoi au client.");
					e.printStackTrace();
				}
			}

			// responses signal
			try {
				PrintWriter outSender = new PrintWriter(sender.getOutputStream(), true);

				if (isSent) {
					currentDatas.put("status", StatusMess.DELIVERED);
				} else {
					currentDatas.put("status", StatusMess.SENT);
				}
				if (currentDatas != null) {
					messageDAOImpl.addMessage(new Message(currentDatas));
				}
				outSender.println(formateResponse(true, Helpers.responseSendMessage, currentDatas, null));
			} catch (IOException e) {
				e.printStackTrace();
			}

//			for (Socket clientSocket : clientSockets) {
//				if (!clientSocket.equals(sender)) {
//					try {
//						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
//						System.out.println("Le serveur a reçu le message : " + currentDatas.toString() + " ::: De : "
//								+ sender + "ALL SOCKET::::::::::" + clientSockets);
//						out.println(formateResponse(true, null, currentDatas, null));
//					} catch (IOException e) {
//						System.err.println("Erreur lors de l'envoi au client.");
//						e.printStackTrace();
//					}
//				}
//			}
		}
	}

	@Override
	public void stop() {
		stopServer();
		Platform.exit();
		System.exit(0);
	}

}
