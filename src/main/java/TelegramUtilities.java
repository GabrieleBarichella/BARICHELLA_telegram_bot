import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.polls.input.InputPollOption;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

public class TelegramUtilities {

    public static Message sendQuiz(TelegramClient client, long chatId, String text, List<InputPollOption> pollOptions, int correctIndex) throws TelegramApiException {
        SendPoll poll = SendPoll.builder()
                .chatId(chatId)
                .question(text)
                .options(pollOptions)
                .type("quiz")
                .correctOptionId(correctIndex)
                .isAnonymous(false)
                .build();

        return client.execute(poll);
    }

    public static void sendHtml(TelegramClient client, long chatId, String text) throws TelegramApiException {

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text(text)
                .build();

        client.execute(message);
    }

    public static void sendText(TelegramClient client, long chatId, String text) throws TelegramApiException {

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        client.execute(message);
    }

    public static void sendText(TelegramClient client, long chatId, String text, InlineKeyboardMarkup markup) throws TelegramApiException {

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(markup)
                .build();

        client.execute(message);
    }

    public static void sendPhoto(TelegramClient client, long chatId, String animeImageUrl, String text) throws TelegramApiException {

        SendPhoto photo = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(animeImageUrl))
                .caption(text)
                .parseMode("HTML")
                .build();

        client.execute(photo);
    }

    public static void sendPhoto(TelegramClient client, long chatId, String animeImageUrl, String text, InlineKeyboardMarkup markup) throws TelegramApiException {

        SendPhoto photo = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(animeImageUrl))
                .caption(text)
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();

        client.execute(photo);
    }

    public static void answerInlineQuery(TelegramClient client, String queryId, List<InlineQueryResult> inlineResults) throws TelegramApiException {

        AnswerInlineQuery answer = AnswerInlineQuery.builder()
                .inlineQueryId(queryId)
                .results(inlineResults)
                .cacheTime(0)
                .build();

        client.execute(answer);
    }

    public static void answerCallBackQuery(TelegramClient client, String queryId, String text) throws TelegramApiException {

        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .callbackQueryId(queryId)
                .text(text)
                .build();

        client.execute(answer);
    }

    public static void editMessageReplyMarkup(TelegramClient client, long chatId, int messageId, InlineKeyboardMarkup keyboard) throws TelegramApiException {

        EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(messageId)
                .replyMarkup(keyboard)
                .build();

        client.execute(edit);
    }
}
