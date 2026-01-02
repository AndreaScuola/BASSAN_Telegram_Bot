package modelli;

import modelli.Game;
import modelli.Genre;
import modelli.PlatformWrapper;

public class GamePrint {
    public static String format(Game game) {

        String piattaforme = "";
        if (game.platforms != null) {
            for (PlatformWrapper pw : game.platforms) {
                if (!piattaforme.isEmpty()) piattaforme += ", ";
                piattaforme += pw.platform.name;
            }
        }
        if (piattaforme.isEmpty()) piattaforme = "N/D";

        String generi = "";
        if (game.genres != null) {
            for (Genre g : game.genres) {
                if (!generi.isEmpty()) generi += ", ";
                generi += g.name;
            }
        }
        if (generi.isEmpty()) generi = "N/D";

        return """
            🎮 %s
            🗓 Uscita: %s
            ⭐ Rating: %.1f
            🏆 Metacritic: %d
            🖥 Piattaforme: %s
            🏷 Generi: %s
            🖼 Immagine: %s
            """.formatted(
                game.name,
                game.released != null ? game.released : "N/D",
                game.rating,
                game.metacritic,
                piattaforme,
                generi,
                game.background_image != null ? game.background_image : "N/D"
        );
    }
}
