import modelli.Game;

import java.sql.*;

public class Database {
    private static Database instance;
    private Connection connection;

    private Database() {
        try{
            String url = "jdbc:sqlite:Database/GameManager.db";
            connection = DriverManager.getConnection(url);
            System.out.println("Connected to database successfully");
        } catch (SQLException e){
            System.err.println("Errore connessione: " + e.getMessage());
        }
    }

    public static Database getInstance() {
        if (instance == null)
            instance = new Database();
        return instance;
    }

    private boolean testConnection(){
        try{
            if(connection == null || !connection.isValid(5)){
                System.err.println("Errore di connessione al DB locale");
                return false;
            }
        } catch (SQLException e){
            System.err.println("Errore di connessione al DB locale");
            return false;
        }

        return true;
    }

    public void insertUser(int telegramId, String username) {
        String query = "INSERT OR IGNORE INTO Users (telegram_id, username) VALUES (?, ?)";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, telegramId);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore insertUser: " + e.getMessage());
        }
    }

    public int getUserId(int telegramId) {
        String query = "SELECT id FROM Users WHERE telegram_id = ?";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, telegramId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
                return rs.getInt("id");

        } catch (SQLException e) {
            System.err.println("Errore getUserId: " + e.getMessage());
        }
        return -1;
    }

    public void insertGame(Game game) {
        String query = "INSERT OR IGNORE INTO Games(id, name, released, rating, metacritic, image_url) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, game.id);
            stmt.setString(2, game.name);
            stmt.setString(3, game.released);
            stmt.setDouble(4, game.rating);
            stmt.setInt(5, game.metacritic);
            stmt.setString(6, game.background_image);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore insertGame: " + e.getMessage());
        }
    }

    public void addToLibrary(int telegramId, Game game) {
        insertGame(game);
        int userId = getUserId(telegramId);

        String query = "INSERT OR IGNORE INTO Library (user_id, game_id) VALUES (?, ?)";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, game.id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore addToLibrary: " + e.getMessage());
        }
    }

    public void addToWishlist(int telegramId, Game game) {
        insertGame(game);
        int userId = getUserId(telegramId);

        String query = "INSERT OR IGNORE INTO Wishlist (user_id, game_id) VALUES (?, ?)";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, game.id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore addToWishlist: " + e.getMessage());
        }
    }
}
