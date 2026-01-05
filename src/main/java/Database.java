import modelli.Game;
import java.sql.*;
import java.util.ArrayList;

public class Database {
    private static Database instance;
    private Connection connection;

    private Database() {
        try{
            String url = "jdbc:sqlite:Database/GameManager.db";
            connection = DriverManager.getConnection(url);
            System.out.println("Connesso al db locale");
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

    public void insertUser(long telegramId) {
        String query = "INSERT OR IGNORE INTO Users(telegram_id) VALUES (?)";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setLong(1, telegramId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore insertUser: " + e.getMessage());
        }
    }

    public int getUserId(long telegramId) {
        String query = "SELECT id FROM Users WHERE telegram_id = ?";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setLong(1, telegramId);
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

    public void addToLibrary(long telegramId, Game game) {
        insertGame(game);
        int userId = getUserId(telegramId);

        if (userId == -1) {
            System.err.println("addToLibrary: userId = -1");
            return;
        }

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

    public void removeFromLibrary(long telegramId, int gameId) {
        int userId = getUserId(telegramId);

        if (userId == -1) {
            System.err.println("Errore removeFromLibrary: userId = -1");
            return;
        }

        String query = "DELETE FROM Library WHERE user_id = ? AND game_id = ?";
        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, gameId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore removeFromLibrary: " + e.getMessage());
        }
    }

    public boolean isInLibrary(long telegramId, int gameId) {
        int userId = getUserId(telegramId);
        if (userId == -1) {
            System.err.println("Errore isInLibrary: userId = -1");
            return false;
        }

        String query = "SELECT user_id FROM Library WHERE user_id = ? AND game_id = ?";
        try{
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, gameId);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Errore isInLibrary: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<Game> readLibrary(long telegramId){
        ArrayList<Game> games = new ArrayList<Game>();
        int userId = getUserId(telegramId);

        if (userId == -1) {
            System.err.println("readLibrary: userId = -1");
            return games;
        }

        String query = "SELECT g.* " +
                "FROM Library l INNER JOIN Games g ON l.game_id = g.id " +
                "WHERE l.user_id = ?";

        try{
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Game g = new Game();
                g.id = rs.getInt("id");
                g.name = rs.getString("name");
                g.released = rs.getString("released");
                g.rating = rs.getDouble("rating");
                g.background_image = rs.getString("image_url");

                games.add(g);
            }
        } catch (SQLException e) {
            System.err.println("Errore readLibrary: " + e.getMessage());
        }

        return games;
    }

    public int countLibrary(long telegramId) {
        int userId = getUserId(telegramId);
        if (userId == -1)
            return 0;

        String query = "SELECT COUNT(*) FROM Library WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
                return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            System.err.println("Errore countLibrary: " + e.getMessage());
            return 0;
        }
    }

    public void addToWishlist(long telegramId, Game game) {
        insertGame(game);
        int userId = getUserId(telegramId);

        String query = "INSERT OR IGNORE INTO Wishlist(user_id, game_id) VALUES (?, ?)";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, game.id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore addToWishlist: " + e.getMessage());
        }
    }

    public void removeFromWishlist(long telegramId, int gameId) {
        int userId = getUserId(telegramId);
        String query = "DELETE FROM Wishlist WHERE user_id = ? AND game_id = ?";
        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, gameId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore removeFromWishlist: " + e.getMessage());
        }
    }

    public boolean isInWishlist(long telegramId, int gameId) {
        int userId = getUserId(telegramId);
        if(userId == -1){
            System.out.println("isInWishlist: userId = -1");
            return false;
        }

        String query = "SELECT user_id FROM Wishlist WHERE user_id = ? AND game_id = ?";
        try{
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, gameId);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Errore isInWishlist: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<Game> readWishlist(long telegramId){
        ArrayList<Game> games = new ArrayList<Game>();
        int userId = getUserId(telegramId);

        if (userId == -1) {
            System.err.println("readWishlist: userId = -1");
            return games;
        }

        String query = "SELECT g.* " +
                "FROM Wishlist w INNER JOIN Games g ON w.game_id = g.id " +
                "WHERE w.user_id = ?";

        try{
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Game g = new Game();
                g.id = rs.getInt("id");
                g.name = rs.getString("name");
                g.released = rs.getString("released");
                g.rating = rs.getDouble("rating");
                g.background_image = rs.getString("image_url");

                games.add(g);
            }
        } catch (SQLException e) {
            System.err.println("Errore readWishlist: " + e.getMessage());
        }

        return games;
    }

    public int countWishlist(long telegramId) {
        int userId = getUserId(telegramId);
        if (userId == -1)
            return 0;

        String query = "SELECT COUNT(*) FROM Wishlist WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
                return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            System.err.println("Errore countWishlist: " + e.getMessage());
            return 0;
        }
    }
}
