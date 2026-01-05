public class GameInfo {
    int correctOptionId;
    int gameId;
    int points;
    String correctTitle;

    public GameInfo(int correctOptionId, int gameId, int points, String correctTitle) {
        this.correctOptionId = correctOptionId;
        this.gameId = gameId;
        this.points = points;
        this.correctTitle = correctTitle;
    }
}
