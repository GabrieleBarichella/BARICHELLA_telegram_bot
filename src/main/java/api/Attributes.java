package api;

import java.util.Map;

public class Attributes {
    private String slug;
    private String synopsis;
    private String description;
    private Map<String, String> titles;
    private String canonicalTitle;
    private double averageRating;
    private Image posterImage;
    private Image coverImage;
    private int episodeCount;
    private int episodeLength;
    private int popularityRank;
    private int ratingRank;
    private String status;

    public String getSlug() { return slug; }
    public String getSynopsis() { return synopsis; }
    public String getDescription() { return description; }
    public Map<String, String> getTitles() { return titles; }
    public String getCanonicalTitle() { return canonicalTitle; }
    public double getAverageRating() { return averageRating; }
    public Image getPosterImage() { return posterImage; }
    public Image getCoverImage() { return coverImage; }
    public int getEpisodeCount() { return episodeCount; }
    public int getEpisodeLength() { return episodeLength; }
    public int getPopularityRank() { return popularityRank; }
    public int getRatingRank() { return ratingRank; }
    public String getStatus() { return status; }
}
