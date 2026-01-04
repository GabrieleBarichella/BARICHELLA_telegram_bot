package api;

import java.time.OffsetDateTime;
import java.util.Map;

public class Attributes {
    private String slug;
    private String synopsis;
    private Map<String, String> titles;
    private String canonicalTitle;
    private double averageRating;
    private Image posterImage;
    private int episodeCount;
    private int popularityRank;
    private int ratingRank;
    private String status;
    private String updatedAt;

    public String getSlug() { return slug; }
    public String getSynopsis() { return synopsis; }
    public Map<String, String> getTitles() { return titles; }
    public String getCanonicalTitle() { return canonicalTitle; }
    public double getAverageRating() { return averageRating; }
    public Image getPosterImage() { return posterImage; }
    public int getEpisodeCount() { return episodeCount; }
    public int getPopularityRank() { return popularityRank; }
    public int getRatingRank() { return ratingRank; }
    public String getStatus() { return status; }
    public String getUpdatedAt() { return updatedAt; }
}
