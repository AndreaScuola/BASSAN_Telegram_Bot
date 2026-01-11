import modelli.GameResponse;
import modelli.Game;
import modelli.Genre;
import modelli.SteamDiscountInfo;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.ArrayList;
import java.util.List;

public class GameBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final RawgService rawgService;
    SteamService steamService;


    public GameBot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
        rawgService = new RawgService();
        steamService = new SteamService();
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

                Game g = rawgService.selectGameById(gameId);

                //Eseguo le operazioni
                if (db.isInLibrary(telegramId, gameId))
                    db.removeFromLibrary(telegramId, gameId);
                else if (g != null)
                    db.addToLibrary(telegramId, g);

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

                Game g = rawgService.selectGameById(gameId);

                //Eseguo le operazioni
                if (db.isInWishlist(telegramId, gameId))
                    db.removeFromWishlist(telegramId, gameId);
                else if (g != null)
                    db.addToWishlist(telegramId, g);

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
        if (update.hasMessage() && update.getMessage().hasText()){

            chatId = update.getMessage().getChatId();
            telegramId = update.getMessage().getFrom().getId();

            //Inserisce l'utente nella tabella -> Se c'è già non lo inserisce
            db.insertUser(telegramId);

            String messageText = update.getMessage().getText().trim().toLowerCase();
            String response;

            //#region /start
            if (messageText.equals("/start")) {
                GameSender.sendStart(telegramClient, chatId);
                return;
            }
            //#endregion

            //#region /help
            else if (messageText.equals("/help")) {
                GameSender.sendHelp(telegramClient, chatId);
                return;
            }
            //#endregion

            //#region /gameseries <nome>
            else if(messageText.startsWith("/gameseries")){
                String[] parts = messageText.split(" ", 2);

                if (parts.length < 2 || parts[1].isBlank())
                    GameSender.sendMessage(telegramClient, chatId, "Uso corretto:\n/gameseries <nome del gioco>");
                else {
                    String gameName = parts[1];

                    try {
                        List<Game> games = rawgService.selectGameSeriesByName(gameName);

                        if (games.isEmpty()) {
                            GameSender.sendEmptyGameList(telegramClient, chatId, "❌ Nessuna serie trovata per *" + gameName + "*");
                            return;
                        }

                        for (Game g : games)
                            GameSender.sendGame(telegramClient, chatId, g, telegramId);
                        return;
                    } catch (Exception e) {
                        GameSender.sendMessage(telegramClient, chatId,"Errore RAWG");
                    }
                }

                return;
            }
            //#endregion

            //#region /gamedlc <nome>
            else if (messageText.startsWith("/gamedlc")) {
                String[] parts = messageText.split(" ", 2);

                if (parts.length < 2 || parts[1].isBlank())
                    GameSender.sendMessage(telegramClient, chatId,"Uso corretto:\n/gamedlc <nome del gioco>");
                else {
                    String gameName = parts[1];

                    try {
                        List<Game> dlcs = rawgService.selectGameDLCsByName(gameName);

                        if (dlcs.isEmpty()) {
                            GameSender.sendEmptyGameList(telegramClient, chatId, "❌ Nessun DLC trovato per *" + gameName + "*");
                            return;
                        }

                        for (Game g : dlcs)
                            GameSender.sendGame(telegramClient, chatId, g, telegramId);
                        return;
                    } catch (Exception e) {
                        GameSender.sendMessage(telegramClient, chatId,"Errore RAWG");
                    }
                }

                return;
            }
            //#endregion

            //#region /game <nome>
            else if (messageText.startsWith("/game")) {
                String[] parts = messageText.split(" ", 2);

                if (parts.length < 2 || parts[1].isBlank())
                    GameSender.sendMessage(telegramClient, chatId,"Uso corretto:\n/game <nome del gioco>");
                else {
                    String gameName = parts[1];

                    try {
                        GameResponse gameResponse = rawgService.selectGameByName(gameName);

                        if (gameResponse == null || gameResponse.results.isEmpty())
                            GameSender.sendMessage(telegramClient, chatId,"Nessun gioco trovato");
                        else {
                            Game game = gameResponse.results.get(0);
                            GameSender.sendGame(telegramClient, chatId, game, telegramId);
                            return;
                        }
                    } catch (Exception e) {
                        GameSender.sendMessage(telegramClient, chatId,"Errore RAWG");
                    }
                }

                return;
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
                    else if (parts.length == 2 && isNumber(parts[1])) {
                        try {
                            limit = Integer.parseInt(parts[1]);

                            if(limit < 1)
                                limit = 1;
                            else if(limit > 20)
                                limit = 20;
                        }
                        catch (Exception e) {}

                        List<Game> games = rawgService.getRandomGame(limit);

                        if (games.isEmpty())
                            GameSender.sendMessage(telegramClient, chatId,"Nessun gioco trovato");
                        else {
                            for (Game g : games)
                                GameSender.sendGame(telegramClient, chatId, g, telegramId);
                            return;
                        }
                    }

                    // ===== /random genre <genere> =====
                    else if (parts.length >= 3 && parts[1].equals("genre")) {
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
                            GameSender.sendMessage(telegramClient, chatId,"Nessun gioco trovato per il genere: " + genre);
                        else {
                            for (Game g : games)
                                GameSender.sendGame(telegramClient, chatId, g, telegramId);
                            return;
                        }
                    }
                    else
                        GameSender.sendMessage(telegramClient, chatId,"Uso corretto:\n/random <numero>\n/random genre <genere> <numero>");
                } catch (Exception e) {
                    GameSender.sendMessage(telegramClient, chatId,"Errore RAWG");
                }

                return;
            }
            //#endregion

            //#region /recommend
            else if (messageText.startsWith("/recommend")) {
                String[] parts = messageText.split(" ");
                if (parts.length < 2)
                    GameSender.sendMessage(telegramClient, chatId,"Uso corretto:\n/recommend <genere> <numero>");
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
                        List<Game> games = rawgService.recommendByGenres(generi, limit);

                        if (games.isEmpty())
                            GameSender.sendMessage(telegramClient, chatId,"Nessun gioco trovato");
                        else {
                            for (Game g : games)
                                GameSender.sendGame(telegramClient, chatId, g, telegramId);

                            return;
                        }
                    } catch (Exception e) {
                        GameSender.sendMessage(telegramClient, chatId,"Errore RAWG");
                    }
                }
                return;
            }
            //#endregion

            //#region /genres
            else if (messageText.equals("/genres")) {
                try {
                    List<Genre> genres = rawgService.getAllGenres();

                    if (genres.isEmpty())
                        GameSender.sendMessage(telegramClient, chatId,"Nessun genere trovato");
                    else {
                        response = "🎮 Generi disponibili:\n\n";
                        for (Genre g : genres)
                            response += "- " + g.slug + "\n";

                        GameSender.sendMessage(telegramClient, chatId, response);
                        return;
                    }
                } catch (Exception e) {
                    GameSender.sendMessage(telegramClient, chatId,"Errore durante il recupero dei generi");
                }
                return;
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
                    stats = "Non hai ancora aggiunto giochi.\nUsa /game per iniziare!";
                else {
                    stats = """
                        📊 *Le tue statistiche*
                        
                        🎮 Giochi in libreria: %d
                        ❤️ Giochi in wishlist: %d
                        """.formatted(libraryCount, wishlistCount);
                }

                GameSender.sendMarkdownMessage(telegramClient, chatId, stats);
                return;
            }
            //#endregion

            //#region /trailer
            else if (messageText.startsWith("/trailer")) {
                String[] parts = messageText.split(" ", 2);

                if (parts.length < 2 || parts[1].isBlank()) {
                    GameSender.sendMessage(telegramClient, chatId, "Uso corretto:\n/trailer <nome del gioco>");
                    return;
                }

                try {
                    GameResponse gr = rawgService.selectGameByName(parts[1]);

                    if (gr.results.isEmpty()) {
                        GameSender.sendEmptyGameList(telegramClient, chatId, "❌ Gioco non trovato");
                        return;
                    }

                    String trailerUrl = rawgService.getTrailerUrl(gr.results.get(0).id);

                    if (trailerUrl == null)
                        GameSender.sendNoTrailer(telegramClient, chatId);
                    else
                        GameSender.sendTrailer(telegramClient, chatId, trailerUrl, gr.results.get(0).name);
                } catch (Exception e) {
                    GameSender.sendMessage(telegramClient, chatId, "❌ Errore nel recupero del trailer");
                }
            }
            //#endregion

            //#region /steamwishlist
            else if (messageText.equals("/steamwishlist")) {
                ArrayList<String> wishlistNames = db.getWishlistGameNames(telegramId);

                if (wishlistNames.isEmpty()) {
                    GameSender.sendEmptyGameList(telegramClient, chatId);
                    return;
                }

                ArrayList<String> lines = new ArrayList<>();
                lines.add("🎮 *Sconti wishlist:*");

                for (String name : wishlistNames)
                    lines.add(steamService.getDiscountByName(name));

                GameSender.sendMarkdownMessage(telegramClient, chatId,(String.join("\n\n", lines)));
                return;
            }
            //#endregion

            //#region /steam
            else if (messageText.startsWith("/steam")) {
                String[] parts = messageText.split(" ", 2);

                if (parts.length < 2 || parts[1].isBlank()) {
                    response = "Uso corretto:\n/steam <nome del gioco>";
                } else {
                    String gameName = parts[1];
                    response = steamService.getDiscountByName(gameName);
                }

                GameSender.sendMarkdownMessage(telegramClient, chatId, response);
                return;
            }
            //#endregion

            //#region comandi non riconosciuti
            else {
                GameSender.sendMarkdownMessage(telegramClient, chatId, "Comando non riconosciuto. Usa /help");
            }
            //#endregion
        }
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
