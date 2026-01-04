import java.sql.*;

public class Database {

    private Connection connection;
    private static Database instance = null;

    private Database() throws SQLException {
        String url = "jdbc:sqlite:database/database.db";
        connection = DriverManager.getConnection(url);
        System.out.println("Database connection established");
    }

    public static Database getInstance() throws SQLException {
        if(instance == null)
            instance = new Database();
        return instance;
    }

    public void addUser(long userId, String username) {

        if(checkConnectionError()) return;

        String sql = """
            INSERT INTO users(user_id, username)
            VALUES (?, ?)
            ON CONFLICT(user_id) DO UPDATE SET username = excluded.username
        """;

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setString(2, username);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
        }
    }

    public String getUsername(long userId) {

        if(checkConnectionError()) return null;

        String sql = "SELECT username FROM users WHERE user_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("username") : null;
        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
            return null;
        }
    }

    public String getUserAnimeState(long userId, String animeId) throws SQLException {
        String sql = "SELECT state FROM user_anime WHERE user_id = ? AND anime_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, animeId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("state") : null;
        }
    }

    public void addOrUpdateUserAnime(long userId, String animeId, String state) throws SQLException {
        String sql = """
            INSERT INTO user_anime(user_id, anime_id, state)
            VALUES (?, ?, ?)
            ON CONFLICT(user_id, anime_id) DO UPDATE SET state = excluded.state
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, animeId);
            ps.setString(3, state);
            ps.executeUpdate();
        }
    }

    public boolean checkConnectionError(){
        try {
            if(connection == null || !connection.isValid(5)) {
                System.err.println("Connection error");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Connection error");
            return true;
        }
        return false;
    }
}
