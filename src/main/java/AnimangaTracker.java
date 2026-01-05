import api.Anime;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.api.objects.polls.input.InputPollOption;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimangaTracker implements org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer {

    private TelegramClient telegramClient;
    private Map<String, GameInfo> activeGames = new HashMap<>();
    private Database db = null;

    public AnimangaTracker(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
        try { db = Database.getInstance(); }
        catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void consume(Update update) {

        if (update.hasInlineQuery()) { handleInlineQuery(update.getInlineQuery()); return; }

        if (update.hasCallbackQuery()) { handleCallbackQuery(update.getCallbackQuery()); return; }

        if (update.hasPollAnswer()) { handlePollAnswer(update.getPollAnswer()); return; }

        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            String username = update.getMessage().getFrom().getUserName();

            if (API.isAnimeId(text)) {
                Anime anime = API.searchById(text);
                db.addAnime(anime);
                sendPhotoWithButtons(chatId, anime, db.getUserAnimeState(chatId, anime.getId()));
                return;
            }

            if (text.startsWith("/") || text.startsWith(".") || text.startsWith("!")) handleCommands(chatId, username, text);
            else handleError(chatId);
        }
    }

    private void handleInlineQuery(InlineQuery inlineQuery) {
        String query = inlineQuery.getQuery();
        List<Anime> results = API.search(query);
        List<InlineQueryResult> inlineResults = new ArrayList<>();

        for (Anime a : results) {
            String animeId = a.getId();
            String animeTitle = a.getAttributes().getCanonicalTitle();
            String animeSynopsis = a.getAttributes().getSynopsis();
            String animeImageUrl = a.getAttributes().getPosterImage().getSmall();

            InputTextMessageContent messageContent = BuilderUtilities.buildMessageContent(animeId);
            InlineQueryResultArticle article = BuilderUtilities.buildResultArticle(animeId, animeTitle, animeSynopsis, animeImageUrl, messageContent);
            inlineResults.add(article);
        }

        try {
            TelegramUtilities.answerInlineQuery(telegramClient, inlineQuery.getId(), inlineResults);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();

        if (data.startsWith("CHOOSE_GAME_")) {
            String type = data.substring("CHOOSE_GAME_".length());
            if (type.equals("POSTER")) startPosterGame(chatId);
            else if (type.equals("STATS")) startStatsGame(chatId);
            return;
        }

        String[] parts = data.split("_");
        if (parts.length == 2) {
            String animeId = parts[0];
            String action = parts[1];

            try {
                if ("FAVORITE".equals(action)) {
                    if (db.isFavorite(chatId, animeId)) {
                        db.removeFavorite(chatId, animeId);
                    } else {
                        db.addFavorite(chatId, animeId);
                    }
                } else {
                    String currentState = db.getUserAnimeState(chatId, animeId);
                    if (action.equals(currentState)) {
                        db.removeUserAnime(chatId, animeId);
                    } else {
                        db.addOrUpdateUserAnime(chatId, animeId, action);
                    }
                }

                Anime anime = API.searchById(animeId);
                String state = db.getUserAnimeState(chatId, animeId);
                boolean isFavorite = db.isFavorite(chatId, animeId);

                InlineKeyboardMarkup keyboard = BuilderUtilities.buildAnimeKeyboard(anime, state, isFavorite);

                TelegramUtilities.editMessageReplyMarkup(telegramClient, chatId, messageId, keyboard);

                TelegramUtilities.answerCallBackQuery(telegramClient, callbackQuery.getId(), "Updated ✅");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePollAnswer(PollAnswer answer) {

        String pollId = answer.getPollId();

        if (!activeGames.containsKey(pollId)) return;

        GameInfo game = activeGames.get(pollId);
        int selectedOption = answer.getOptionIds().getFirst();
        boolean correct = selectedOption == game.correctOptionId;
        int points = correct ? game.points : 0;

        String text = correct
                ? "✅ Correct Answer! You Earned " + points + " points."
                : "❌ Wrong Answer! The correct option was: " + game.correctTitle;

        try {

            db.addUserGame(answer.getUser().getId(), game.gameId, points);
            TelegramUtilities.sendText(telegramClient, answer.getUser().getId(), text);

        } catch (Exception e) { e.printStackTrace(); }

        activeGames.remove(pollId);
    }

    private void sendPhotoWithButtons(long chatId, Anime anime, String currentState) {

        if (anime == null) return;

        try {
            boolean isFavorite = db.isFavorite(chatId, anime.getId());
            String animeImageUrl = anime.getAttributes().getPosterImage().getOriginal();
            InlineKeyboardMarkup keyboard = BuilderUtilities.buildAnimeKeyboard(anime, currentState, isFavorite);

            String caption = "<b>" + anime.getAttributes().getCanonicalTitle() + "</b>\n"
                    + "Episodes: " + anime.getAttributes().getEpisodeCount() + "\n"
                    + "Rating: " + anime.getAttributes().getAverageRating() + "\n"
                    + "Status: " + anime.getAttributes().getStatus() + "\n\n";

            TelegramUtilities.sendPhoto(telegramClient, chatId, animeImageUrl, caption, keyboard);
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void handleCommands(long chatId, String username, String text) {
        String command = text.toLowerCase().substring(1);
        switch (command) {
            case "start": handleStart(chatId, username); break;
            case "play": handlePlay(chatId); break;
            case "list": handleList(chatId); break;
            case "favorites": handleFavorites(chatId); break;
            case "leaderboard": handleLeaderboard(chatId); break;
            case "suggestion": handleSuggestion(chatId); break;
            case "stats": handleStats(chatId); break;
            case "help": handleHelp(chatId); break;
            default: handleError(chatId); break;
        }
    }

    private void handleStart(long chatId, String username) {

        db.addUser(chatId, username);

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

        try { TelegramUtilities.sendHtml(telegramClient, chatId, text); }
        catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void handlePlay(long chatId) {

        try {

            InlineKeyboardButton posterButton = BuilderUtilities.buildButton("🎨 Guess from Poster (1pt)", "CHOOSE_GAME_POSTER");
            InlineKeyboardButton statsButton = BuilderUtilities.buildButton("📊 Guess from Stats (3pt)", "CHOOSE_GAME_STATS");

            InlineKeyboardRow row1 = new InlineKeyboardRow();
            row1.add(posterButton);

            InlineKeyboardRow row2 = new InlineKeyboardRow();
            row2.add(statsButton);

            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(List.of(row1, row2)).build();

            TelegramUtilities.sendText(telegramClient, chatId, "Choose the game tou want to play:", markup);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleList(long chatId) {
        try {
            List<String> watchlist = db.getUserAnimeByState(chatId, "WATCHLIST");
            List<String> watching = db.getUserAnimeByState(chatId, "WATCHING");
            List<String> completed = db.getUserAnimeByState(chatId, "COMPLETED");

            StringBuilder text = new StringBuilder("📺 <b>Your Anime List</b>\n\n");
            text.append("📌 <b>Watchlist</b>\n"); watchlist.forEach(t -> text.append("• ").append(t).append("\n"));
            text.append("\n▶️ <b>Watching</b>\n"); watching.forEach(t -> text.append("• ").append(t).append("\n"));
            text.append("\n✅ <b>Completed</b>\n"); completed.forEach(t -> text.append("• ").append(t).append("\n"));

            TelegramUtilities.sendHtml(telegramClient, chatId, text.toString());

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleFavorites(long chatId) {
        try {
            List<String> favorites = db.getUserFavorites(chatId);

            if (favorites.isEmpty()) {
                TelegramUtilities.sendText(telegramClient, chatId, "⭐ You don't have any favorites yet.");
                return;
            }

            StringBuilder text = new StringBuilder("⭐ <b>Your Favorite Anime:</b>\n\n");
            for (String title : favorites) text.append("• ").append(title).append("\n");

            TelegramUtilities.sendHtml(telegramClient, chatId, text.toString());

        } catch (Exception e) {
            e.printStackTrace();
            try { TelegramUtilities.sendText(telegramClient, chatId, "❌ Error retrieving favorites."); }
            catch (TelegramApiException ex) { ex.printStackTrace(); }
        }
    }

    private void handleLeaderboard(long chatId) {
        try {
            List<Map<String, Object>> topUsers = db.getTopUsers(10);

            if (topUsers.isEmpty()) {
                TelegramUtilities.sendText(telegramClient, chatId, "🏆 The leaderboard is empty.");
                return;
            }

            StringBuilder text = new StringBuilder("🏆 <b>Leaderboard – Top Users</b>\n\n");
            int rank = 1;

            for (Map<String, Object> row : topUsers) {
                String user = row.get("username") != null
                        ? row.get("username").toString()
                        : row.get("chat_id").toString();
                int points = ((Number) row.get("total_points")).intValue();
                text.append(rank).append(". ").append(user).append(" – ").append(points).append(" points\n");
                rank++;
            }

            TelegramUtilities.sendHtml(telegramClient, chatId, text.toString());

        } catch (Exception e) {
            e.printStackTrace();
            try { TelegramUtilities.sendText(telegramClient, chatId, "⚠️ Error retrieving leaderboard."); }
            catch (TelegramApiException ex) { ex.printStackTrace(); }
        }
    }

    private void handleSuggestion(long chatId) {
        try {
            List<Anime> randomAnimeList = API.getRandomAnime(1);

            if (randomAnimeList.isEmpty()) {
                TelegramUtilities.sendText(telegramClient, chatId, "Could not find any suggestion 😔");
                return;
            }

            Anime anime = randomAnimeList.getFirst();
            String animeImageUrl = anime.getAttributes().getPosterImage().getOriginal();

            String caption = "<b>" + anime.getAttributes().getCanonicalTitle() + "</b>\n"
                    + "Episodes: " + anime.getAttributes().getEpisodeCount() + "\n"
                    + "Rating: " + anime.getAttributes().getAverageRating() + "\n"
                    + "Status: " + anime.getAttributes().getStatus();

            TelegramUtilities.sendPhoto(telegramClient, chatId, animeImageUrl, caption);

        } catch (Exception e) {
            e.printStackTrace();
            try { TelegramUtilities.sendText(telegramClient, chatId, "Error suggesting anime 😔"); }
            catch (TelegramApiException ex) { ex.printStackTrace(); }
        }
    }

    private void handleStats(long chatId) {

        try {

            Map<String, Integer> gameStats = db.getUserGameStats(chatId);
            int completedAnime = db.getCompletedAnimeCount(chatId);

            int totalScore = gameStats.get("total_score");
            int gamesPlayed = gameStats.get("games_played");

            String text = """
            📊 <b>Your statistics</b>

            🎮 <b>Total Points:</b> %d
            🕹️ <b>Games Played:</b> %d
            ✅ <b>Completed Anime:</b> %d
            """.formatted(totalScore, gamesPlayed, completedAnime);

            TelegramUtilities.sendHtml(telegramClient, chatId, text);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                TelegramUtilities.sendText(telegramClient, chatId, "❌ Errore retrieving statistics.");
            } catch (TelegramApiException ignored) {}
        }
    }

    private void handleHelp(long chatId) {
        String text = """ 
        📖 <b>AnimangaTracker – Help</b>
        Here is the list of available commands:
        
        🚀 <b>/start</b>
        Start the bot and see a short introduction.
        
        🔍 <b>Inline Search</b>
        Search anime anywhere on Telegram by typing:
        <code>@AnimangaTrackerBot One Piece</code>
        
        📺 <b>/list</b>
        View your personal anime list:
        • Watchlist
        • Watching
        • Completed
        
        ⭐ <b>/favorites</b>
        View your favorite anime list.
        
        🎮 <b>/play</b>
        Play anime guessing games and earn points.
        
        🏆 <b>/leaderboard</b>
        See the top users of minigames.
        
        🎯 <b>/suggestion</b>
        Get a random anime suggestion.
        
        📊 <b>/stats</b>
        View your personal statistics:
        • Watched anime
        • Game scores
        
        ℹ️ <b>/help</b>
        Show this help message.
        
        ✨ <b>Tip:</b>
        Use the inline buttons under messages to quickly manage your anime list!
        """;

        try {
            TelegramUtilities.sendHtml(telegramClient, chatId, text);
        }
        catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void startPosterGame(long chatId) {

        try {
            List<Anime> options = API.getRandomAnime(3);
            if (options.size() < 3) return;

            Anime correctAnime = options.get((int) (Math.random() * 3));
            String posterUrl = correctAnime.getAttributes().getPosterImage().getOriginal();

            QuizData quizData = buildQuizData(options, correctAnime);

            TelegramUtilities.sendPhoto(telegramClient, chatId, posterUrl, "🎨 Here's the poster! Now guess...");

            Message pollMessage = TelegramUtilities.sendQuiz(telegramClient, chatId, "🎨 Guess the anime from its poster!", quizData.options, quizData.correctIndex);

            String pollId = pollMessage.getPoll() != null ? pollMessage.getPoll().getId() : "POSTER_" + System.currentTimeMillis();

            activeGames.put(pollId, new GameInfo(quizData.correctIndex, 1, 1, correctAnime.getAttributes().getCanonicalTitle()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startStatsGame(long chatId) {

        try {
            List<Anime> options = API.getRandomAnime(3);
            if (options.size() < 3) return;

            Anime correctAnime = options.get((int) (Math.random() * 3));

            String statsText = "📊 Guess the anime from its data!\n\n" +
                "Episodes: " + correctAnime.getAttributes().getEpisodeCount() + " • " +
                "Rating: " + correctAnime.getAttributes().getAverageRating() + " • " +
                "Status: " + correctAnime.getAttributes().getStatus();

            QuizData quizData = buildQuizData(options, correctAnime);

            Message pollMessage = TelegramUtilities.sendQuiz(telegramClient, chatId, statsText, quizData.options, quizData.correctIndex);

            String pollId = pollMessage.getPoll() != null ? pollMessage.getPoll().getId() : "STATS_" + System.currentTimeMillis();

            activeGames.put(pollId, new GameInfo(quizData.correctIndex, 2, 3, correctAnime.getAttributes().getCanonicalTitle()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private QuizData buildQuizData(List<Anime> options, Anime correctAnime) {

        List<InputPollOption> pollOptions = new ArrayList<>();
        int correctIndex = -1;

        for (int i = 0; i < options.size(); i++) {
            Anime anime = options.get(i);
            String title = anime.getAttributes().getCanonicalTitle();

            pollOptions.add(new InputPollOption(title));

            if (anime.getId().equals(correctAnime.getId())) correctIndex = i;
        }

        return new QuizData(pollOptions, correctIndex);
    }

    private void handleError(long chatId) {
        String text = "⚠️ <b>Oops! Command not recognized</b>\nUse /help to see available commands.";
        try {
            TelegramUtilities.sendHtml(telegramClient, chatId, text);
        } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}
