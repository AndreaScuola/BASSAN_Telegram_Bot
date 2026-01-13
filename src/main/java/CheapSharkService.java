import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import modelli.CheapSharkDeal;

import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

public class CheapSharkService extends ApiClient{

    private final Gson gson;

    public CheapSharkService() {
        gson = new Gson();
    }

    public List<CheapSharkDeal> getDeals(String gameName, int limit) {
        try {
            String url = "https://www.cheapshark.com/api/1.0/deals?title=" + encode(gameName);
            HttpResponse<String> res = getHttpResponse(url);

            Type listType = new TypeToken<List<CheapSharkDeal>>() {}.getType();
            List<CheapSharkDeal> deals = gson.fromJson(res.body(), listType);

            return deals.stream().limit(limit).toList();
        } catch (Exception e) {
            System.err.println("Errore CheapShark: " + e.getMessage());
            return List.of();
        }
    }

    public CheapSharkDeal getCheapestDeal(String gameName) {
        List<CheapSharkDeal> deals = getDeals(gameName, 1);
        return deals.isEmpty() ? null : deals.get(0);
    }
}
