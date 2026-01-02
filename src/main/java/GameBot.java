import modelli.GamePrint;
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

        if (!update.hasMessage() || !update.getMessage().hasText())
            return;

        String messageText = update.getMessage().getText().trim().toLowerCase();
        long chatId = update.getMessage().getChatId();
        String response;

        // ===== /help =====
        if (messageText.equals("/help")) {

            response = """
                    🎮 GameBot - Comandi disponibili
                    /help - Mostra questo messaggio
                    /game <nome> - Cerca un videogioco
                    /random - Ritorna un videogioco random
                    /recomend <genere> <numero> - Ritorna n videogiochi del genere specificato
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
                        response = GamePrint.format(game);
                    }

                } catch (Exception e) {
                    response = "⚠️ Errore durante la richiesta al servizio RAWG.";
                    e.printStackTrace();
                }
            }
        }

        // ===== RANDOM =====
        else if (messageText.equals("/random")) {
            try {
                Game randomGame = rawgService.getRandomGame();

                if (randomGame == null)
                    response = "❌ Nessun gioco trovato.";
                else
                    response = GamePrint.format(randomGame);

            } catch (Exception e) {
                response = "⚠️ Errore durante la richiesta RAWG.";
            }
        }

        // ===== RECOMMEND =====
        else if (messageText.startsWith("/recommend")) {
            String[] parts = messageText.split(" ");
            if (parts.length < 2)
                response = "❗ Uso corretto:\n/recommend <generi> <numero>";
            else {
                String generi = parts[1];
                int limit = 5; //default

                if (parts.length >= 3) {
                    try {
                        limit = Integer.parseInt(parts[2]);
                    } catch (Exception e) {}
                }

                try {
                    var games = rawgService.recommendByGenres(generi, limit);

                    if (games.isEmpty()) {
                        response = "❌ Nessun gioco trovato per questi generi.";
                    } else {
                        StringBuilder sb = new StringBuilder("🎯 Giochi consigliati:\n\n");
                        for (Game g : games) {
                            sb.append(GamePrint.format(g)).append("\n");
                        }
                        response = sb.toString();
                    }

                } catch (Exception e) {
                    response = "⚠️ Errore durante la richiesta RAWG.";
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
