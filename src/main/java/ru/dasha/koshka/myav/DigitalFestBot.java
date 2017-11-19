package ru.dasha.koshka.myav;

import com.vdurmont.emoji.EmojiParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendSticker;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.dasha.koshka.model.ActivityType;
import ru.dasha.koshka.model.FestActivity;
import ru.dasha.koshka.model.FestCourse;
import ru.dasha.koshka.model.FestSchedule;
import ru.dasha.koshka.utils.DBUtils;
import ru.dasha.koshka.utils.ScheduleUtils;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DigitalFestBot extends TelegramLongPollingBot {
    private static final Logger logger = LogManager.getLogger(DigitalFestBot.class.getName());
    private static final String BOT_TOKEN = "454379716:AAGuvTIuqe2ElAxiTt6G3tUGhYla2h4Cx8Q";

    private static final String CHOOSE_SCHEDULE_MESSAGE = "Выбери интересующую тебя категорию:";
    private static final String CHOOSE_LECTURE_MESSAGE = "Выбери интересующую тебя лекцию:";
    private static final String CHOOSE_LECTURE_AFTER_CORRECT_WORD_MESSAGE = "Давай выберем, куда пойдём дальше:";
    private static final String CORRECT_ANSWER_MESSAGE = "Правильно!" +
            EmojiParser.parseToUnicode(":+1:") + " Количество набранных баллов:%s";
    private static final String NO_MORE_LECTURES_MESSAGE = "К сожалению, сегодня для тебя лекции закончились" +
            EmojiParser.parseToUnicode(":sob:");

    private static final String LECTURE_CATEGORY_NAME = EmojiParser.parseToUnicode(":pencil2: Лекции");
    private static final String OTHER_CATEGORY_NAME = EmojiParser.parseToUnicode(":video_game: Активности");
    private static final String MY_POINTS_NAME = EmojiParser.parseToUnicode(":eyeglasses: Мои очки");

    private static final String YES_ANSWER_NAME = EmojiParser.parseToUnicode(":white_check_mark: Да");
    private static final String NO_ANSWER_NAME = EmojiParser.parseToUnicode(":red_circle: Нет");
    private static final String CANCEL_ANSWER_NAME = EmojiParser.parseToUnicode(":x: Отмена");

    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
    private static FestSchedule festLecturesSchedule = null;
    private static FestSchedule festGameSchedule = null;
    private static List<FestCourse> festCourses = null;

    private static final List<String> sadStickers = Arrays.asList(StickerConstants.STICKER_SAD,
            StickerConstants.STICKER_CRYING, StickerConstants.STICKER_DO_NOT_KNOW);
    private static final List<String> happyStickers = Arrays.asList(StickerConstants.STICKER_BU_DUM_TSS,
            StickerConstants.STICKER_CLAP_CLAP, StickerConstants.STICKER_GOOD, StickerConstants.STICKER_YESS,
            StickerConstants.STICKER_YOU_WIN);


    public DigitalFestBot() {
        super();
        updateSchedule(null);
        MyTimerTask timerTask = new MyTimerTask();
        TimeWorker tw = new TimeWorker(timerTask);
        tw.start();
    }

    private void updateSchedule(Long chatId) {
        try {
            festLecturesSchedule = ScheduleUtils.createSchedule();
            festGameSchedule = ScheduleUtils.createGameSchedule();
            festCourses = ScheduleUtils.createCourses();
        } catch (Exception e) {
            if (chatId != null) {
                sendMessage(chatId, "Упссс! Произошла ошибка при обновлении расписания. Обратитесь к разработчику!", null);
            }
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            final String messageText = message.hasText() ? update.getMessage().getText() : "";
            long chatId = message.getChatId();
            long userId = message.getFrom().getId();
            if ("/start".equals(messageText)) {
                doStart(update.getMessage());
            } else if ("/updateSchedule".toLowerCase().equals(messageText.toLowerCase())) {
                updateSchedule(chatId);
            } else {
                int userAction = DBUtils.getUserAction(userId);
                switch (userAction) {
                    case 1://ожидание ввода возраста
                        setAge(messageText, chatId, userId);
                        break;
                    case 2: //работа с расписанием
                        if (LECTURE_CATEGORY_NAME.equals(messageText)) {
                            showActualActivities(chatId, userId, true);
                        } else if (OTHER_CATEGORY_NAME.equals(messageText)) {
                            //showGamesSchedule(chatId, userId);
                            showActualActivities(chatId, userId, false);
                        } else if (MY_POINTS_NAME.equals(messageText)) {
                            sendMyPointsMessage(userId);
                        }
                        break;
                    case 3: //угадывание контрольного слова
                        checkWord(message);
                    default:
                        break;
                }
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Message message = callbackQuery.getMessage();
            long chatId = message.getChatId();
            long userId = chatId;//todo!!!
            String data = callbackQuery.getData();
            if (data.startsWith("yesButton")) {
                if (data.endsWith("like")) {
                    processLikeEventButton(message);
                } else {
                    String activityIdStr = data.replace("yesButton:", "");
                    processYesButton(message, activityIdStr);
                }
            } else if (data.startsWith("noButton")) {
                if (data.endsWith("dislike")) {
                    processDislikeEventButton(message);
                } else {
                    processNoButton(message);
                }
            } else if ("childButton".equals(data) || "parentButton".equals(data)) {
                processParentChildButton(message, data);
            } else if (data.startsWith("activityButton:")) {
                String activityIdStr = data.replace("activityButton:", "");
                if ("no".equals(activityIdStr)) {
                    sendMessage(chatId, "Тогда посмотри расписание активностей:", null);
                    showGamesSchedule(chatId, userId);
                } else {
                    sendActivityMessage(chatId, Integer.parseInt(activityIdStr));
                }
            } else if ("cancel".equals(data)) {
                DBUtils.setUserAction(chatId, 2, null);
                editMessage(message, message.getText().substring(0, message.getText().indexOf("...") + 1));
                chooseEvent(chatId);
            }

        }

    }

    private void processYesButton(Message message, String activityIdStr) {
        int activityId = Integer.parseInt(activityIdStr);
        FestActivity activity = getActivityById(activityId);
        if (activity == null) {
            return;
        }
        long chatId = message.getChatId();
        editMessage(message, message.getText().substring(0, message.getText().indexOf(".")));
        boolean isWordGuessed = DBUtils.getActivityWordAmount(chatId, activityId) > 0;
        if (isWordGuessed) {
            sendMessage(chatId, "Ты уже угадал секретное слово!", null);
            chooseEvent(chatId);
        } else {
            DBUtils.setUserAction(chatId, 3, activityId);
            sendMessage(message.getChatId(), "Введите кодовое слово:", new ReplyKeyboardRemove());
            /*SendMessage enterSecretWordMessage = new SendMessage()
                    .setChatId(chatId)
                    .setText("Введите кодоое слово:")
                    .setReplyMarkup(new ReplyKeyboardRemove());
            try {
                execute(enterSecretWordMessage);
            } catch (TelegramApiException e) {
               logger.error(e.getMessage(), e);
            }*/
        }
    }

    private void processNoButton(Message message) {
        editMessage(message, message.getText().substring(0, message.getText().indexOf(".") + 1));
        sendMessage(message.getChatId(), CHOOSE_LECTURE_AFTER_CORRECT_WORD_MESSAGE, null);
        //chooseEvent(message.getChatId());
    }

    private void processParentChildButton(Message message, String data) {
        long chatId = message.getChatId();
        DBUtils.createUser(chatId,
                "childButton".equals(data),
                message.getChat().getFirstName() + " " + message.getChat().getLastName());
        editMessage(message, message.getText().substring(0, message.getText().lastIndexOf("!") + 1));
        if ("childButton".equals(data)) {
            askAge(message);
        } else {
            chooseEvent(chatId);
        }
    }

    private void chooseEvent(long chatId) {
        SendMessage chooseScheduleMessage = new SendMessage()
                .setChatId(chatId)
                .setText(CHOOSE_SCHEDULE_MESSAGE);

        createEventKeyboard(chooseScheduleMessage);

    }

    private void sendMessage(long chatId, String messageText, ReplyKeyboard replyKeyboard) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(messageText)
                .setReplyMarkup(replyKeyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void editMessage(Message message, String newText) {
        EditMessageText editMessageText = new EditMessageText().
                setChatId(message.getChatId()).
                setMessageId(message.getMessageId()).
                setText(newText);
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void sendSticker(long chatId, String stickerName) {
        try {
            sendSticker(new SendSticker().
                    setChatId(chatId).
                    setSticker(stickerName));
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void createEventKeyboard(SendMessage message) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(LECTURE_CATEGORY_NAME);
        row.add(OTHER_CATEGORY_NAME);
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(MY_POINTS_NAME);
        keyboard.add(row);
        /*for (int i = 0; i < buttonNames.size(); i += 2) {
            KeyboardRow row = new KeyboardRow();
            row.add(buttonNames.get(i));
            if (i + 1 < buttonNames.size()) {
                row.add(buttonNames.get(i + 1));
            }
            keyboard.add(row);
        }*/
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void sendActivityMessage(long chatId, int activityId) {
        FestActivity activity = getActivityById(activityId);
        if (activity == null) {
            return;
        }
        String messageText = ActivityType.GAME.equals(activity.getType()) ?
                activity.getName() + "\nМесто: " + activity.getRoom() + ".\nВы пойдёте?" :
                String.format("В %s начнётся лекция в кабинете %s на тему '%s'. Вы пойдёте?",
                        timeFormatter.format(activity.getStartTime()), activity.getRoom(), activity.getName());
        SendMessage nextActivityMessage = new SendMessage()
                .setChatId(chatId)
                .setText(messageText);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        List<List<InlineKeyboardButton>> allButtons = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(YES_ANSWER_NAME);
        yesButton.setCallbackData("yesButton:" + activityId);
        buttons.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(NO_ANSWER_NAME);
        noButton.setCallbackData("noButton:" + activityId);
        buttons.add(noButton);
        allButtons.add(buttons);
        markup.setKeyboard(allButtons);
        nextActivityMessage.setReplyMarkup(markup);
        try {
            execute(nextActivityMessage);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }


    private void checkWord(Message m) {
        int activityId = DBUtils.getUserActionActivityId(m.getChatId());
        FestActivity activity = getActivityById(activityId);
        if (activity == null) {
            return;
        }
        String word = m.getText();
        if (word != null && activity.getSecret() != null &&
                word.toLowerCase().equals(activity.getSecret().toLowerCase())) {
            int correctWordsAmount = DBUtils.setCorrectWord(m.getChatId(), activityId, word);
            if (correctWordsAmount > 0) {
                sendSticker(m.getChatId(), getRandomSticker(true));
                sendMessage(m.getChatId(), String.format(CORRECT_ANSWER_MESSAGE, correctWordsAmount), null);
                createEventKeyboard(new SendMessage()
                        .setChatId(m.getChatId())
                        .setText(CHOOSE_LECTURE_AFTER_CORRECT_WORD_MESSAGE));
            }
        } else {
            senWrongAnswerMessage(m.getChatId());
        }
    }

    private void senWrongAnswerMessage(long chatId) {
        sendSticker(chatId, getRandomSticker(false));
        SendMessage wrongAnswerMessage = new SendMessage()
                .setChatId(chatId)
                .setText("Упс! Неправильно! Попробуй еще раз... " +
                        "Или нажми Отмена для выхода из режима ввода кодового слова.");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        List<List<InlineKeyboardButton>> allButtons = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_ANSWER_NAME);
        cancelButton.setCallbackData("cancel");
        buttons.add(cancelButton);
        allButtons.add(buttons);
        markup.setKeyboard(allButtons);
        wrongAnswerMessage.setReplyMarkup(markup);
        try {
            execute(wrongAnswerMessage);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String getUserName(Message message) {
        String result = "";
        if (message.getFrom() != null) {
            if (message.getFrom().getFirstName() != null) {
                result += message.getFrom().getFirstName();
            }
        }
        return result;
    }

    private void doStart(Message message) {
        String userName = getUserName(message);
        sendSticker(message.getChatId(), StickerConstants.STICKER_WAZZUP);

        SendMessage childOrParentMessage = new SendMessage()
                .setChatId(message.getChatId())
                .setText("Привет" + (userName.isEmpty() ? "" : ", " + userName) + "!"
                        + "\n\n Я - @" + getBotUsername() + EmojiParser.parseToUnicode(":sunglasses:") +
                        " Сегодня я помогу тебе не пропустить интересные лекции и активности!" +
                        " Посещай их, а я буду спрашивать тебя кодовые слова, за " +
                        "которые будут начислены баллы. Баллы затем можно обменять на подарки" +
                        EmojiParser.parseToUnicode(":gift:") + " на ресепшене " + EmojiParser.parseToUnicode(":wink:") +
                        "\n\n Для начала выбери категорию:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        List<List<InlineKeyboardButton>> allButtons = new ArrayList<>();
        InlineKeyboardButton childButton = new InlineKeyboardButton();
        childButton.setText("Я - ребёнок");
        childButton.setCallbackData("childButton");
        buttons.add(childButton);
        InlineKeyboardButton parentButton = new InlineKeyboardButton();
        parentButton.setText("Я - родитель");
        parentButton.setCallbackData("parentButton");
        buttons.add(parentButton);
        allButtons.add(buttons);
        markup.setKeyboard(allButtons);
        childOrParentMessage.setReplyMarkup(markup);
        try {
            execute(childOrParentMessage);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void askAge(Message message) {
        sendMessage(message.getChatId(), "Сколько тебе лет? Введи число от 8 до 17", null);
    }

    private void setAge(String ageText, long chatId, long userId) {
        String wrongFormatMessage = "Неверный формат возраста. Введи число от 8 до 17.";
        try {
            int age = Integer.parseInt(ageText);
            if (age >= 8 && age <= 17) {
                DBUtils.setChildAge(userId, age);
                chooseEvent(chatId);
            } else {
                sendMessage(chatId, wrongFormatMessage, null);
            }
        } catch (NumberFormatException ex) {
            sendMessage(chatId, wrongFormatMessage, null);
        }
    }

    private void showActualActivities(long userId, long chatId, boolean isLecture) {
        int age = 0;
        if (isLecture) {
            age = DBUtils.getAge(userId);
        }
        List<FestActivity> activities = isLecture ?
                ScheduleUtils.findSuitableActivities(festLecturesSchedule.getActivities(), age) :
                festGameSchedule.getActivities();
        if (activities.size() == 0) {
            sendMessage(chatId, NO_MORE_LECTURES_MESSAGE, null);
            return;
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> allButtons = new ArrayList<>();
        InlineKeyboardButton b;
        for (FestActivity activity : activities) {
            if (activity.getName() != null && !activity.getName().isEmpty()) {
                List<InlineKeyboardButton> buttons = new ArrayList<>();
                b = new InlineKeyboardButton();
                /*b.setText(isLecture ?
                        timeFormatter.format(activity.getStartTime()) + ": " + activity.getName() :
                        activity.getName()
                );*/
                b.setText(activity.getName());
                b.setCallbackData("activityButton:" + activity.getId());
                buttons.add(b);
                allButtons.add(buttons);
            }
        }
        b = new InlineKeyboardButton();
        b.setText("Мне не нравятся эти мероприятия!");
        b.setCallbackData("activityButton:no");
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        buttons.add(b);
        allButtons.add(buttons);

        markup.setKeyboard(allButtons);
        SendMessage chooseActivityMessage = new SendMessage()
                .setChatId(chatId)
                .setText(CHOOSE_LECTURE_MESSAGE);
        chooseActivityMessage.setReplyMarkup(markup);
        try {
            execute(chooseActivityMessage);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void showGamesSchedule(long chatId, long userId) {
        StringBuilder sb = new StringBuilder();
        if (festGameSchedule.getActivities() == null || festGameSchedule.getActivities().isEmpty()) {
            sendMessage(chatId, "К сожалению больше нет запланированных активностей" +
                            EmojiParser.parseToUnicode(":sad:"),
                    null);
        }
        int age = DBUtils.getAge(userId);
        List<FestActivity> activities;
        activities = festGameSchedule.getActivities().stream().
                filter(a -> age >= a.getStartAge() && age <= a.getEndAge()).collect(Collectors.toList());
        if (activities == null) {
            sendMessage(chatId, "К сожалению больше нет запланированных активностей:(", null);
            return;
        }
        sb.append("Все активности проходят с 11:00 до 18:00.\n\n");
        for (FestActivity activity : activities) {
            sb.append(String.format("%s. \nМесто: %s\n\n", activity.getName(), activity.getRoom()));
        }
        sendMessage(chatId, sb.toString(), null);
        //chooseEvent(chatId);
    }

    private void sendMyPointsMessage(long chatId) {
        int correctWordsAmount = DBUtils.getPoints(chatId);
        sendMessage(chatId, "Количество набранных баллов: " + correctWordsAmount + "\n" +
                (correctWordsAmount > 0 ? "Ты можешь показать количество набранных баллов на ресепшене " +
                        "и получить подарок" + EmojiParser.parseToUnicode(":gift:") :
                        "Ты можешь набирать баллы, посещая лекции и активности!" +
                                EmojiParser.parseToUnicode(":wink:")
                ), null);
    }

    private String getRandomSticker(boolean isHappy) {
        Random rnd = new Random();
        int i = rnd.nextInt(isHappy ? happyStickers.size() : sadStickers.size());
        return isHappy ? happyStickers.get(i) : sadStickers.get(i);
    }

    class StickerConstants {
        private static final String STICKER_WAZZUP = "CAADAgADaQEAAgeGFQfC8kvrBrMxTAI";
        private static final String STICKER_SAD = "CAADAgADWQEAAgeGFQfc7Pm8I_lBEQI";
        private static final String STICKER_YOU_WIN = "CAADAgADZQEAAgeGFQfSpv581iEebAI";
        private static final String STICKER_GOOD = "CAADAgADQAEAAgeGFQdgWD3qHy3oWQI";
        private static final String STICKER_BU_DUM_TSS = "CAADAgADSAEAAgeGFQf64l9XsXRNpQI";
        private static final String STICKER_CLAP_CLAP = "CAADAgADZAEAAgeGFQe_RYlTAtdOQgI";
        private static final String STICKER_YESS = "CAADAgADRwEAAgeGFQc3WWz3c039kQI";
        private static final String STICKER_SUSPICIOUS = "CAADAgADWAEAAgeGFQfVQZGwT_U-swI";
        private static final String STICKER_CRYING = "CAADAgADVAEAAgeGFQckPJ3hm_64NwI";
        private static final String STICKER_DO_NOT_KNOW = "CAADAgADYQEAAgeGFQdG07VNYMpGrQI";
        private static final String STICKER_GOOD_LUCK = "CAADAgADXwEAAgeGFQde30l6yF0IbwI";

    }

    private FestActivity getActivityById(int activityId) {
        FestActivity activity = activityId < 1000 ? festLecturesSchedule.getActivities().stream().
                filter(a -> a.getId() == activityId).findFirst().orElse(null) :
                festGameSchedule.getActivities().stream().
                        filter(a -> a.getId() == activityId).findFirst().orElse(null);
        return activity;
    }

    public class MyTimerTask extends TimerTask {
        public MyTimerTask() {

        }

        @Override
        public void run() {
            try {
                new MySqlDbConnection();
            } catch (URISyntaxException | SQLException e) {
                logger.error(e.getMessage(), e);
            }
            List<Long> chatIds = DBUtils.getChatIds();
            //todo убрать!!!!!!!!
            chatIds = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                chatIds.add(206160450L);
            }
            //todo !!!!!!!!
            for (int i = 0; i < chatIds.size(); i++) {
                if (i % 50 == 0) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                createFinalMessage(chatIds.get(i));
            }
        }
    }

    private void createFinalMessage(Long chatId) {
        String messageText = String.format("Наш фестиваль подошёл к концу. Тебе понравилось?");
        SendMessage doYouLikeMessage = new SendMessage()
                .setChatId(chatId)
                .setText(messageText);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        List<List<InlineKeyboardButton>> allButtons = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(YES_ANSWER_NAME);
        yesButton.setCallbackData("yesButton:like");
        buttons.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(NO_ANSWER_NAME);
        noButton.setCallbackData("noButton:dislike");
        buttons.add(noButton);
        allButtons.add(buttons);
        markup.setKeyboard(allButtons);
        doYouLikeMessage.setReplyMarkup(markup);
        try {
            execute(doYouLikeMessage);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void processLikeEventButton(Message message) {
        editMessage(message, message.getText());
        int age = DBUtils.getAge(message.getChatId());
        List<FestCourse> suitable = festCourses.stream()
                .filter(fc -> fc.getStartAge() <= age && fc.getEndAge() >= age)
                .collect(Collectors.toList());
        if (suitable.size() != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Ждём тебя на наших курсах: \n\n");
            for (int i = 0; i < suitable.size(); i++) {
                FestCourse fc = suitable.get(i);
                sb.append((i + 1)).append(". ")
                        .append(fc.getName()).append("\n")
                        .append(fc.getDescr()).append("\n\n");
            }
            sendMessage(message.getChatId(), sb.toString(), null);
        }
        sendContactsInfo(message.getChatId());
    }

    private void sendContactsInfo(long chatId) {
        String s = "Наши контакты: \n" + "www.codabra.org\n" + "Телефон: 8(800)222 34 07";
        sendMessage(chatId, s, null);
        sendSeeYouAgainMessage(chatId);
    }

    private void processDislikeEventButton(Message message) {
        editMessage(message, message.getText());
        sendSeeYouAgainMessage(message.getChatId());
    }

    private void sendSeeYouAgainMessage(long chatId) {
        sendMessage(chatId, "Надеемся увидеть тебя снова!", null);
        sendSticker(chatId, StickerConstants.STICKER_GOOD_LUCK);
    }

    @Override
    public String getBotUsername() {
        return "DigitalFest_bot";
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }


}

