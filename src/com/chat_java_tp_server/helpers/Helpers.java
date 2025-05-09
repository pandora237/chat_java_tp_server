package com.chat_java_tp_server.helpers;

public class Helpers {

	// commun server
	public static final String sendFile = "send_file";
	public static final String sendSimpleMess = "send_file";
	public static final String askFile = "ask_file";
	public static final String audioType = "audio_call";
	public static final String audioTypeResp = "audio_call_resp";
	public static final String videoType = "video_call";
	public static final String emoji = "emoji";
	public static final String audioTypeReceiver = "audio_call_receiver";
	public static final String videoTypeReceiver = "video_call_receiver";

	public static final String endCallType = "end_call";
	public static final String login = "login";
	public static final String logout = "logout";
	public static final String otherUserLogged = "other_user_logged";
	public static final String deliveredPortCall = "deliveredPortCall";

	public static final String responseSendMessage = "response_send_message";
	public static final String getMessUserSendReceive = "get_mess_user_send_receive";

	public static final String FILE_DOWNLOAD = "downloads/";
	// end commun

	public enum StatusMess {
		SENT, DELIVERED, READ
	}

	public static String extractVal(String text, String key) {
		if (text == null || key == null) {
			return null;
		}

		// Création du regex basé sur la clé, suivie de l'égalité et de la valeur
		String regex = key + "='([^']+)'"; // Match tout entre les apostrophes
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
		java.util.regex.Matcher matcher = pattern.matcher(text);

		return matcher.find() ? matcher.group(1) : null;
	}

}