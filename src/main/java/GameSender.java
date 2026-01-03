import modelli.Game;
import modelli.Genre;
import modelli.PlatformWrapper;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

public class GameSender {

    public static void sendGame(
            TelegramClient client,
            long chatId,
            Game game
    ) throws TelegramApiException {

        String caption = buildText(game);
        InlineKeyboardMarkup keyboard = buildKeyboard(game);

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
            //fallback senza immagine
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId)
                    .text(caption)
                    .replyMarkup(keyboard)
                    .build();

            client.execute(msg);
        }
    }

    // ===== TESTO =====
    private static String buildText(Game game) {

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
        if (generi.isEmpty())
            generi = "N/D";

        return """
            🎮 %s
            🗓 Uscita: %s
            ⭐ Rating: %.1f
            🏆 Metacritic: %d
            🖥 Piattaforme: %s
            🏷 Generi: %s
            """.formatted(
                game.name,
                game.released != null ? game.released : "N/D",
                game.rating,
                game.metacritic,
                piattaforme,
                generi
        );
    }

    // ===== BOTTONI =====
    private static InlineKeyboardMarkup buildKeyboard(Game game) {

        InlineKeyboardButton libraryBtn = InlineKeyboardButton.builder()
                .text("➕ Libreria")
                .callbackData("LIB_" + game.id)
                .build();

        InlineKeyboardButton wishlistBtn = InlineKeyboardButton.builder()
                .text("❤️ Wishlist")
                .callbackData("WISH_" + game.id)
                .build();

        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(libraryBtn);
        row.add(wishlistBtn);

        return InlineKeyboardMarkup.builder()
                .keyboardRow(row)
                .build();
    }
}
