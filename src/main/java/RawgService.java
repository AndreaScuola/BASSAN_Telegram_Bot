import com.google.gson.Gson;
import modelli.Game;
import modelli.GameResponse;
import modelli.Genre;
import modelli.GenreResponse;

import java.util.List;
import java.util.Random;

public class RawgService extends ApiClient {
    private final Random random = new Random();
    private final Gson gson = new Gson();

    public GameResponse selectGameByName(String name) {
        String params = "search=" + encode(name) + "&page_size=1";
        var response = getHttpResponse("games", params);

        if (response.statusCode() != 200)
            throw new RuntimeException("RAWG API error: " + response.statusCode());

        return gson.fromJson(response.body(), GameResponse.class);
    }

    public Game getRandomGame() {
        //pagina random da RAWG
        int randomPage = random.nextInt(500) + 1;

        String params = "page_size=20&page=" + randomPage;
        var response = getHttpResponse("games", params);

        GameResponse gameResponse = new Gson().fromJson(response.body(), GameResponse.class);

        if (gameResponse == null || gameResponse.results.isEmpty())
            return null;

        return gameResponse.results.get(
                random.nextInt(gameResponse.results.size())
        );
    }

    public List<Game> recommendByGenres(String genres, int limit) {
        String params =
                "genres=" + encode(genres) +
                        "&ordering=-rating" +
                        "&page_size=" + limit;

        var response = getHttpResponse("games", params);
        GameResponse gameResponse = new Gson().fromJson(response.body(), GameResponse.class);
        return gameResponse != null ? gameResponse.results : List.of();
    }

    public List<Genre> getAllGenres(){
        String endpoint = "genres";
        var response = getHttpResponse(endpoint);
        GenreResponse genreResponse = new Gson().fromJson(response.body(), GenreResponse.class);
        return genreResponse != null ? genreResponse.results : List.of();
    }

}
