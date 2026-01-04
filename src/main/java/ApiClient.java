import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private static final String RAWG_BASE_URL = "https://api.rawg.io/api/";
    private static final String API_KEY = ConfigurationSingleton.getInstance().getProperty("APIKEY_RAWG");
    private final HttpClient client = HttpClient.newHttpClient();

    protected HttpResponse<String> getHttpResponse(String endpoint) {
        try {
            String url = RAWG_BASE_URL + endpoint + "?key=" + API_KEY;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Errore chiamata API RAWG", e);
        }
    }

    protected HttpResponse<String> getHttpResponse(String endpoint, String queryParams) {
        try {
            String url = RAWG_BASE_URL + endpoint
                    + "?key=" + API_KEY
                    + "&" + queryParams;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Errore chiamata API RAWG", e);
        }
    }

    //Usata per permettere l'invio di caratteri come spazio nell'url
    protected static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}