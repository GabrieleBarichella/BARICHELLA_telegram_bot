package api;

import java.util.Map;

public class Attributes {
    private String synopsis;
    private String canonicalTitle;
    private double averageRating;
    private Image posterImage;
    private int episodeCount;
    private int popularityRank;
    private int ratingRank;
    private String status;
    private String updatedAt;

    public String getSynopsis() { return synopsis; }
    public String getCanonicalTitle() { return canonicalTitle; }
    public double getAverageRating() { return averageRating; }
    public Image getPosterImage() { return posterImage; }
    public int getEpisodeCount() { return episodeCount; }
    public int getPopularityRank() { return popularityRank; }
    public int getRatingRank() { return ratingRank; }
    public String getStatus() { return status; }
    public String getUpdatedAt() { return updatedAt; }
}
