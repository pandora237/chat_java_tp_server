package chat_java_tp_server;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String username;
    private String tel;
    private String email;

    public User(int id, String nom, String prenom, String username, String email, String tel) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.username = username;
        this.tel = tel;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", username='" + username + '\'' +
                ", tel='" + tel + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public static User formateUser(String text) {
        String regex = "id=(\\d+), nom='(.*?)', prenom='(.*?)', username='(.*?)', tel='(.*?)', email='(.*?)'}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            String nom = matcher.group(2);
            String prenom = matcher.group(3);
            String username = matcher.group(4);
            String tel = matcher.group(5);
            String email = matcher.group(6);

            return new User(id, nom, prenom, username, email, tel);
        } else {
            return null;
        }
    }
}
