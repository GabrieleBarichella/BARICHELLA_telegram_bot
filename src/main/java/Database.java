import api.Anime;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void addAnime(Anime anime) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO anime (
                anime_id,
                title,
                synopsis,
                episode_count,
                average_rating,
                status,
                poster_url,
                last_updated
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(anime.getId()));
            ps.setString(2, anime.getAttributes().getCanonicalTitle());
            ps.setString(3, anime.getAttributes().getSynopsis());
            ps.setObject(4, anime.getAttributes().getEpisodeCount());
            ps.setObject(5, anime.getAttributes().getAverageRating());
            ps.setString(6, anime.getAttributes().getStatus());
            ps.setString(7, anime.getAttributes().getPosterImage().getOriginal());

            ps.executeUpdate();
        }
    }

    public int getCompletedAnimeCount(long userId) {
        String sql = """
        SELECT COUNT(*)
        FROM user_anime
        WHERE user_id = ? AND state = 'COMPLETED'
        """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public Map<String, Integer> getUserGameStats(long userId) {
        String sql = """
        SELECT
            COUNT(*) AS games_played,
            COALESCE(SUM(score), 0) AS total_score
        FROM user_games
        WHERE user_id = ?
        """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Integer> stats = new HashMap<>();
                stats.put("games_played", rs.getInt("games_played"));
                stats.put("total_score", rs.getInt("total_score"));
                return stats;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Map.of("games_played", 0, "total_score", 0);
    }

    public List<String> getUserAnimeByState(long userId, String state) {

        if (checkConnectionError()) return List.of();

        String sql = """
            SELECT a.title
            FROM user_anime ua
            JOIN anime a ON a.anime_id = ua.anime_id
            WHERE ua.user_id = ?
            AND ua.state = ?
            ORDER BY a.title
        """;

        List<String> result = new ArrayList<>();

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setString(2, state);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
            return null;
        }

        return result;
    }

    public String getUserAnimeState(long userId, String animeId) {
        String sql = "SELECT state FROM user_anime WHERE user_id = ? AND anime_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setString(2, animeId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("state") : null;
        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
            return null;
        }
    }

    public void addOrUpdateUserAnime(long userId, String animeId, String state) {
        String sql = """
            INSERT INTO user_anime(user_id, anime_id, state)
            VALUES (?, ?, ?)
            ON CONFLICT(user_id, anime_id) DO UPDATE SET state = excluded.state
        """;

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setString(2, animeId);
            ps.setString(3, state);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
        }
    }

    public void addUserGame(long userId, int gameId, int points) {
        String insertSql = "INSERT INTO user_games(user_id, game_id, score) VALUES (?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(insertSql);
            ps.setLong(1, userId);
            ps.setInt(2, gameId);
            ps.setInt(3, points);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getTopUsers(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        String sql = """
        SELECT u.user_id, u.username, SUM(ug.score) AS total_points
        FROM user_games ug
        JOIN users u ON ug.user_id = u.user_id
        GROUP BY u.user_id, u.username
        ORDER BY total_points DESC
        LIMIT ?
        """;

        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("user_id", rs.getLong("user_id"));
                row.put("username", rs.getString("username"));
                row.put("total_points", rs.getInt("total_points"));
                result.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
            return null;
        }

        return result;
    }

    public void removeUserAnime(long userId, String animeId) {
        String sql = "DELETE FROM user_anime WHERE user_id = ? AND anime_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, userId);
            ps.setString(2, animeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Query error: " + e.getMessage());
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
