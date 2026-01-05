import api.Anime;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
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
    Map<String, GameInfo> activeGames = new HashMap<>();

    public AnimangaTracker(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
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

    private InlineKeyboardMarkup buildAnimeKeyboard(Anime anime, String currentState, boolean isFavorite) {
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

        InlineKeyboardButton favoriteBtn = InlineKeyboardButton.builder()
                .text("Favorite " + (isFavorite ? "⭐" : "☆"))
                .callbackData(anime.getId() + "_FAVORITE")
                .build();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(watchingBtn);
        row1.add(completedBtn);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(watchlistBtn);
        row2.add(favoriteBtn);

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .build();
    }

    private void handlePollAnswer(PollAnswer answer) {
        String pollId = answer.getPollId();

        if (!activeGames.containsKey(pollId)) return;

        GameInfo game = activeGames.get(pollId);
        int selectedOption = answer.getOptionIds().getFirst();
        boolean correct = selectedOption == game.correctOptionId;

        try {
            Database db = Database.getInstance();
            int points = correct ? game.points : 0;
            db.addUserGame(answer.getUser().getId(), game.gameId, points);

            String text = correct
                    ? "✅ Correct Answer! You Earned " + points + " points."
                    : "❌ Wrong Answer! The correct option was: " + game.correctTitle;

            SendMessage msg = SendMessage.builder()
                    .chatId(answer.getUser().getId())
                    .text(text)
                    .build();

            telegramClient.execute(msg);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            activeGames.remove(pollId);
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
                Database db = Database.getInstance();

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

                InlineKeyboardMarkup keyboard =
                        buildAnimeKeyboard(anime, state, isFavorite);

                EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(keyboard)
                        .build();

                telegramClient.execute(edit);

                telegramClient.execute(
                        AnswerCallbackQuery.builder()
                                .callbackQueryId(callbackQuery.getId())
                                .text("Aggiornato ✅")
                                .build()
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startPosterGame(long chatId) {
        try {
            List<Anime> options = API.getRandomAnime(3);
            if (options.size() < 3) return;

            Anime correctAnime = options.get((int)(Math.random() * 3));

            List<InputPollOption> pollOptions = new ArrayList<>();
            int correctIndex = -1;

            for (int i = 0; i < options.size(); i++) {
                Anime a = options.get(i);
                pollOptions.add(new InputPollOption(a.getAttributes().getCanonicalTitle()));
                if (a.getId().equals(correctAnime.getId())) correctIndex = i;
            }

            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(correctAnime.getAttributes().getPosterImage().getOriginal()))
                    .caption("🎨 Here's the poster! Now guess...")
                    .build();
            telegramClient.execute(photo);

            SendPoll poll = SendPoll.builder()
                    .chatId(chatId)
                    .question("🎨 Guess the anime from its poster!")
                    .options(pollOptions)
                    .type("quiz")
                    .correctOptionId(correctIndex)
                    .isAnonymous(false)
                    .build();

            Message pollMessage = telegramClient.execute(poll);

            String fakePollId = pollMessage.getPoll() != null ? pollMessage.getPoll().getId() : "FAKE_" + System.currentTimeMillis();
            activeGames.put(fakePollId, new GameInfo(correctIndex, 1, 1, correctAnime.getAttributes().getCanonicalTitle()));

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

            List<InputPollOption> pollOptions = new ArrayList<>();
            int correctIndex = -1;

            for (int i = 0; i < options.size(); i++) {
                Anime a = options.get(i);
                pollOptions.add(new InputPollOption(a.getAttributes().getCanonicalTitle()));
                if (a.getId().equals(correctAnime.getId())) correctIndex = i;
            }

            SendPoll poll = SendPoll.builder()
                    .chatId(chatId)
                    .question(statsText)
                    .options(pollOptions)
                    .type("quiz")
                    .correctOptionId(correctIndex)
                    .isAnonymous(false)
                    .build();

            Message pollMessage = telegramClient.execute(poll);

            String pollId = pollMessage.getPoll() != null ? pollMessage.getPoll().getId() : "STATS_" + System.currentTimeMillis();
            activeGames.put(pollId, new GameInfo(correctIndex, 2, 3, correctAnime.getAttributes().getCanonicalTitle()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStart(long chatId, String username) {
        try { Database db = Database.getInstance(); db.addUser(chatId, username); } catch (SQLException e) { e.printStackTrace(); }

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

        try { telegramClient.execute(SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).build()); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void handlePlay(long chatId) {
        try {
            InlineKeyboardRow row1 = new InlineKeyboardRow();
            row1.add(InlineKeyboardButton.builder().text("🎨 Guess from Poster (1pt)").callbackData("CHOOSE_GAME_POSTER").build());
            InlineKeyboardRow row2 = new InlineKeyboardRow();
            row2.add(InlineKeyboardButton.builder().text("📊 Guess from Stats (3pt)").callbackData("CHOOSE_GAME_STATS").build());

            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(List.of(row1, row2)).build();

            telegramClient.execute(SendMessage.builder().chatId(chatId).text("Choose the game tou want to play:").replyMarkup(markup).build());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleList(long chatId) {
        try {
            Database db = Database.getInstance();
            List<String> watchlist = db.getUserAnimeByState(chatId, "WATCHLIST");
            List<String> watching = db.getUserAnimeByState(chatId, "WATCHING");
            List<String> completed = db.getUserAnimeByState(chatId, "COMPLETED");

            StringBuilder text = new StringBuilder("📺 <b>Your Anime List</b>\n\n");
            text.append("📌 <b>Watchlist</b>\n"); watchlist.forEach(t -> text.append("• ").append(t).append("\n"));
            text.append("\n▶️ <b>Watching</b>\n"); watching.forEach(t -> text.append("• ").append(t).append("\n"));
            text.append("\n✅ <b>Completed</b>\n"); completed.forEach(t -> text.append("• ").append(t).append("\n"));

            telegramClient.execute(SendMessage.builder().chatId(chatId).parseMode("HTML").text(text.toString()).build());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleFavorites(long chatId) {
        try {
            Database db = Database.getInstance();
            List<String> favorites = db.getUserFavorites(chatId);

            if (favorites.isEmpty()) {
                telegramClient.execute(
                        SendMessage.builder()
                                .chatId(chatId)
                                .text("⭐ You don't have any favorites yet.")
                                .build()
                );
                return;
            }

            StringBuilder text = new StringBuilder("⭐ <b>Your Favorite Anime:</b>\n\n");
            for (String title : favorites) {
                text.append("• ").append(title).append("\n");
            }

            telegramClient.execute(
                    SendMessage.builder()
                            .chatId(chatId)
                            .parseMode("HTML")
                            .text(text.toString())
                            .build()
            );

        } catch (Exception e) {
            e.printStackTrace();
            try {
                telegramClient.execute(
                        SendMessage.builder()
                                .chatId(chatId)
                                .text("❌ Error retrieving favorites.")
                                .build()
                );
            } catch (TelegramApiException ignored) {}
        }
    }

    private void handleLeaderboard(long chatId) {
        try {
            Database db = Database.getInstance();
            List<Map<String, Object>> topUsers = db.getTopUsers(10);

            if (topUsers.isEmpty()) {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("🏆 The leaderboard is empty.")
                        .build());
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

            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .parseMode("HTML")
                    .text(text.toString())
                    .build());

        } catch (Exception e) {
            e.printStackTrace();
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("⚠️ Error retrieving leaderboard.")
                        .build());
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleSuggestion(long chatId) {
        try {
            List<Anime> randomAnimeList = API.getRandomAnime(1);

            if (randomAnimeList.isEmpty()) {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("Could not find any suggestion 😔")
                        .build());
                return;
            }

            Anime anime = randomAnimeList.getFirst();

            String caption = "<b>" + anime.getAttributes().getCanonicalTitle() + "</b>\n"
                    + "Episodes: " + anime.getAttributes().getEpisodeCount() + "\n"
                    + "Rating: " + anime.getAttributes().getAverageRating() + "\n"
                    + "Status: " + anime.getAttributes().getStatus();

            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(anime.getAttributes().getPosterImage().getOriginal()))
                    .caption(caption)
                    .parseMode("HTML")
                    .build();

            telegramClient.execute(photo);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("Error suggesting anime 😔")
                        .build());
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleStats(long chatId) {
        try {
            Database db = Database.getInstance();

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

            SendMessage msg = SendMessage.builder()
                    .chatId(chatId)
                    .parseMode("HTML")
                    .text(text)
                    .build();

            telegramClient.execute(msg);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                telegramClient.execute(
                        SendMessage.builder()
                                .chatId(chatId)
                                .text("❌ Errore retrieving statistics.")
                                .build()
                );
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

        try { telegramClient.execute(SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).build()); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void handleError(long chatId) {
        String text = "⚠️ <b>Oops! Command not recognized</b>\nUse /help to see available commands.";
        try { telegramClient.execute(SendMessage.builder().chatId(chatId).parseMode("HTML").text(text).build()); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void sendPhotoWithButtons(long chatId, Anime anime, String currentState) {
        if (anime == null) return;
        try {
            boolean isFavorite = Database.getInstance().isFavorite(chatId, anime.getId());
            InlineKeyboardMarkup keyboard = buildAnimeKeyboard(anime, currentState, isFavorite);
            String caption = "<b>" + anime.getAttributes().getCanonicalTitle() + "</b>\n"
                    + "Episodes: " + anime.getAttributes().getEpisodeCount() + "\n"
                    + "Rating: " + anime.getAttributes().getAverageRating() + "\n"
                    + "Status: " + anime.getAttributes().getStatus() + "\n\n";

            telegramClient.execute(SendPhoto.builder().chatId(chatId).photo(new InputFile(anime.getAttributes().getPosterImage().getOriginal())).caption(caption).parseMode("HTML").replyMarkup(keyboard).build());
        }
        catch (Exception e) { e.printStackTrace(); }
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
                try {
                    Anime anime = API.searchById(text);
                    Database db = Database.getInstance();
                    db.addAnime(anime);
                    sendPhotoWithButtons(chatId, anime, db.getUserAnimeState(chatId, anime.getId()));
                } catch (SQLException e) { e.printStackTrace(); }
                return;
            }

            if (text.startsWith("/") || text.startsWith(".") || text.startsWith("!")) handleCommands(chatId, username, text);
            else handleError(chatId);
        }
    }
}
