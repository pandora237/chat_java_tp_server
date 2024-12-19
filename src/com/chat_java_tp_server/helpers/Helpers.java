package com.chat_java_tp_server.helpers;

public class Helpers {

    public static String extractVal(String text, String key) {
        if (text == null || key == null) {
            return null;
        }

        // Création du regex basé sur la clé, suivie de l'égalité et de la valeur
        String regex = key + "='([^']+)'";  // Match tout entre les apostrophes
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        return matcher.find() ? matcher.group(1) : null;
    }

}