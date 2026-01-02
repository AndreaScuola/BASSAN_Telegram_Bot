import modelli.GameResponse;
import modelli.Game;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class GameBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final RawgService rawgService;

    public GameBot(String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.rawgService = new RawgService();
    }

    @Override
    public void consume(Update update) {

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String messageText = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        String response;

        // ===== /help =====
        if (messageText.equals("/help")) {

            response = """
                    🎮 GameBot - Comandi disponibili
                    /help - Mostra questo messaggio
                    /game <nome> - Cerca un videogioco
                    /library - Mostra la tua libreria
                    """;
        }
        // ===== /game <nome> =====
        else if (messageText.startsWith("/game")) {

            String[] parts = messageText.split(" ", 2);

            if (parts.length < 2 || parts[1].isBlank()) {
                response = "❗ Uso corretto:\n/game <nome del gioco>";
            } else {
                String gameName = parts[1];

                try {
                    GameResponse gameResponse = rawgService.selectGameByName(gameName);

                    if (gameResponse == null || gameResponse.results.isEmpty()) {
                        response = "❌ Nessun gioco trovato con questo nome.";
                    } else {
                        Game game = gameResponse.results.get(0);

                        // Piattaforme come stringa
                        String piattaforme = "";
                        if (game.platforms != null) {
                            for (modelli.PlatformWrapper pw : game.platforms) {
                                if (!piattaforme.isEmpty()) piattaforme += ", ";
                                piattaforme += pw.platform.name;
                            }
                        }
                        if (piattaforme.isEmpty()) piattaforme = "N/D";

                        // Generi come stringa
                        String generi = "";
                        if (game.genres != null) {
                            for (modelli.Genre g : game.genres) {
                                if (!generi.isEmpty()) generi += ", ";
                                generi += g.name;
                            }
                        }
                        if (generi.isEmpty()) generi = "N/D";

                        // Costruzione messaggio
                        response = """
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

                } catch (Exception e) {
                    response = "⚠️ Errore durante la richiesta al servizio RAWG.";
                    e.printStackTrace();
                }
            }
        }

        // ===== COMANDO NON RICONOSCIUTO =====
        else {
            response = "❓ Comando non riconosciuto. Usa /help";
        }

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(response)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
