import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
public class Main {
    public static void main(String[] args) {

        String botToken = SingletonConfiguration.getInstance().getProperty("BOT_TOKEN");

        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, new AnimangaTracker(botToken));
            System.out.println("Bot successfully started!");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
