import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private final HttpClient client = HttpClient.newHttpClient();

    protected HttpResponse<String> getHttpResponse(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .uri(URI.create(url))
                    .GET()
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Errore chiamata API: " + e.getMessage());
            return null;
        }
    }

    //Usata per permettere l'invio di caratteri come spazio nell'url
    protected static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}