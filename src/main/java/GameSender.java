import modelli.Game;
import modelli.GameResponse;
import modelli.Genre;
import modelli.PlatformWrapper;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import java.util.ArrayList;

public class GameSender {
    public static void sendMessage(TelegramClient client, long chatId, String textMessage){
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(textMessage)
                .build();

        try {
            client.execute(message);
        } catch (Exception e) {
            System.err.println("Errore stampa message: " + e.getMessage());
        }
    }

    public static void sendMarkdownMessage(TelegramClient client, long chatId, String textMessage){
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(textMessage)
                .parseMode("Markdown")
                .build();

        try {
            client.execute(message);
        } catch (Exception e) {
            System.err.println("Errore stampa MD message: " + e.getMessage());
        }
    }

    public static void sendStart(TelegramClient client, long chatId) {
        String text = """
            👋 *Benvenuto su GameBot!*
            
            🎮 Il tuo assistente personale per il mondo dei videogiochi.
            Con GameBot puoi scoprire nuovi giochi, gestire la tua libreria,
            controllare sconti su Steam e molto altro.
            
            ✨ *Cosa puoi fare:*
            🔍 Cercare videogiochi
            📚 Gestire la tua libreria personale
            ❤️ Salvare giochi nella wishlist
            🎲 Scoprire giochi casuali
            🧩 Trovare DLC e giochi della stessa serie
            💸 Controllare prezzi e sconti su Steam
            
            📌 *Comandi principali:*
            /game <nome> — Cerca un videogioco
            /random — Gioco casuale
            /library — La tua libreria
            /wishlist — La tua wishlist
            /steam <nome> — Prezzi e sconti Steam
            /steamwishlist — Sconti sui giochi in wishlist
            /gameseries <nome> — Giochi della stessa serie
            /gamedlc <nome> — DLC ed espansioni
            /genres — Tutti i generi disponibili
            /help — Lista completa dei comandi
            
            🚀 Inizia subito cercando un gioco:
            👉 `/game Portal`
            
            Buon divertimento! 🎮🔥
            """;

        sendMarkdownMessage(client, chatId, text);
    }

    public static void sendHelp(TelegramClient client, long chatId) {
        String text = """
                🎮 *GameBot* — Comandi disponibili
                
                ---
                
                🔍 *Ricerca giochi*
                • /game <nome> — Cerca un videogioco  
                • /gameseries <nome> — Giochi della stessa serie  
                • /gamedlc <nome> — DLC ed espansioni del gioco  
                • /genres — Lista di tutti i generi disponibili  
                
                ---
                
                🎲 *Giochi casuali*
                • /random — Videogioco casuale  
                • /random <numero> — N videogiochi casuali  
                • /random genre <genere> — Random per genere  
                • /random genre <genere> <numero> — N giochi random per genere  
                
                ---
                
                ⭐ *Consigli*
                • /recommend <genere> — 5 giochi consigliati per genere  
                • /recommend <genere> <numero> — N giochi consigliati  
                
                ---
                
                📚 *Libreria & Wishlist*
                • /library — La tua libreria personale  
                • /wishlist — La tua wishlist  
                • /stats — Statistiche personali  
                
                ---
                
                💸 *Steam*
                • /steam <nome> — Prezzo e sconti Steam  
                • /steamwishlist — Sconti sui giochi in wishlist  
                
                ---
                
                ℹ️ *Altro*
                • /help — Mostra questo messaggio
                """;

        sendMarkdownMessage(client, chatId, text);
    }



    public static void sendGame(TelegramClient client, long chatId, Game game, long telegramId) throws TelegramApiException {
        String caption = buildText(game);
        InlineKeyboardMarkup keyboard = buildKeyboard(game, telegramId);

        //Se esiste l'immagine -> SendPhoto
        if (game.background_image != null && !game.background_image.isBlank()) {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(game.background_image))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build();

            client.execute(photo);
        } else {
            //Se non c'è l'immagine
            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId)
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build();
            client.execute(photo);
        }
    }

    private static String buildText(Game game) {
        String piattaforme = "";
        if (game.platforms != null) {
            for (PlatformWrapper pw : game.platforms) {
                if (!piattaforme.isEmpty())
                    piattaforme += ", ";
                piattaforme += pw.platform.name;
            }
        }
        if (piattaforme.isEmpty())
            piattaforme = "N/D";

        String generi = "";
        if (game.genres != null) {
            for (Genre g : game.genres) {
                if (!generi.isEmpty())
                    generi += ", ";
                generi += g.name;
            }
        }
        if (generi.isEmpty())
            generi = "N/D";

        return """
            🎮 %s
            🗓 Uscita: %s
            ⭐ Rating: %.1f
            🏆 Metacritic: %d
            🖥 Piattaforme: %s
            🏷 Generi: %s
            """.formatted(game.name, game.released != null ? game.released : "N/D", game.rating, game.metacritic, piattaforme, generi);
    }

    public static InlineKeyboardMarkup buildKeyboard(Game game, long telegramId) {
        Database db = Database.getInstance();
        boolean inLibrary = db.isInLibrary(telegramId, game.id);
        boolean inWishlist = db.isInWishlist(telegramId, game.id);
        String textLibraryBtn = inLibrary ? "❌ Rimuovi Libreria" : "➕ Libreria";
        String textWishlistBtn = inWishlist ? "❌ Rimuovi Wishlist" : "❤️ Wishlist";

        InlineKeyboardButton libraryBtn = InlineKeyboardButton.builder()
                .text(textLibraryBtn)
                .callbackData("LIB_" + game.id)
                .build();

        InlineKeyboardButton wishlistBtn = InlineKeyboardButton.builder()
                .text(textWishlistBtn)
                .callbackData("WISH_" + game.id)
                .build();

        InlineKeyboardButton rawgBtn = InlineKeyboardButton.builder()
                .text("🔍 Apri su RAWG")
                .url("https://rawg.io/games/" + game.id)
                .build();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(libraryBtn);
        row1.add(wishlistBtn);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(rawgBtn);

        ArrayList<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public static void sendEmptyGameList(TelegramClient telegramClient, long chatId) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text("📚 *La collezione è vuota!*\n\nNon hai ancora aggiunto nessun gioco\nUsa /game per cercarne uno e aggiungerlo!")
                .parseMode("Markdown")
                .build();

        sendEmptyGameListGif(telegramClient, chatId);

        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("Errore sendEmptyGameList: " + e.getMessage());
        }
    }

    public static void sendEmptyGameList(TelegramClient telegramClient, long chatId, String message) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .parseMode("Markdown")
                .build();

        sendEmptyGameListGif(telegramClient, chatId);

        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("Errore sendEmptyGameList: " + e.getMessage());
        }
    }

    public static InlineKeyboardMarkup buildLoadingKeyboard() {
        InlineKeyboardButton loadingBtn = InlineKeyboardButton.builder()
                .text("⏳ Aggiornamento...")
                .callbackData("DISABLED")
                .build();

        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(loadingBtn);

        return InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .build();
    }

    public static void sendEmptyGameListGif(TelegramClient telegramClient, long chatId){
        SendAnimation gif = SendAnimation.builder()
                .chatId(chatId)
                .animation(new InputFile("https://media.giphy.com/media/v1.Y2lkPWVjZjA1ZTQ3eGxuZWV0bDhiNndiZDlqYWN6d2s3cW15NnI4aTZ3NWxxbDlxend5OCZlcD12MV9naWZzX3NlYXJjaCZjdD1n/Tfqq9a3G83tsvJoBTs/giphy.gif"))
                .build();

        try {
            telegramClient.execute(gif);
        } catch (TelegramApiException e) {
            System.err.println("Errore sendEmptyGameListGif: " + e.getMessage());
        }
    }









    public static void sendTrailer(TelegramClient client, long chatId, String trailerUrl, String gameName) {
        InlineKeyboardButton btn = InlineKeyboardButton.builder()
                .text("▶️ Guarda Trailer")
                .url(trailerUrl)
                .build();

        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(btn);

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .build();

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text("🎬 Trailer – " + gameName)
                .replyMarkup(keyboard)
                .build();

        try {
            client.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("Errore sendTrailer: " + e.getMessage());
        }
    }





/*
    public static void sendTrailer(TelegramClient client, long chatId, String trailerUrl, GameResponse gameResponse) {
        SendVideo video = SendVideo.builder()
                .chatId(chatId)
                .video(new InputFile(trailerUrl))
                .caption("🎬 Trailer – " + gameResponse.results.get(0).name)
                .build();


        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text("🎬 Trailer:\n" + url)
                .build();


        try {
            client.execute(video);
        } catch (TelegramApiException e) {
            System.err.println("Errore sendTrailer: " + e.getMessage());
        }
    }

    */

    public static void sendNoTrailer(TelegramClient client, long chatId) {
        sendMessage(client, chatId, "🎬 Trailer non disponibile per questo gioco");
    }
}
