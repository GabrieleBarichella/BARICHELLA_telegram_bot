package api;

public class Attributes {
    private String synopsis;
    private String canonicalTitle;
    private double averageRating;
    private Image posterImage;
    private int episodeCount;
    private String status;

    public String getSynopsis() { return synopsis; }
    public String getCanonicalTitle() { return canonicalTitle; }
    public double getAverageRating() { return averageRating; }
    public Image getPosterImage() { return posterImage; }
    public int getEpisodeCount() { return episodeCount; }
    public String getStatus() { return status; }
}
