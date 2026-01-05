import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import modelli.SteamDiscountInfo;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SteamService {
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public int getAppIdByName(String name) {
        try {
            String url = "https://store.steampowered.com/api/storesearch/?term="
                    + java.net.URLEncoder.encode(name, "UTF-8")
                    + "&l=english&cc=US";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            JsonObject json = gson.fromJson(res.body(), JsonObject.class);
            JsonArray items = json.getAsJsonArray("items");

            if (items != null && items.size() > 0) {
                return items.get(0).getAsJsonObject().get("id").getAsInt();
            }
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
            JsonObject json = gson.fromJson(res.body(), JsonObject.class);
            JsonObject appData = json.getAsJsonObject(String.valueOf(appid));

            if (appData != null && appData.get("success").getAsBoolean()) {
                JsonObject data = appData.getAsJsonObject("data");
                JsonObject price = data.has("price_overview") ? data.getAsJsonObject("price_overview") : null;

                if (price != null)
                    return new SteamDiscountInfo(data.get("name").getAsString(), price.get("initial").getAsInt(), price.get("final").getAsInt(), price.get("discount_percent").getAsInt());
            }
        } catch (Exception e) {
            System.err.println("Steam price error: " + e.getMessage());
        }
        return null;
    }

    public String getDiscountByName(String name) {
        int appid = getAppIdByName(name);

        if (appid == -1) {
            return "❌ *Non trovato su Steam*: " + name;
        }

        SteamDiscountInfo info = getDiscountInfo(appid);

        if (info == null) {
            return "📌 *" + name + "* — prezzo non disponibile";
        }

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
