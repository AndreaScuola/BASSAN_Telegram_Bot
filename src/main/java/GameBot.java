import modelli.GameResponse;
import modelli.Game;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.ArrayList;
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
        long chatId;
        long telegramId;
        Database db = Database.getInstance();

        //#region GESTIONE CALLBACK DEI BUTTON
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            telegramId = update.getCallbackQuery().getFrom().getId();

            //Inserisce l'utente nella tabella -> Se c'è già non lo inserisce
            db.insertUser(telegramId);

            String data = update.getCallbackQuery().getData();
            String infoMessage = "";

            //#region gestione button disabled
            if (data.equals("DISABLED")) {
                AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                        .callbackQueryId(update.getCallbackQuery().getId())
                        .text("⏳ Attendi...")
                        .showAlert(false)  //true per alert popup, false per notifica in basso
                        .build();

                try {
                    telegramClient.execute(answer);
                } catch (TelegramApiException e) {
                    System.err.println("Errore pulsanti: " + e.getMessage());
                }

                return;
            } //#endregion
            //#region gestione button libreria
            else if (data.startsWith("LIB_")) {
                int gameId = Integer.parseInt(data.split("_")[1]);
                long messageId = update.getCallbackQuery().getMessage().getMessageId();

                //Metto il pulsante di attesa
                EditMessageReplyMarkup editMarkup = EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId((int)messageId)
                        .replyMarkup(GameSender.buildLoadingKeyboard())
                        .build();

                try {
                    telegramClient.execute(editMarkup);
                } catch (TelegramApiException e) {
                    System.err.println("Errore pulsanti: " + e.getMessage());
                }

                //Eseguo le operazioni
                if (db.isInLibrary(telegramId, gameId)) {
                    db.removeFromLibrary(telegramId, gameId);
                    infoMessage = "Gioco rimosso dalla Libreria!";
                } else {
                    Game g = rawgService.selectGameById(gameId);
                    if (g != null) {
                        db.addToLibrary(telegramId, g);
                        infoMessage = "✅ Gioco aggiunto alla Libreria!";
                    } else {
                        infoMessage = "❌ Gioco non trovato su RAWG.";
                    }
                }

                Game g = rawgService.selectGameById(gameId);

                //Rimetto i pulsanti normali
                editMarkup = EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId((int) messageId)
                        .replyMarkup(GameSender.buildKeyboard(g, telegramId))
                        .build();

                try {
                    telegramClient.execute(editMarkup);
                } catch (TelegramApiException e) {
                    System.err.println("Errore pulsanti: " + e.getMessage());
                }
            } //#endregion
            //#region gestione button wishlist
            else if (data.startsWith("WISH_")) {  //Gestione aggiunta/rimozione wishlist
                int gameId = Integer.parseInt(data.split("_")[1]);
                long messageId = update.getCallbackQuery().getMessage().getMessageId();

                //Metto il pulsante di attesa
                EditMessageReplyMarkup editMarkup = EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId((int)messageId)
                        .replyMarkup(GameSender.buildLoadingKeyboard())
                        .build();

                try {
                    telegramClient.execute(editMarkup);
                } catch (TelegramApiException e) {
                    System.err.println("Errore pulsanti: " + e.getMessage());
                }

                //Eseguo le operazioni
                if (db.isInWishlist(telegramId, gameId)) {
                    db.removeFromWishlist(telegramId, gameId);
                    infoMessage = "Gioco rimosso dalla Wishlist!";
                } else {
                    Game g = rawgService.selectGameById(gameId);
                    if (g != null) {
                        db.addToWishlist(telegramId, g);
                        infoMessage = "❤️ Gioco aggiunto alla Wishlist!";
                    } else {
                        infoMessage = "❌ Gioco non trovato su RAWG.";
                    }
                }

                Game g = rawgService.selectGameById(gameId);

                //Rimetto i pulsanti normali
                editMarkup = EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId((int) messageId)
                        .replyMarkup(GameSender.buildKeyboard(g, telegramId))
                        .build();

                try {
                    telegramClient.execute(editMarkup);
                } catch (TelegramApiException e) {
                    System.err.println("Errore pulsanti: " + e.getMessage());
                }
            }
            //#endregion

            return;
        }
        //#endregion

        //#region GESTIONE MESSAGGI DI TESTO
        if (!update.hasMessage() || !update.getMessage().hasText())
            return;

        chatId = update.getMessage().getChatId();
        telegramId = update.getMessage().getFrom().getId();

        //Inserisce l'utente nella tabella -> Se c'è già non lo inserisce
        db.insertUser(telegramId);

        String messageText = update.getMessage().getText().trim().toLowerCase();
        String response;

        //#region /help
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
                    /stats - Mostra il numero di giochi in libreria e wishlist
                    /library - Mostra la tua libreria
                    /wishlist - Mostra la tua wishlist
                    """;
        }
        //#endregion

        //#region /game <nome>
        else if (messageText.startsWith("/game")) {
            String[] parts = messageText.split(" ", 2);

            if (parts.length < 2 || parts[1].isBlank())
                response = "Uso corretto:\n/game <nome del gioco>";
            else {
                String gameName = parts[1];

                try {
                    GameResponse gameResponse = rawgService.selectGameByName(gameName);

                    if (gameResponse == null || gameResponse.results.isEmpty())
                        response = "Nessun gioco trovato";
                    else {
                        Game game = gameResponse.results.get(0);
                        GameSender.sendGame(telegramClient, chatId, game, telegramId);
                        return;
                    }
                } catch (Exception e) {
                    response = "Errore RAWG";
                }
            }
        }
        //#endregion

        //#region /random
        else if (messageText.startsWith("/random")) {
            String[] parts = messageText.split(" ");
            int limit = 1;

            try {
                // ===== /random =====
                if (parts.length == 1) {
                    List<Game> games = rawgService.getRandomGame(limit);
                    for (Game g : games)
                        GameSender.sendGame(telegramClient, chatId, g, telegramId);
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
                        GameSender.sendGame(telegramClient, chatId, g, telegramId);
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
                            GameSender.sendGame(telegramClient, chatId, g, telegramId);
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
        //#endregion

        //#region /recommend
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
                            GameSender.sendGame(telegramClient, chatId, g, telegramId);

                        return;
                    }
                } catch (Exception e) {
                    response = "Errore RAWG.";
                }
            }
        }
        //#endregion

        //#region /genres
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
        //#endregion

        //#region /library
        else if (messageText.equals("/library")){
            ArrayList<Game> library = db.readLibrary(telegramId);

            if (library.isEmpty())
                GameSender.sendEmptyGameList(telegramClient, chatId);
            else {
                try {
                    for (Game g : library)
                        GameSender.sendGame(telegramClient, chatId, g, telegramId);
                } catch (Exception e) {
                    System.err.println("Errore stampa libreria");
                }
            }

            return;
        }
        //#endregion

        //#region /wishlist
        else if (messageText.equals("/wishlist")) {
            ArrayList<Game> wishlist = db.readWishlist(telegramId);

            if (wishlist.isEmpty())
                GameSender.sendEmptyGameList(telegramClient, chatId);
            else {
                try {
                    for (Game g : wishlist)
                        GameSender.sendGame(telegramClient, chatId, g, telegramId);
                } catch (Exception e) {
                    System.err.println("Errore stampa wishlist");
                }
            }

            return;
        }
        //#endregion

        //#region /stats
        else if (messageText.equals("/stats")) {
            int libraryCount = db.countLibrary(telegramId);
            int wishlistCount = db.countWishlist(telegramId);
            String stats;

            if (libraryCount == 0 && wishlistCount == 0)
                stats = "\n\nNon hai ancora aggiunto giochi.\nUsa /game per iniziare!";
            else {
                stats = """
                        📊 *Le tue statistiche*
                        
                        🎮 Giochi in libreria: %d
                        ❤️ Giochi in wishlist: %d
                        """.formatted(libraryCount, wishlistCount);
            }

            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(stats)
                    .parseMode("Markdown")
                    .build();

            try {
                telegramClient.execute(message);
            } catch (TelegramApiException e) {
                System.err.println("Errore /stats: " + e.getMessage());
            }

            return;
        }
        //#region

        //#region comandi non riconosciuti
        else {
            response = "Comando non riconosciuto. Usa /help";
        }
        //#endregion

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(response)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {}


        //#endregion
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
