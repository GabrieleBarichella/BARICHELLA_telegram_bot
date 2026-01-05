import org.telegram.telegrambots.meta.api.objects.polls.input.InputPollOption;

import java.util.List;

public class QuizData {
    List<InputPollOption> options;
    int correctIndex;

    QuizData(List<InputPollOption> options, int correctIndex) {
        this.options = options;
        this.correctIndex = correctIndex;
    }
}
