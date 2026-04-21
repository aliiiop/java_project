package com.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RatingBot extends TelegramLongPollingBot {
    private static final String UNDO_BUTTON = "↩️ Отменить шаг";

    private final String BOT_TOKEN = "8762441185:AAHBI8LCr47AD6XJRx-bakOSLHfzGgTVpuk";
    private final String BOT_USERNAME = "Kara_Tabletka_Bot";
    private final Map<Long, UserSession> users = new HashMap<>();

    public RatingBot() {
        FaceEditService.ensureMediaDirectories();
        users.putAll(SessionStorage.loadAll());
        System.out.println("📜 Загружены сессии для " + users.size() + " пользователей");
    }

    private void persistSessions() {
        Map<Long, UserSession> data = new HashMap<>();
        for (Map.Entry<Long, UserSession> entry : users.entrySet()) {
            if (entry.getValue().hasPersistentData()) {
                data.put(entry.getKey(), entry.getValue());
            }
        }
        SessionStorage.saveAll(data);
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (!update.hasMessage()) {
                return;
            }

            Message msg = update.getMessage();
            Long chatId = msg.getChatId();

            UserSession session = users.getOrDefault(chatId, new UserSession());
            users.put(chatId, session);

            if (msg.hasText()) {
                String text = msg.getText();

                if (text.equals("/start")) {
                    start(chatId, session);
                    return;
                } else if (text.equals("/premium")) {
                    premium(chatId, session);
                    return;
                } else if (text.equals("/compare")) {
                    compare(chatId, session);
                    return;
                } else if (text.equals("🔄 Новая оценка")) {
                    start(chatId, session);
                    return;
                } else if (text.equals("⭐ Сравнить со звездой")) {
                    compare(chatId, session);
                    return;
                } else if (text.equals("💎 Премиум")) {
                    premium(chatId, session);
                    return;
                } else if (text.equals("❓ Помощь")) {
                    help(chatId, session);
                    return;
                } else if (text.equals("📜 История") || text.equals("/history")) {
                    showHistory(chatId, session);
                    return;
                } else if (text.equals("/undo") || text.equals(UNDO_BUTTON)) {
                    undo(chatId, session);
                    return;
                }

                if (session.getState() == UserState.WAITING_FOR_HEIGHT
                    || session.getState() == UserState.WAITING_FOR_WEIGHT
                    || session.getState() == UserState.WAITING_FOR_CHEST
                    || session.getState() == UserState.WAITING_FOR_WAIST
                    || session.getState() == UserState.WAITING_FOR_HIPS) {
                    handleMeasurementStep(chatId, text, session);
                    return;
                }

                if (session.getState() == UserState.SELECTING_GENDER) {
                    handleGenderSelection(chatId, text, session);
                    return;
                }

                if (session.getState() == UserState.SELECTING_RATING_TYPE) {
                    handleRatingTypeSelection(chatId, text, session);
                    return;
                }

                sendText(chatId, "❌ Используйте кнопки меню");
                mainMenu(chatId);
                return;
            }

            if (msg.hasPhoto() && session.getState() == UserState.WAITING_FOR_PHOTO) {
                handlePhoto(chatId, msg, session);
            } else {
                sendText(chatId, "❌ Отправьте фото или используйте кнопки");
            }
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void start(Long chatId, UserSession s) {
        s.clearUndoActions();
        s.getBodyMeasurements().clear();
        s.setRatingType(null);
        s.setState(UserState.SELECTING_GENDER);
        showGenderMenu(chatId);
    }

    private void premium(Long chatId, UserSession s) {
        if (!s.isPremium()) {
            s.activatePremiumDays(30);
            persistSessions();
            sendText(
                chatId,
                "✅ Премиум активирован на 30 дней!\n\n"
                    + "Теперь вам доступны:\n"
                    + "• подробный разбор по каждой оценке\n"
                    + "• рекомендации, как поднять балл\n"
                    + "• сравнение с прошлым результатом\n"
                    + "• расширенная история и статистика"
            );
        } else {
            String until = s.getPremiumUntilLabel();
            String suffix = until == null ? "" : "\nАктивен до: " + until;
            sendText(
                chatId,
                "💎 Премиум уже активен." + suffix
                    + "\n\nВам доступны подробный разбор, советы по улучшению и полная история."
            );
        }
        mainMenu(chatId);
    }

    private void compare(Long chatId, UserSession s) {
        if (s.getLastRating() == 0) {
            sendText(chatId, "❌ Сначала получите оценку внешности!\nИспользуйте /start");
        } else {
            sendText(chatId, RatingUtils.compareToCelebrity(s.getLastRating(), s.getGender()));
        }
        mainMenu(chatId);
    }

    private void help(Long chatId, UserSession s) {
        String premiumInfo = s.isPremium()
            ? "\n\n💎 У вас активен premium: подробный разбор, советы и полная история."
            : "\n\n💎 Premium открывает детальный разбор, советы по улучшению и статистику по истории.";

        Map<String, String> commands = new HashMap<>();
        commands.put("/start", "начать новую оценку");
        commands.put("/undo", "отменить последний шаг");
        commands.put("/history", "посмотреть прошлые оценки");
        commands.put("/compare", "сравнить последний результат со звездой");
        commands.put("/premium", "открыть premium-возможности");

        ArrayList<String> commandOrder = new ArrayList<>();
        commandOrder.add("/start");
        commandOrder.add("/undo");
        commandOrder.add("/history");
        commandOrder.add("/compare");
        commandOrder.add("/premium");

        ArrayList<String> helpSections = new ArrayList<>();
        helpSections.add("🆘 *Помощь*");

        StringBuilder commandsText = new StringBuilder("*Команды:*\n");
        for (String command : commandOrder) {
            commandsText.append(command)
                .append(" - ")
                .append(commands.get(command))
                .append("\n");
        }
        helpSections.add(commandsText.toString().trim());

        helpSections.add(
            "*Кнопки в меню:*\n"
                + "🔄 Новая оценка - начать новую проверку с нуля\n"
                + "⭐ Сравнить со звездой - доступно после получения последней оценки\n"
                + "📜 История - открыть список ваших прошлых результатов\n"
                + "❓ Помощь - заново показать эту справку\n"
                + UNDO_BUTTON + " - откатить предыдущий шаг"
        );

        helpSections.add(
            "*Как проходит оценка:*\n"
                + "1. Выберите пол\n"
                + "2. Выберите тип оценки: лицо по фото, тело по фото или тело по замерам\n"
                + "3. Отправьте фото либо по очереди введите все нужные замеры\n"
                + "4. Получите итоговый балл и при желании откройте историю или сравнение"
        );

        helpSections.add(
            "*Подсказки:*\n"
                + "• Для фото лучше отправлять чёткое и свежее изображение\n"
                + "• Для лица подходит фото анфас при хорошем освещении\n"
                + "• Для тела лучше использовать фото в полный рост\n"
                + "• Замеры вводите только числами, без букв и лишних символов"
        );

        StringBuilder helpText = new StringBuilder();
        for (int i = 0; i < helpSections.size(); i++) {
            if (i > 0) {
                helpText.append("\n\n");
            }
            helpText.append(helpSections.get(i));
        }
        helpText.append(premiumInfo);

        sendText(chatId, helpText.toString());
        mainMenu(chatId);
    }

    private void handleGenderSelection(Long chatId, String text, UserSession s) {
        String previousGender = s.getGender();
        String previousRatingType = s.getRatingType();

        if (text.equals("👨 Мужчина")) {
            s.setGender("male");
        } else if (text.equals("👩 Женщина")) {
            s.setGender("female");
        } else {
            sendText(chatId, "❌ Используйте кнопки");
            return;
        }

        s.pushUndoAction(UserState.SELECTING_GENDER, previousGender, previousRatingType, null);
        s.setState(UserState.SELECTING_RATING_TYPE);
        showRatingTypeMenu(chatId);
    }

    private void handleRatingTypeSelection(Long chatId, String text, UserSession s) {
        s.pushUndoAction(UserState.SELECTING_RATING_TYPE, s.getGender(), s.getRatingType(), null);

        if (text.equals("📸 Оценка лица по фото")) {
            s.setRatingType("face");
            s.setState(UserState.WAITING_FOR_PHOTO);
            sendPhotoPrompt(chatId, s);
        } else if (text.equals("🏃 Оценка тела по фото")) {
            s.setRatingType("body");
            s.setState(UserState.WAITING_FOR_PHOTO);
            sendPhotoPrompt(chatId, s);
        } else if (text.equals("📏 Оценка тела по замерам")) {
            s.setRatingType("measure");
            s.getBodyMeasurements().clear();
            s.setState(UserState.WAITING_FOR_HEIGHT);
            sendMeasurementPrompt(chatId, s);
        } else {
            s.undoLastAction();
            sendText(chatId, "❌ Используйте кнопки");
        }
    }

    private void handleMeasurementStep(Long chatId, String text, UserSession s) {
        try {
            double value = Double.parseDouble(text.trim());

            if (value <= 0 || value > 300) {
                sendText(chatId, "❌ Некорректное значение! Укажите число от 1 до 300");
                return;
            }

            if (s.getState() == UserState.WAITING_FOR_HEIGHT) {
                s.pushUndoAction(UserState.WAITING_FOR_HEIGHT, s.getGender(), s.getRatingType(), "height");
                s.getBodyMeasurements().put("height", value);
                s.setState(UserState.WAITING_FOR_WEIGHT);
                sendMeasurementPrompt(chatId, s, "✅ Рост: " + String.format("%.0f", value) + " см");
            } else if (s.getState() == UserState.WAITING_FOR_WEIGHT) {
                s.pushUndoAction(UserState.WAITING_FOR_WEIGHT, s.getGender(), s.getRatingType(), "weight");
                s.getBodyMeasurements().put("weight", value);
                s.setState(UserState.WAITING_FOR_CHEST);
                sendMeasurementPrompt(chatId, s, "✅ Вес: " + String.format("%.1f", value) + " кг");
            } else if (s.getState() == UserState.WAITING_FOR_CHEST) {
                s.pushUndoAction(UserState.WAITING_FOR_CHEST, s.getGender(), s.getRatingType(), "chest");
                s.getBodyMeasurements().put("chest", value);
                s.setState(UserState.WAITING_FOR_WAIST);
                sendMeasurementPrompt(chatId, s, "✅ Грудь: " + String.format("%.0f", value) + " см");
            } else if (s.getState() == UserState.WAITING_FOR_WAIST) {
                s.pushUndoAction(UserState.WAITING_FOR_WAIST, s.getGender(), s.getRatingType(), "waist");
                s.getBodyMeasurements().put("waist", value);
                s.setState(UserState.WAITING_FOR_HIPS);
                sendMeasurementPrompt(chatId, s, "✅ Талия: " + String.format("%.0f", value) + " см");
            } else if (s.getState() == UserState.WAITING_FOR_HIPS) {
                if (s.getGender().equals("male")) {
                    s.pushUndoAction(UserState.WAITING_FOR_HIPS, s.getGender(), s.getRatingType(), "shoulder");
                    s.getBodyMeasurements().put("shoulder", value);
                    sendText(chatId, "✅ Плечи: " + String.format("%.0f", value) + " см\n\n🎉 *Данные собраны! Идет анализ...*");
                } else {
                    s.pushUndoAction(UserState.WAITING_FOR_HIPS, s.getGender(), s.getRatingType(), "hips");
                    s.getBodyMeasurements().put("hips", value);
                    sendText(chatId, "✅ Бёдра: " + String.format("%.0f", value) + " см\n\n🎉 *Данные собраны! Идет анализ...*");
                }
                finalizeMeasurements(chatId, s);
            }
        } catch (NumberFormatException e) {
            sendText(chatId, "❌ Ошибка! Введите корректное число\n(без букв и спецсимволов)");
        }
    }

    private void finalizeMeasurements(Long chatId, UserSession s) {
        try {
            double rating = RatingUtils.evaluateBodyByMeasurements(s.getBodyMeasurements(), s.getGender());
            Double previousRating = s.getPreviousRating("measure");
            s.setLastRating(rating);
            s.addHistoryEntry("measure", s.getGender(), rating);
            persistSessions();

            String result = RatingUtils.generateMeasurementsMessage(
                s.getBodyMeasurements(), rating, s.isPremium(), s.getGender(), previousRating
            );
            s.clearUndoActions();
            s.setState(UserState.SELECTING_GENDER);
            sendText(chatId, result);
            mainMenu(chatId);
        } catch (Exception e) {
            sendText(chatId, "❌ Ошибка при обработке данных. Попробуйте еще раз.");
            e.printStackTrace();
        }
    }

    private void handlePhoto(Long chatId, Message msg, UserSession s) {
        try {
            String photoId = msg.getPhoto().get(msg.getPhoto().size() - 1).getFileId();
            String ratingType = s.getRatingType();
            String gender = s.getGender();
            sendText(chatId, "📸 Фото получено! Идет анализ...");

            double rating;
            if ("face".equals(ratingType)) {
                rating = RatingUtils.evaluateFace(photoId, gender);
            } else {
                rating = RatingUtils.evaluateBodyByPhoto(photoId, gender);
            }

            Double previousRating = s.getPreviousRating(ratingType);
            s.setLastRating(rating);
            s.addHistoryEntry(ratingType, gender, rating);
            persistSessions();

            String result = RatingUtils.generateRatingMessage(
                rating, s.isPremium(), gender, previousRating, ratingType
            );
            s.clearUndoActions();
            s.setState(UserState.SELECTING_GENDER);
            sendText(chatId, result);
            maybeSendFaceEdit(chatId, photoId, gender, ratingType, rating);
            mainMenu(chatId);
        } catch (Exception e) {
            sendText(chatId, "❌ Ошибка при обработке фото. Попробуйте еще раз.");
            e.printStackTrace();
        }
    }

    private void showHistory(Long chatId, UserSession s) {
        List<UserSession.RatingHistoryEntry> history = s.getHistory();
        if (history.isEmpty()) {
            sendText(chatId, "📜 *История пуста*\n\nУ вас пока нет ни одной оценки.\nНажмите «🔄 Новая оценка», чтобы начать!");
            mainMenu(chatId);
            return;
        }

        StringBuilder sb = new StringBuilder("📜 *История ваших оценок*\n");
        sb.append("Всего: ").append(history.size()).append("\n\n");

        if (s.isPremium()) {
            double best = 0.0;
            double sum = 0.0;
            Map<String, Integer> byType = countHistoryByType(history);
            for (UserSession.RatingHistoryEntry entry : history) {
                sum += entry.getRating();
                best = Math.max(best, entry.getRating());
            }
            double average = sum / history.size();
            String typeSummary = buildTypeSummary(byType);
            sb.append("💎 *Премиум-статистика*\n");
            sb.append("Средний балл: ").append(String.format("%.2f", average)).append("\n");
            sb.append("Лучший балл: ").append(String.format("%.1f", best)).append("\n");
            sb.append("По типам: ").append(typeSummary).append("\n\n");

            int idx = 1;
            for (UserSession.RatingHistoryEntry entry : history) {
                sb.append(entry.format(idx++)).append("\n\n");
            }

            sendText(chatId, sb.toString());
            mainMenu(chatId);
            return;
        }

        List<UserSession.RatingHistoryEntry> recentHistory = getRecentHistory(history, 10);
        int idx = history.size() - recentHistory.size() + 1;
        for (UserSession.RatingHistoryEntry entry : recentHistory) {
            sb.append(entry.format(idx++)).append("\n\n");
        }

        if (history.size() > recentHistory.size()) {
            sb.append("_(показаны последние 10 из ").append(history.size()).append(")_");
        }

        sendText(chatId, sb.toString());
        mainMenu(chatId);
    }

    private Map<String, Integer> countHistoryByType(List<UserSession.RatingHistoryEntry> history) {
        HashMap<String, Integer> byType = new HashMap<>();
        for (UserSession.RatingHistoryEntry entry : history) {
            byType.merge(entry.getRatingType(), 1, Integer::sum);
        }
        return byType;
    }

    private List<UserSession.RatingHistoryEntry> getRecentHistory(
        List<UserSession.RatingHistoryEntry> history,
        int limit
    ) {
        ArrayList<UserSession.RatingHistoryEntry> recentHistory = new ArrayList<>();
        if (limit <= 0) {
            return recentHistory;
        }

        LinkedList<UserSession.RatingHistoryEntry> recentQueue = new LinkedList<>();
        for (UserSession.RatingHistoryEntry entry : history) {
            recentQueue.addLast(entry);
            if (recentQueue.size() > limit) {
                recentQueue.removeFirst();
            }
        }

        recentHistory.addAll(recentQueue);
        return recentHistory;
    }

    private String buildTypeSummary(Map<String, Integer> byType) {
        ArrayList<String> summary = new ArrayList<>();
        addTypeSummary(summary, byType, "face");
        addTypeSummary(summary, byType, "body");
        addTypeSummary(summary, byType, "measure");

        StringBuilder summaryText = new StringBuilder();
        for (int i = 0; i < summary.size(); i++) {
            if (i > 0) {
                summaryText.append(", ");
            }
            summaryText.append(summary.get(i));
        }
        return summaryText.toString();
    }

    private void addTypeSummary(List<String> summary, Map<String, Integer> byType, String type) {
        Integer count = byType.get(type);
        if (count != null) {
            summary.add(RatingUtils.getRatingTypeLabel(type) + " - " + count);
        }
    }

    private void mainMenu(Long chatId) {
        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText("🏠 *Главное меню*\n\nЧто хотите сделать?");
        m.setParseMode("Markdown");

        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        r1.add("🔄 Новая оценка");
        r1.add("⭐ Сравнить со звездой");
        rows.add(r1);

        KeyboardRow r2 = new KeyboardRow();
        r2.add("💎 Премиум");
        r2.add("📜 История");
        rows.add(r2);

        KeyboardRow r3 = new KeyboardRow();
        r3.add("❓ Помощь");
        r3.add(UNDO_BUTTON);
        rows.add(r3);

        kb.setKeyboard(rows);
        m.setReplyMarkup(kb);

        try {
            execute(m);
        } catch (TelegramApiException ignored) {
        }
    }

    private void undo(Long chatId, UserSession s) {
        if (!s.undoLastAction()) {
            sendText(chatId, "↩️ Нечего отменять.");
            return;
        }
        sendStatePrompt(chatId, s, "↩️ Последний шаг отменён.");
    }

    private void sendStatePrompt(Long chatId, UserSession s, String prefix) {
        StringBuilder text = new StringBuilder();
        if (prefix != null && !prefix.isBlank()) {
            text.append(prefix).append("\n\n");
        }

        if (s.getState() == UserState.SELECTING_GENDER) {
            showGenderMenu(chatId, text.append("Выберите ваш пол:").toString());
            return;
        }
        if (s.getState() == UserState.SELECTING_RATING_TYPE) {
            showRatingTypeMenu(chatId, text.append("📋 Выберите тип оценки:").toString());
            return;
        }
        if (s.getState() == UserState.WAITING_FOR_PHOTO) {
            sendPhotoPrompt(chatId, s, text.toString());
            return;
        }
        if (s.getState() == UserState.WAITING_FOR_HEIGHT
            || s.getState() == UserState.WAITING_FOR_WEIGHT
            || s.getState() == UserState.WAITING_FOR_CHEST
            || s.getState() == UserState.WAITING_FOR_WAIST
            || s.getState() == UserState.WAITING_FOR_HIPS) {
            sendMeasurementPrompt(chatId, s, text.toString().trim());
            return;
        }

        mainMenu(chatId);
    }

    private void showGenderMenu(Long chatId) {
        showGenderMenu(chatId, "👋 Добро пожаловать в бот оценки внешности!\n\nВыберите ваш пол:");
    }

    private void showGenderMenu(Long chatId, String text) {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("👨 Мужчина");
        row.add("👩 Женщина");
        rows.add(row);

        KeyboardRow undoRow = new KeyboardRow();
        undoRow.add(UNDO_BUTTON);
        rows.add(undoRow);

        kb.setKeyboard(rows);
        sendText(chatId, text, kb);
    }

    private void showRatingTypeMenu(Long chatId) {
        showRatingTypeMenu(chatId, "📋 Выберите тип оценки:");
    }

    private void showRatingTypeMenu(Long chatId, String text) {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow r1 = new KeyboardRow();
        r1.add("📸 Оценка лица по фото");
        r1.add("🏃 Оценка тела по фото");
        rows.add(r1);

        KeyboardRow r2 = new KeyboardRow();
        r2.add("📏 Оценка тела по замерам");
        rows.add(r2);

        KeyboardRow undoRow = new KeyboardRow();
        undoRow.add(UNDO_BUTTON);
        rows.add(undoRow);

        kb.setKeyboard(rows);
        sendText(chatId, text, kb);
    }

    private void sendPhotoPrompt(Long chatId, UserSession s) {
        sendPhotoPrompt(chatId, s, "");
    }

    private void sendPhotoPrompt(Long chatId, UserSession s, String prefix) {
        String baseText = "face".equals(s.getRatingType())
            ? "📸 Отправьте фото лица\n\nТребования: четкое фото, анфас, хорошее освещение"
            : "📸 Отправьте фото тела\n\nТребования: четкое фото, в полный рост";
        sendText(chatId, joinPrefix(prefix, baseText), buildUndoKeyboard());
    }

    private void sendMeasurementPrompt(Long chatId, UserSession s) {
        sendMeasurementPrompt(chatId, s, null);
    }

    private void sendMeasurementPrompt(Long chatId, UserSession s, String prefix) {
        String prompt;
        if (s.getState() == UserState.WAITING_FOR_HEIGHT) {
            prompt = "📏 *ПОШАГОВЫЙ ВВОД ЗАМЕРОВ*\n\n📍 Шаг 1 из 5\n\n*Введите ваш рост (см):*\n(например: 175)";
        } else if (s.getState() == UserState.WAITING_FOR_WEIGHT) {
            prompt = "📍 Шаг 2 из 5\n\n*Введите ваш вес (кг):*\n(например: 75)";
        } else if (s.getState() == UserState.WAITING_FOR_CHEST) {
            prompt = "📍 Шаг 3 из 5\n\n*Введите объём груди (см):*\n(например: 100)";
        } else if (s.getState() == UserState.WAITING_FOR_WAIST) {
            prompt = "📍 Шаг 4 из 5\n\n*Введите объём талии (см):*\n(например: 80)";
        } else if ("male".equals(s.getGender())) {
            prompt = "📍 Шаг 5 из 5\n\n*Введите ширину плеч (см):*\n(например: 110)";
        } else {
            prompt = "📍 Шаг 5 из 5\n\n*Введите объём бёдер (см):*\n(например: 90)";
        }
        sendText(chatId, joinPrefix(prefix, prompt), buildUndoKeyboard());
    }

    private ReplyKeyboardMarkup buildUndoKeyboard() {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(UNDO_BUTTON);
        rows.add(row);

        kb.setKeyboard(rows);
        return kb;
    }

    private String joinPrefix(String prefix, String body) {
        if (prefix == null || prefix.isBlank()) {
            return body;
        }
        return prefix + "\n\n" + body;
    }

    private void sendText(Long chatId, String text) {
        sendText(chatId, text, null);
    }

    private void sendText(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText(text);
        m.setParseMode("Markdown");
        if (keyboard != null) {
            m.setReplyMarkup(keyboard);
        }
        try {
            execute(m);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }

    private void maybeSendFaceEdit(
        Long chatId,
        String photoId,
        String gender,
        String ratingType,
        double rating
    ) {
        if (!FaceEditService.shouldCreateFaceEdit(ratingType, rating)) {
            return;
        }

        FaceEditService.RenderResult renderResult = buildFaceEdit(chatId, photoId, gender, rating);
        if (renderResult.isReady()) {
            sendRenderedEdit(chatId, renderResult.getOutput());
            return;
        }

        if (renderResult.getMessage() != null) {
            System.out.println("Face edit skipped for chat " + chatId + ": " + renderResult.getMessage());
        }
    }

    private FaceEditService.RenderResult buildFaceEdit(
        Long chatId,
        String photoId,
        String gender,
        double rating
    ) {
        try {
            GetFile getFileMethod = new GetFile();
            getFileMethod.setFileId(photoId);

            org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFileMethod);
            Path facePhoto = FaceEditService.buildPhotoPath(chatId, tgFile.getFilePath());
            downloadFile(tgFile.getFilePath(), facePhoto.toFile());
            return FaceEditService.renderFaceEdit(chatId, gender, rating, facePhoto);
        } catch (Exception e) {
            return FaceEditService.RenderResult.failed(
                "Не удалось подготовить face edit: " + e.getMessage()
            );
        }
    }

    private void sendRenderedEdit(Long chatId, Path videoPath) {
        SendVideo video = new SendVideo();
        video.setChatId(chatId.toString());
        video.setVideo(new InputFile(videoPath.toFile()));
        video.setCaption("🎬 Эдит готов");
        video.setSupportsStreaming(true);
        try {
            execute(video);
        } catch (TelegramApiException e) {
            System.err.println("Не удалось отправить эдит: " + e.getMessage());
        }
    }
}
