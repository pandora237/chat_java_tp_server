package com.chat_java_tp_server.helpers;

public class Helpers {

	// commun server
	public static final String sendFile = "send_file";
	public static final String audioType = "audio_call";
	public static final String videoType = "video_call";
	public static final String endCallType = "end_call";
	public static final String login = "login";
	public static final String logout = "logout";
	public static final String otherUserLogged = "other_user_logged";
	// end commun

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