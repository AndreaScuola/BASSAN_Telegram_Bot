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

    public Game selectGameById(int id) {
        String endpoint = "games/" + id;
        String params = "page_size=1";
        var response = getHttpResponse(endpoint, params);

        if (response.statusCode() != 200) {
            System.err.println("ERRORE API RAWG: " + response.statusCode());
            return null;
        }

        Game game = gson.fromJson(response.body(), Game.class);

        if (game == null || game.id == 0) {
            System.err.println("Nessun risultato trovato per ID: " + id);
            return null;
        }

        return game;
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
        return results.subList(0, Math.min(limit, results.size())); //Ritorna il minimo tra "limit" ed il numero di risultati
    }

    public List<Game> getRandomByGenre(String genre, int limit) {
        List<Game> collected = new ArrayList<>();
        Random random = new Random();

        int maxPages = 10; //Limite di sicurezza -> Se non trovo "limit" risultati alla prima pagine ne prendo altre e faccio altri tentativi
        int attempts = 0;

        while (collected.size() < limit && attempts < maxPages) {
            int page = random.nextInt(50) + 1; //Pagina random
            String params = "genres=" + genre +
                    "&page_size=20" +
                    "&page=" + page;

            var response = getHttpResponse("games", params);
            GameResponse gameResponse = new Gson().fromJson(response.body(), GameResponse.class);

            if (gameResponse != null && gameResponse.results != null)
                collected.addAll(gameResponse.results);

            attempts++;
        }

        if (collected.isEmpty())
            return Collections.emptyList();

        Collections.shuffle(collected);
        return collected.subList(0, Math.min(limit, collected.size()));
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