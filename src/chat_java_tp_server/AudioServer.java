package chat_java_tp_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.chat_java_tp_server.helpers.ConfigEnv;
import com.chat_java_tp_server.helpers.Helpers;

public class AudioServer {

	private static int PORT_AUDIO_SEND;
	private static int PORT_AUDIO_RECEIVE;
	private static Set<Socket> clientSockets_audio_send = Collections.synchronizedSet(new HashSet<>());
	private static Set<Socket> clientSockets_audio_receive = Collections.synchronizedSet(new HashSet<>());
	private static ServerSocket serverSocket_audio_send = null;
	private static ServerSocket serverSocket_audio_receive = null;
	public static AtomicBoolean running_audio = new AtomicBoolean(false);
	private Thread serverThread_audio_send;
	private Thread serverThread_audio_receive;
	private static final int bufferSize = 8048;
	ServerChat serverChat;

	public AudioServer(ConfigEnv config_env, ServerChat serverChat) {
		PORT_AUDIO_SEND = Integer.parseInt(config_env.get("PORT_AUDIO"));
		PORT_AUDIO_RECEIVE = PORT_AUDIO_SEND + 1;
		this.serverChat = serverChat;
	}

	void startServer() throws IOException {
		if (running_audio.get()) {
			System.out.println("Le serveur Audio est déjà en cours d'exécution.");
			return;
		}

		running_audio.set(true);

		try {
			serverSocket_audio_send = new ServerSocket(PORT_AUDIO_SEND);
			serverSocket_audio_receive = new ServerSocket(PORT_AUDIO_RECEIVE);
		} catch (IOException e) {
			e.printStackTrace();
			stopServer();
			return;
		}

		serverThread_audio_send = new Thread(() -> {
			serverChat.updateLabel(
					"Statut : Serveur Audio en cours sur les ports " + PORT_AUDIO_SEND + " et " + PORT_AUDIO_RECEIVE,
					true, running_audio.get());

			while (running_audio.get()) {
				try {
					Socket clientSocket = serverSocket_audio_send.accept();
					synchronized (getClientSockets_audio_send()) {
						getClientSockets_audio_send().add(clientSocket);
					}
					System.out.println("Client audio (envoi) connecté : " + clientSocket);
					new Thread(() -> handleClient(clientSocket, true)).start();
				} catch (IOException e) {
					if (running_audio.get()) {
						e.printStackTrace();
					} else {
						System.out.println("Serveur Audio arrêté.");
					}
				}
			}
		});

		serverThread_audio_receive = new Thread(() -> {
			while (running_audio.get()) {
				try {
					Socket clientSocket = serverSocket_audio_receive.accept();
					synchronized (clientSockets_audio_receive) {
						clientSockets_audio_receive.add(clientSocket);
					}
					System.out.println("Client audio (réception) connecté : " + clientSocket);
					new Thread(() -> handleClient(clientSocket, false)).start();
				} catch (IOException e) {
					if (running_audio.get()) {
						e.printStackTrace();
					} else {
						System.out.println("Serveur Audio arrêté.");
					}
				}
			}
		});

		serverThread_audio_send.start();
		serverThread_audio_receive.start();
	}

	void stopServer() {
		if (!running_audio.get()) {
			System.out.println("Le serveur Audio est déjà arrêté.");
			return;
		}

		running_audio.set(false);
		System.out.println("Arrêt du serveur Audio...");
		serverChat.updateLabel("Statut : Serveur Audio arrêté", true, running_audio.get());

		try {
			if (serverSocket_audio_send != null) {
				serverSocket_audio_send.close();
			}
			if (serverSocket_audio_receive != null) {
				serverSocket_audio_receive.close();
			}

			synchronized (getClientSockets_audio_send()) {
				for (Socket clientSocket : getClientSockets_audio_send()) {
					clientSocket.close();
				}
				getClientSockets_audio_send().clear();
			}

			synchronized (clientSockets_audio_receive) {
				for (Socket clientSocket : clientSockets_audio_receive) {
					clientSocket.close();
				}
				clientSockets_audio_receive.clear();
			}

			System.out.println("Serveur Audio arrêté avec succès.");
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			if (serverThread_audio_send != null) {
				serverThread_audio_send.interrupt();
				serverThread_audio_send.join();
			}
			if (serverThread_audio_receive != null) {
				serverThread_audio_receive.interrupt();
				serverThread_audio_receive.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void handleClient(Socket sender, boolean is_sender) {
		try {
			InputStream in = sender.getInputStream();
			byte[] dataBuffer = new byte[bufferSize];

			int bytesRead;
			while ((bytesRead = in.read(dataBuffer)) != -1) {
				String dataString = new String(dataBuffer, 0, bytesRead, StandardCharsets.UTF_8);

				Set<Socket> clientSet = is_sender ? getClientSockets_audio_send() : clientSockets_audio_receive;

				synchronized (clientSet) {
					Set<Socket> toRemove = new HashSet<>();
					for (Socket clientSocket : clientSet) {
						if (clientSocket.equals(sender)) {
							continue;
						}

						try {
							OutputStream out = clientSocket.getOutputStream();
							out.write(dataBuffer, 0, bytesRead);
							out.flush();
						} catch (IOException e) {
							System.err.println("Erreur lors de l'envoi audio au client : " + clientSocket);
							e.printStackTrace();
							toRemove.add(clientSocket);
						}

						if (dataString.contains(Helpers.endCallType)) {
							System.out.println("Fin d'appel détectée pour : " + clientSocket);
							toRemove.add(clientSocket);
						}
					}

					for (Socket socketToRemove : toRemove) {
						try {
							socketToRemove.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						clientSet.remove(socketToRemove);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Client audio déconnecté : " + sender);
		} finally {
			synchronized (is_sender ? getClientSockets_audio_send() : clientSockets_audio_receive) {
				getClientSockets_audio_send().remove(sender);
			}
			try {
				sender.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static Set<Socket> getClientSockets_audio_send() {
		return clientSockets_audio_send;
	}

	public static void setClientSockets_audio_send(Set<Socket> clientSockets_audio_send) {
		AudioServer.clientSockets_audio_send = clientSockets_audio_send;
	}

}
