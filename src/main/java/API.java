import api.Anime;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class API {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static String base_url = "https://kitsu.io/api/edge/";
    private static Gson gson = new Gson();

    private static HttpRequest getHttpRequest(String url, String method, HttpRequest.BodyPublisher body) {
        return HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .uri(java.net.URI.create(url))
                .method(method, body)
                .build();
    }

    private static HttpResponse<String> getHttpResponse(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (IOException | InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    public static List<Anime> search(String title) {
        List<Anime> animeList = new ArrayList<>();

        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String url = base_url + "anime?filter[text]=" + encodedTitle;

            HttpRequest request = getHttpRequest(url, "GET", HttpRequest.BodyPublishers.noBody());
            HttpResponse<String> response = getHttpResponse(request);

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray data = json.getAsJsonArray("data");

                for (int i = 0; i < data.size(); i++) {
                    JsonObject animeJson = data.get(i).getAsJsonObject();
                    Anime a = gson.fromJson(animeJson, Anime.class);
                    animeList.add(a);
                }
            } else {
                System.out.println("Errore API: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return animeList;
    }

    public static Anime searchById(String id) {
        Anime anime = null;

        try {
            String url = base_url + "anime/" + id;
            HttpRequest request = getHttpRequest(url, "GET", HttpRequest.BodyPublishers.noBody());
            HttpResponse<String> response = getHttpResponse(request);

            if (response != null && response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject animeJson = json.getAsJsonObject("data");

                anime = gson.fromJson(animeJson, Anime.class);
            } else {
                System.out.println("Errore API: " + (response != null ? response.statusCode() : "null response"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return anime;
    }

    public static boolean isAnimeId(String id) {
        if (id == null || id.isEmpty()) return false;
        return id.chars().allMatch(Character::isDigit);
    }
}
