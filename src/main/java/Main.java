import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

//Creazione bot token --> https://www.andreaminini.com/web/telegram/come-creare-un-bot-su-telegram

public class Main {
    public static void main(String[] args) {
        String botToken = ConfigurationSingleton.getInstance().getProperty("BOT_TOKEN");

        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, new GameBot(botToken));
            System.out.println("GameBot si è avviato!");

            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}