import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import modelli.SteamAppWrapper;
import modelli.SteamDiscountInfo;
import modelli.SteamPriceOverview;
import modelli.SteamSearchResponse;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SteamService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public int getAppIdByName(String name) {
        try {
            String url = "https://store.steampowered.com/api/storesearch/?term=" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "&l=english&cc=US";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            SteamSearchResponse response = gson.fromJson(res.body(), SteamSearchResponse.class);

            if (response != null && response.items != null && !response.items.isEmpty())
                return response.items.get(0).id;
        } catch (Exception e) {
            System.err.println("Errore getAppIdByName: " + e.getMessage());
        }

        return -1;
    }

    public SteamDiscountInfo getDiscountInfo(int appid) {
        try {
            String url = "https://store.steampowered.com/api/appdetails?appids=" + appid + "&cc=US&l=english";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            Type type = new TypeToken<Map<String, SteamAppWrapper>>() {}.getType();
            Map<String, SteamAppWrapper> map = gson.fromJson(res.body(), type);

            SteamAppWrapper wrapper = map.get(String.valueOf(appid));

            if (wrapper != null && wrapper.success && wrapper.data != null && wrapper.data.price_overview != null) {
                SteamPriceOverview p = wrapper.data.price_overview;

                //Calcola il prezzo finale usando iniziale e percentuale
                int calculatedFinal = p.initial - (p.initial * p.discount_percent / 100);

                return new SteamDiscountInfo(wrapper.data.name, p.initial, calculatedFinal, p.discount_percent);
            }
        } catch (Exception e) {
            System.err.println("Errore getDiscountInfo: " + e.getMessage());
        }

        return null;
    }

    public String getDiscountByName(String name) {
        int appid = getAppIdByName(name);

        if (appid == -1)
            return "❌ *Non trovato su Steam*: " + name;

        SteamDiscountInfo info = getDiscountInfo(appid);

        if (info == null)
            return "📌 *" + name + "* — prezzo non disponibile";

        if (info.discountPercent > 0) {
            return """
                    🛍 *%s*
                    🔻 -%d%%
                    💰 %.2f€ (era %.2f€)
                    """.formatted(
                    info.name,
                    info.discountPercent,
                    info.salePrice / 100.0,
                    info.originalPrice / 100.0
            );
        }

        return "📊 *" + info.name + "* — nessuno sconto";
    }
}
