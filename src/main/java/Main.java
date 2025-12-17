import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

//Creazione bot token --> https://www.andreaminini.com/web/telegram/come-creare-un-bot-su-telegram

public class Main {
    public static void main(String[] args) {
        String botToken = ConfigurationSingleton.getInstance().getProperty("BOT_TOKEN");

        // Using try-with-resources to allow autoclose to run upon finishing
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, new GameBot(botToken));
            System.out.println("MyAmazingBot successfully started!");
            // Ensure this prcess wait forever
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}