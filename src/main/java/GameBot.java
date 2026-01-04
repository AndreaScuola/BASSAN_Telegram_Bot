import modelli.GameResponse;
import modelli.Game;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.List;

public class GameBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final RawgService rawgService;

    public GameBot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
        rawgService = new RawgService();
    }

    @Override
    public void consume(Update update) {
        // ===== CALLBACK DEI BOTTONI =====
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            System.out.println("CLICK: " + data);

            // RISPOSTA TEMPORANEA
            AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                    .callbackQueryId(update.getCallbackQuery().getId())
                    .text("✔️ Azione ricevuta")
                    .showAlert(false)
                    .build();
            try {
                telegramClient.execute(answer);
            } catch (TelegramApiException e) {}

            return;
        }

        // ===== MESSAGGI DI TESTO =====
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
                    /genres - Ritorna la lista di tutti i generi disponibili
                    /random - Ritorna un videogioco random
                    /random <numero> - Ritorna N videogiochi random
                    /random genre <genere> - Ritorna un videogioco random del genere specificato
                    /random genre <genere> <numero> - Ritorna N videogiochi random del genere specificato
                    /recommend <genere> - Ritorna 5 videogiochi del genere specificato
                    /recommend <genere> <numero> - Ritorna N videogiochi del genere specificato
                    /library - Mostra la tua libreria
                    """;
        }

        // ===== /game <nome> =====
        else if (messageText.startsWith("/game")) {
            String[] parts = messageText.split(" ", 2);

            if (parts.length < 2 || parts[1].isBlank())
                response = "Uso corretto:\n/game <nome del gioco>";
            else {
                String gameName = parts[1];

                try {
                    GameResponse gameResponse = rawgService.selectGameByName(gameName);

                    if (gameResponse == null || gameResponse.results.isEmpty())
                        response = "Nessun gioco trovato.";
                    else {
                        Game game = gameResponse.results.get(0);
                        GameSender.sendGame(telegramClient, chatId, game);
                        return;
                    }
                } catch (Exception e) {
                    response = "Errore RAWG.";
                }
            }
        }

        // ===== RANDOM =====
        else if (messageText.startsWith("/random")) {
            String[] parts = messageText.split(" ");
            int limit = 1;

            try {
                // ===== /random =====
                if (parts.length == 1) {
                    List<Game> games = rawgService.getRandomGame(limit);
                    for (Game g : games)
                        GameSender.sendGame(telegramClient, chatId, g);
                    return;
                }

                // ===== /random <numero> =====
                if (parts.length == 2 && isNumber(parts[1])) {
                    try {
                        limit = Integer.parseInt(parts[1]);

                        if(limit < 1)
                            limit = 1;
                        else if(limit > 20)
                            limit = 20;
                    }
                    catch (Exception e) {}

                    List<Game> games = rawgService.getRandomGame(limit);
                    for (Game g : games)
                        GameSender.sendGame(telegramClient, chatId, g);
                    return;
                }

                // ===== /random genre <genere> =====
                if (parts.length >= 3 && parts[1].equals("genre")) {
                    String genre = parts[2];

                    if (parts.length >= 4 && isNumber(parts[3])){
                        try {
                            limit = Integer.parseInt(parts[3]);

                            if(limit < 1)
                                limit = 1;
                            else if(limit > 20)
                                limit = 20;
                        }
                        catch (Exception e) {}
                    }

                    List<Game> games = rawgService.getRandomByGenre(genre, limit);

                    if (games.isEmpty())
                        response = "Nessun gioco trovato per il genere: " + genre;
                    else {
                        for (Game g : games)
                            GameSender.sendGame(telegramClient, chatId, g);
                        return;
                    }
                }

                response = "Uso corretto:\n"
                        + "/random <numero>\n"
                        + "/random genre <genere> <numero>";
            } catch (Exception e) {
                response = "Errore RAWG";
            }
        }

        // ===== RECOMMEND =====
        else if (messageText.startsWith("/recommend")) {
            String[] parts = messageText.split(" ");
            if (parts.length < 2)
                response = "Uso corretto:\n/recommend <genere> <numero>";
            else {
                String generi = parts[1];
                int limit = 5;

                if (parts.length >= 3) {
                    try {
                        limit = Integer.parseInt(parts[2]);

                        if(limit < 1)
                            limit = 5;
                        else if(limit > 20)
                            limit = 20;
                    }
                    catch (Exception e) {}
                }

                try {
                    var games = rawgService.recommendByGenres(generi, limit);

                    if (games.isEmpty())
                        response = "Nessun gioco trovato.";
                    else {
                        for (Game g : games)
                            GameSender.sendGame(telegramClient, chatId, g);

                        return;
                    }
                } catch (Exception e) {
                    response = "Errore RAWG.";
                }
            }
        }

        // ===== GENRES =====
        else if (messageText.equals("/genres")) {
            try {
                var genres = rawgService.getAllGenres();

                if (genres.isEmpty())
                    response = "Nessun genere trovato.";
                else {
                    response = "🎮 Generi disponibili:\n\n";
                    for (var g : genres)
                        response += "- " + g.slug + "\n";
                }
            } catch (Exception e) {
                response = "Errore durante il recupero dei generi.";
            }
        }

        // ===== COMANDO NON RICONOSCIUTO =====
        else {
            response = "Comando non riconosciuto. Usa /help";
        }

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(response)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {}
    }

    private boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
