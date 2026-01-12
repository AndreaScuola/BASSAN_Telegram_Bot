import com.google.gson.Gson;
import modelli.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RawgService extends ApiClient {
    private static final String RAWG_BASE_URL = "https://api.rawg.io/api/";
    private static final String API_KEY = ConfigurationSingleton.getInstance().getProperty("APIKEY_RAWG");
    private final Random random;
    private final Gson gson;

    public RawgService() {
        random = new Random();
        gson = new Gson();
    }

    public String getRawgUrl(String endpoint){
        return RAWG_BASE_URL + endpoint + "?key=" + API_KEY;
    }

    public String getRawgUrl(String endpoint, String queryParams){
        return RAWG_BASE_URL + endpoint + "?key=" + API_KEY + "&" + queryParams;
    }

    public Game selectGameById(int id) {
        String endpoint = "games/" + id;
        String params = "page_size=1";
        var response = getHttpResponse(getRawgUrl(endpoint, params));

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
        var response = getHttpResponse(getRawgUrl("games", params));

        if (response.statusCode() != 200)
            throw new RuntimeException("ERRORE API RAWG: " + response.statusCode());

        return gson.fromJson(response.body(), GameResponse.class);
    }

    public List<Game> selectGameSeriesByName(String name) {
        GameResponse search = selectGameByName(name);

        if (search == null || search.results == null || search.results.isEmpty())
            return new ArrayList<>();

        int gameId = search.results.get(0).id; //Prendo l'id del gioco per l'endpoint

        String url = getRawgUrl("games/" + gameId + "/game-series");
        var response = getHttpResponse(url);

        if (response.statusCode() != 200)
            return new ArrayList<>();

        GameResponse seriesResponse = new Gson().fromJson(response.body(), GameResponse.class);

        if (seriesResponse == null || seriesResponse.results == null)
            return new ArrayList<>();

        return new ArrayList<>(seriesResponse.results);
    }

    public List<Game> getRandomGame(int limit) {
        //pagina random da RAWG
        int randomPage = random.nextInt(500) + 1;

        String params = "page_size=20&page=" + randomPage;
        var response = getHttpResponse(getRawgUrl("games", params));

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

            var response = getHttpResponse(getRawgUrl("games", params));
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
        String params = "genres=" + encode(genres) + "&ordering=-rating" + "&page_size=" + limit;

        var response = getHttpResponse(getRawgUrl("games", params));
        GameResponse gameResponse = new Gson().fromJson(response.body(), GameResponse.class);

        if(gameResponse == null || gameResponse.results.isEmpty())
            return new ArrayList<>();
        return gameResponse.results;
    }

    public List<Genre> getAllGenres(){
        var response = getHttpResponse(getRawgUrl("genres"));
        GenreResponse genreResponse = new Gson().fromJson(response.body(), GenreResponse.class);
        if(genreResponse == null || genreResponse.results.isEmpty())
            return new ArrayList<>();
        return genreResponse.results;
    }

    public List<Game> selectGameDLCsByName(String name) {
        GameResponse search = selectGameByName(name);

        if (search == null || search.results == null || search.results.isEmpty())
            return new ArrayList<>();

        int gameId = search.results.get(0).id; //Prendo l'id del gioco per l'endpoint
        String url = getRawgUrl("games/" + gameId + "/additions"); //Endpoint DLC
        var response = getHttpResponse(url);

        if (response.statusCode() != 200)
            return new ArrayList<>();

        GameResponse dlcResponse = new Gson().fromJson(response.body(), GameResponse.class);

        if (dlcResponse == null || dlcResponse.results == null)
            return new ArrayList<>();

        return new ArrayList<>(dlcResponse.results);
    }

    public String getTrailerUrl(int gameId) {
        String url = getRawgUrl("games/" + gameId + "/movies");
        var response = getHttpResponse(url);

        if (response.statusCode() != 200)
            return null;

        TrailerResponse tr = gson.fromJson(response.body(), TrailerResponse.class);

        if (tr == null || tr.results == null || tr.results.isEmpty())
            return null;

        return tr.results.get(0).data.max;
    }

    public List<Game> selectGamesBySamePublisher(String gameName, int limit) {
        GameResponse gameResponse = selectGameByName(gameName);

        if (gameResponse == null || gameResponse.results.isEmpty())
            return new ArrayList<>();

        int gameId = gameResponse.results.get(0).id;
        Game game = selectGameById(gameId);

        if (game.publishers == null || game.publishers.isEmpty())
            return new ArrayList<>();

        int publisherId = game.publishers.get(0).id;

        //Cerco giochi stesso publisher
        String params = "publishers=" + publisherId + "&page_size=" + limit;
        var response = getHttpResponse(getRawgUrl("games", params));

        if (response.statusCode() != 200){
            System.err.println("ERRORE API RAWG samepublisher: " + response.statusCode());
            return new ArrayList<>();
        }

        GameResponse gr = gson.fromJson(response.body(), GameResponse.class);
        return gr.results;
    }

}