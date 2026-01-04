import api.Anime;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AnimangaTracker implements org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer {

    private TelegramClient telegramClient;

    public AnimangaTracker(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    private void handleCommands(long chatId, String username, String text) {
        String command = text.toLowerCase().substring(1);
        switch (command) {
            case "start":
                handleStart(chatId, username);
                break;
            case "play":
                handlePlay(chatId);
                break;
            case "list":
                handleList(chatId);
                break;
            case "leaderboard":
                handleLeaderboard(chatId);
                break;
            case "suggestion":
                handleSuggestion(chatId);
                break;
            case "stats":
                handleStats(chatId);
                break;
            case "help":
                handleHelp(chatId);
                break;
            default:
                handleError(chatId);
                break;
        }
    }

    private InlineKeyboardMarkup buildAnimeKeyboard(Anime anime, String currentState) {
        if (currentState == null) currentState = "";

        InlineKeyboardButton watchingBtn = InlineKeyboardButton.builder()
                .text("Watching " + ("WATCHING".equals(currentState) ? "✅" : "❌"))
                .callbackData(anime.getId() + "_WATCHING")
                .build();

        InlineKeyboardButton completedBtn = InlineKeyboardButton.builder()
                .text("Completed " + ("COMPLETED".equals(currentState) ? "✅" : "❌"))
                .callbackData(anime.getId() + "_COMPLETED")
                .build();

        InlineKeyboardButton watchlistBtn = InlineKeyboardButton.builder()
                .text("Watchlist " + ("WATCHLIST".equals(currentState) ? "✅" : "❌"))
                .callbackData(anime.getId() + "_WATCHLIST")
                .build();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(watchingBtn);
        row1.add(completedBtn);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(watchlistBtn);

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .build();
    }

    private void handleInlineQuery(InlineQuery inlineQuery) {
        String query = inlineQuery.getQuery();
        List<Anime> results = API.search(query);
        List<InlineQueryResult> inlineResults = new ArrayList<>();

        for (Anime a : results) {
            InputTextMessageContent messageContent = InputTextMessageContent.builder()
                    .messageText(a.getId())
                    .build();

            InlineQueryResultArticle article = InlineQueryResultArticle.builder()
                    .id(a.getId())
                    .title(a.getAttributes().getCanonicalTitle())
                    .description(a.getAttributes().getSynopsis())
                    .thumbnailUrl(a.getAttributes().getPosterImage().getSmall())
                    .inputMessageContent(messageContent)
                    .build();

            inlineResults.add(article);
        }

        AnswerInlineQuery answer = AnswerInlineQuery.builder()
                .inlineQueryId(inlineQuery.getId())
                .results(inlineResults)
                .cacheTime(0)
                .build();

        try {
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();

        String[] parts = data.split("_");
        String animeId = parts[0];
        String newState = parts[1];

        try {
            Database db = Database.getInstance();
            db.addOrUpdateUserAnime(chatId, animeId, newState);

            String currentState = db.getUserAnimeState(chatId, animeId);
            Anime anime = API.searchById(animeId);

            InlineKeyboardMarkup keyboard = buildAnimeKeyboard(anime, currentState);

            EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .replyMarkup(keyboard)
                    .build();

            telegramClient.execute(edit);

            telegramClient.execute(
                    AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQuery.getId())
                            .text("Anime list updated!")
                            .showAlert(false)
                            .build()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStart(long chatId, String username) {
        try {
            Database db = Database.getInstance();
            db.addUser(chatId, username);
        } catch (SQLException e) {
            e.printStackTrace();
            SendMessage message = SendMessage.builder()
                    .parseMode("HTML")
                    .chatId(chatId)
                    .text("Database error, please try again later.")
                    .build();
            try {
                telegramClient.execute(message);
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
            return;
        }

        String text = """
            👋 <b>Welcome to AnimangaTracker!</b>

            📺 Track anime you watch
            🔍 Search titles using inline search
            🎮 Play fun guessing games
            📊 View personal statistics

            👉 To start searching, type:
            <code>@animangatrackerbot One Piece</code>

            📖 Use /help to see all available commands.
            """;

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text(text)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handlePlay(long chatId) {}

    private void handleList(long chatId) {}

    private void handleLeaderboard(long chatId) {}

    private void handleSuggestion(long chatId) {}

    private void handleStats(long chatId) {}

    private void handleHelp(long chatId) {
        String text = """
            📖 <b>AniMangaTracker – Help</b>

            Here is the list of available commands:

            🚀 <b>/start</b>
            Start the bot and see a short introduction.

            🔍 <b>Inline Search</b>
            Search anime anywhere on Telegram by typing:
            <code>@AniMangaTrackerBot One Piece</code>

            📺 <b>/list</b>
            View your personal anime list:
            • Watchlist
            • Watching
            • Completed

            🎮 <b>/play</b>
            Play anime guessing games and earn points.

            🏆 <b>/leaderboard</b>
            See the top users of minigames.

            🎯 <b>/suggestion</b>
            Get a random anime suggestion based on popularity.

            📊 <b>/stats</b>
            View your personal statistics:
            • Watched anime
            • Average rating
            • Game scores

            ℹ️ <b>/help</b>
            Show this help message.

            ✨ Tip:
            Use the inline buttons under messages to quickly manage your anime list!
            """;

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text(text)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleError(long chatId) {
        String text = """
            ⚠️ <b>Oops! Something went wrong</b>

            The command you entered is not recognized or cannot be processed.

            📖 Please use <b>/help</b> to see the list of available commands
            or try again using the inline buttons.
            """;

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text(text)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhotoWithButtons(long chatId, Anime anime, String currentState) {
        if (anime == null) return;
        InlineKeyboardMarkup keyboard = buildAnimeKeyboard(anime, currentState);
        String caption = "<b>" + anime.getAttributes().getCanonicalTitle() + "</b>\n"
                + "Episodi: " + anime.getAttributes().getEpisodeCount() + "\n"
                + "Rating: " + anime.getAttributes().getAverageRating() + "\n"
                + "Status: " + anime.getAttributes().getStatus() + "\n\n";

        SendPhoto photo = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(anime.getAttributes().getPosterImage().getOriginal()))
                .caption(caption)
                .parseMode("HTML")
                .replyMarkup(keyboard)
                .build();

        try {
            telegramClient.execute(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consume(Update update) {
        if (update.hasInlineQuery()) {
            handleInlineQuery(update.getInlineQuery());
            return;
        }

        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String username = update.getMessage().getFrom().getUserName();
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (API.isAnimeId(text)) {
                try {
                    Anime anime = API.searchById(text);
                    Database db = Database.getInstance();
                    String currentState = db.getUserAnimeState(chatId, anime.getId());
                    sendPhotoWithButtons(chatId, anime, currentState);
                } catch (SQLException e) {
                    e.printStackTrace();
                    SendMessage message = SendMessage.builder()
                            .chatId(chatId)
                            .parseMode("HTML")
                            .text("Database error, please try again later.")
                            .build();
                    try {
                        telegramClient.execute(message);
                    } catch (TelegramApiException ex) {
                        ex.printStackTrace();
                    }
                }
                return;
            }

            if (text.startsWith("/") || text.startsWith(".") || text.startsWith("!")) {
                handleCommands(chatId, username, text);
                return;
            }

            handleError(chatId);
        }
    }
}
