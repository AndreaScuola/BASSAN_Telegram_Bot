import com.google.gson.Gson;
import modelli.Game;
import modelli.GameResponse;
import modelli.Genre;
import modelli.GenreResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RawgService extends ApiClient {
    private final Random random;
    private final Gson gson;

    public RawgService() {
        random = new Random();
        gson = new Gson();
    }

    public GameResponse selectGameByName(String name) {
        String params = "search=" + encode(name) + "&page_size=1";
        var response = getHttpResponse("games", params);

        if (response.statusCode() != 200)
            throw new RuntimeException("ERRORE API RAWG: " + response.statusCode());

        return gson.fromJson(response.body(), GameResponse.class);
    }

    public List<Game> getRandomGame(int limit) {
        //pagina random da RAWG
        int randomPage = random.nextInt(500) + 1;

        String params = "page_size=20&page=" + randomPage;
        var response = getHttpResponse("games", params);

        GameResponse gameResponse = new Gson().fromJson(response.body(), GameResponse.class);

        List<Game> results = new ArrayList<>(gameResponse.results);
        Collections.shuffle(results); //Mescola la lista
        return results.subList(0, limit); //Ritorna solo i primi "limit"
    }

    public List<Game> recommendByGenres(String genres, int limit) {
        String params = "genres=" + encode(genres) +
                        "&ordering=-rating" +
                        "&page_size=" + limit;

        var response = getHttpResponse("games", params);
        GameResponse gameResponse = new Gson().fromJson(response.body(), GameResponse.class);

        if(gameResponse == null || gameResponse.results.isEmpty())
            return null;
        return gameResponse.results;
    }

    public List<Genre> getAllGenres(){
        String endpoint = "genres";
        var response = getHttpResponse(endpoint);
        GenreResponse genreResponse = new Gson().fromJson(response.body(), GenreResponse.class);
        if(genreResponse == null || genreResponse.results.isEmpty())
            return null;
        return genreResponse.results;
    }
}