import api.Anime;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
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

import java.util.ArrayList;
import java.util.List;

public class AnimangaTracker implements org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer {

    private TelegramClient telegramClient;

    public AnimangaTracker(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    private void handleCommands(long chatId, String text) {
        String command = text.toLowerCase().substring(1);
        switch (command) {
            case "start":
                handleStart(chatId);
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

    }

    private void handleStart(long chatId) {
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
            throw new RuntimeException(e);
        }
    }

    private void handlePlay(long chatId) {

    }

    private void handleList(long chatId) {

    }

    private void handleLeaderboard(long chatId) {

    }

    private void handleSuggestion(long chatId) {

    }

    private void handleStats(long chatId) {

    }

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

    private void sendPhotoWithButtons(long chatId, Anime anime) {
        if (anime == null) return;

        String caption = "<b>" + anime.getAttributes().getCanonicalTitle() + "</b>\n"
                + "Episodi: " + anime.getAttributes().getEpisodeCount() + "\n"
                + "Rating: " + anime.getAttributes().getAverageRating() + "\n"
                + "Status: " + anime.getAttributes().getStatus() + "\n\n";

        InlineKeyboardButton watchButton = InlineKeyboardButton.builder()
                .text("Inserisci in anime da vedere")
                .callbackData("WATCH_" + anime.getId())
                .build();

        InlineKeyboardButton doneButton = InlineKeyboardButton.builder()
                .text("Segna come visto")
                .callbackData("DONE_" + anime.getId())
                .build();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(watchButton);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(doneButton);

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .build();

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
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (API.isAnimeId(text)) {
                Anime anime = API.searchById(text);
                sendPhotoWithButtons(chatId, anime);
                return;
            }

            if(text.startsWith("/") || text.startsWith(".") || text.startsWith("!")) {
                handleCommands(chatId, text);
                return;
            }

            handleError(chatId);
        }
    }
}
