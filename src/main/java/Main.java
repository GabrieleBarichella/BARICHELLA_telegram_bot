import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
public class Main {
    public static void main(String[] args) {

        String botToken = SingletonConfiguration.getInstance().getProperty("BOT_TOKEN");

        // Using try-with-resources to allow autoclose to run upon finishing
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, new AnimangaTracker(botToken));
            System.out.println("MyAmazingBot successfully started!");
            // Ensure this process wait forever
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}