package chat_java_tp_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.chat_java_tp_server.helpers.ConfigEnv;
import com.chat_java_tp_server.helpers.Helpers;

public class VideoServer extends AudioServer {
	private static int PORT_VIDEO_SEND;
	private static int PORT_VIDEO_RECEIVE;

	private static Set<Socket> clientSockets_video_send = Collections.synchronizedSet(new HashSet<>());
	private static Set<Socket> clientSockets_video_receive = Collections.synchronizedSet(new HashSet<>());
	private static ServerSocket serverSocket_video_send = null;
	private static ServerSocket serverSocket_video_receive = null;
	private static AtomicBoolean running_video = new AtomicBoolean(false);
	private Thread serverThread_video_send;
	private Thread serverThread_video_receive;
	private static final int bufferSize = 16000;

	public VideoServer(ServerChat serverChat) {
		super(serverChat);
	}

	@Override
	public int startServerSender() throws IOException {
		int portAudio = super.startServerSender();
		if (running_video.get()) {
			System.out.println("Le serveur Vidéo est déjà en cours d'exécution.");
			return 0;
		}

		running_video.set(true);

		try {
			serverSocket_video_send = new ServerSocket(0);
			PORT_VIDEO_SEND = serverSocket_video_send.getLocalPort();
		} catch (IOException e) {
			e.printStackTrace();
			stopServer();
			return 0;
		}

		serverThread_video_send = new Thread(() -> {
			while (running_video.get()) {
				try {
					Socket clientSocket = serverSocket_video_send.accept();
					synchronized (clientSockets_video_send) {
						clientSockets_video_send.add(clientSocket);
					}
					System.out.println("Client vidéo (envoi) connecté : " + clientSocket);
					new Thread(() -> handleClientvideo(clientSocket, true)).start();
				} catch (IOException e) {
					if (running_video.get()) {
						e.printStackTrace();
					} else {
						System.out.println("Serveur Vidéo arrêté.");
					}
				}
			}
		});

		serverThread_video_send.start();
		return PORT_VIDEO_SEND;
	}

	@Override
	public int startServerReceive() throws IOException {
		int portAudio = super.startServerReceive();
		if (running_video.get()) {
			System.out.println("Le serveur Vidéo est déjà en cours d'exécution.");
			return 0;
		}

		running_video.set(true);

		try {
			serverSocket_video_send = new ServerSocket(0);
			PORT_VIDEO_SEND = serverSocket_video_send.getLocalPort();
			serverSocket_video_receive = new ServerSocket(0);
		} catch (IOException e) {
			e.printStackTrace();
			stopServer();
			return 0;
		}

		serverThread_video_receive = new Thread(() -> {
			while (running_video.get()) {
				try {
					Socket clientSocket = serverSocket_video_receive.accept();
					synchronized (clientSockets_video_receive) {
						clientSockets_video_receive.add(clientSocket);
					}
					System.out.println("Client vidéo (réception) connecté : " + clientSocket);
					new Thread(() -> handleClientvideo(clientSocket, false)).start();
				} catch (IOException e) {
					if (running_video.get()) {
						e.printStackTrace();
					} else {
						System.out.println("Serveur Vidéo arrêté.");
					}
				}
			}
		});

		serverThread_video_receive.start();
		return PORT_VIDEO_RECEIVE;
	}

	@Override
	void stopServer() {
		super.stopServer();
		if (!running_video.get()) {
			System.out.println("Le serveur Vidéo est déjà arrêté.");
			return;
		}

		running_video.set(false);
		System.out.println("Arrêt du serveur Vidéo...");

		try {
			if (serverSocket_video_send != null) {
				serverSocket_video_send.close();
			}
			if (serverSocket_video_receive != null) {
				serverSocket_video_receive.close();
			}

			synchronized (clientSockets_video_send) {
				for (Socket clientSocket : clientSockets_video_send) {
					clientSocket.close();
				}
				clientSockets_video_send.clear();
			}

			synchronized (clientSockets_video_receive) {
				for (Socket clientSocket : clientSockets_video_receive) {
					clientSocket.close();
				}
				clientSockets_video_receive.clear();
			}

			System.out.println("Serveur Vidéo arrêté avec succès.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleClientvideo(Socket sender, boolean is_sender) {
		try {
			InputStream in = sender.getInputStream();
			byte[] dataBuffer = new byte[bufferSize];

			int bytesRead;
			while ((bytesRead = in.read(dataBuffer)) != -1) {
				String dataString = new String(dataBuffer, 0, bytesRead, StandardCharsets.UTF_8);

				Set<Socket> clientSet = is_sender ? clientSockets_video_send : clientSockets_video_receive;

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
							System.err.println("Erreur lors de l'envoi vidéo au client : " + clientSocket);
							e.printStackTrace();
							toRemove.add(clientSocket);
						}

						if (dataString.contains(Helpers.endCallType)) {
							System.out.println("Fin d'appel détectée pour : " + clientSocket);
							toRemove.add(clientSocket);
						}
					}
					clientSet.removeAll(toRemove);
				}
			}
		} catch (IOException e) {
			System.out.println("Client vidéo déconnecté : " + sender);
		} finally {
			synchronized (is_sender ? clientSockets_video_send : clientSockets_video_receive) {
				clientSockets_video_send.remove(sender);
			}
			try {
				sender.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
