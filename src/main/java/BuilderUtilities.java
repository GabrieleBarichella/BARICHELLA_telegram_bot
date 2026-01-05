import api.Anime;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

public class BuilderUtilities {

    public static InputTextMessageContent buildMessageContent(String animeId) {
        return InputTextMessageContent.builder()
                .messageText(animeId)
                .build();
    }

    public static InlineQueryResultArticle buildResultArticle(String animeId, String animeTitle, String animeSynopsis, String animeImageUrl, InputTextMessageContent message) {
        return InlineQueryResultArticle.builder()
                .id(animeId)
                .title(animeTitle)
                .description(animeSynopsis)
                .thumbnailUrl(animeImageUrl)
                .inputMessageContent(message)
                .build();
    }

    public static InlineKeyboardButton buildButton(Anime anime, String currentState, String genericState) {
        String text = toTitleCase(genericState) + " " + (genericState.equals(currentState) ? "✅" : "❌");
        String callbackData = anime.getId() + "_" + genericState;

        return buildButton(text, callbackData);
    }

    public static InlineKeyboardButton buildButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    public static InlineKeyboardMarkup buildAnimeKeyboard(Anime anime, String currentState, boolean isFavorite) {

        if (currentState == null) currentState = "";

        InlineKeyboardButton watchingBtn = buildButton(anime, currentState, "WATCHING");
        InlineKeyboardButton completedBtn = buildButton(anime, currentState, "COMPLETED");
        InlineKeyboardButton watchlistBtn = buildButton(anime, currentState, "WATCHLIST");

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

    private static String toTitleCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

}
