import com.google.gson.Gson;
import modelli.GameResponse;

public class RawgService extends ApiClient {

    private final Gson gson = new Gson();

    public GameResponse selectGameByName(String name) {
        String params = "search=" + encode(name) + "&page_size=1";
        var response = getHttpResponse("games", params);

        if (response.statusCode() != 200)
            throw new RuntimeException("RAWG API error: " + response.statusCode());

        return gson.fromJson(response.body(), GameResponse.class);
    }
}
