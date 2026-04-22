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
    // Используй /myid чтобы узнать свой chat_id, затем вставь сюда
    private static final long ADMIN_CHAT_ID = 0L;
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
                } else if (text.equals("/myid")) {
                    sendText(chatId, "🆔 Ваш chat ID: `" + chatId + "`");
                    return;
                } else if (text.startsWith("/givepremium")) {
                    handleGivePremium(chatId, text, session);
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

                if (session.getState() == UserState.WAITING_FOR_RECEIPT) {
                    sendText(chatId, "📎 Отправьте скриншот квитанции фото или файлом.\nЧтобы отменить — /start");
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

            if (msg.hasDocument()) {
                if (session.getState() == UserState.WAITING_FOR_RECEIPT) {
                    handleReceiptFile(chatId, msg.getDocument().getFileId(), session);
                } else {
                    sendText(chatId, "❌ Используйте кнопки меню");
                }
                return;
            }

            if (msg.hasPhoto()) {
                if (session.getState() == UserState.WAITING_FOR_PHOTO) {
                    handlePhoto(chatId, msg, session);
                } else if (session.getState() == UserState.WAITING_FOR_RECEIPT) {
                    String photoId = msg.getPhoto().get(msg.getPhoto().size() - 1).getFileId();
                    handleReceiptFile(chatId, photoId, session);
                } else {
                    sendText(chatId, "❌ Отправьте фото или используйте кнопки");
                }
                return;
            }
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void start(Long chatId, UserSession session) {
        session.clearUndoActions();
        session.getBodyMeasurements().clear();
        session.setRatingType(null);
        session.setState(UserState.SELECTING_GENDER);
        showGenderMenu(chatId);
    }

    private void premium(Long chatId, UserSession session) {
        if (session.isPremium()) {
            String until = session.getPremiumUntilLabel();
            String suffix = until == null ? "" : "\nАктивен до: " + until;
            sendText(chatId, "💎 Премиум уже активен." + suffix
                + "\n\nВам доступны подробный разбор, советы по улучшению и полная история.");
            mainMenu(chatId);
            return;
        }

        session.setState(UserState.WAITING_FOR_RECEIPT);
        sendText(chatId,
            "💎 *Премиум подписка — 990 ₸ / 30 дней*\n\n"
            + "Вам откроются:\n"
            + "• подробный разбор каждой оценки\n"
            + "• рекомендации по улучшению\n"
            + "• сравнение с прошлым результатом\n"
            + "• расширенная история и статистика\n\n"
            + "💳 *Реквизиты для оплаты:*\n"
            + "Карта: `" + ReceiptAnalyzer.EXPECTED_CARD + "`\n"
            + "Получатель: `" + ReceiptAnalyzer.EXPECTED_NAME + "`\n"
            + "Сумма: `990 ₸`\n\n"
            + "После оплаты отправьте скриншот квитанции — *фото или файлом*.\n"
            + "Чтобы отменить — /start"
        );
    }

    private void compare(Long chatId, UserSession session) {
        if (session.getLastRating() == 0) {
            sendText(chatId, "❌ Сначала получите оценку внешности!\nИспользуйте /start");
        } else {
            sendText(chatId, RatingUtils.compareToCelebrity(session.getLastRating(), session.getGender()));
        }
        mainMenu(chatId);
    }

    private void help(Long chatId, UserSession session) {
        String premiumInfo = session.isPremium()
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

    private void handleGenderSelection(Long chatId, String text, UserSession session) {
        String previousGender = session.getGender();
        String previousRatingType = session.getRatingType();

        if (text.equals("👨 Мужчина")) {
            session.setGender("male");
        } else if (text.equals("👩 Женщина")) {
            session.setGender("female");
        } else {
            sendText(chatId, "❌ Используйте кнопки");
            return;
        }

        session.pushUndoAction(UserState.SELECTING_GENDER, previousGender, previousRatingType, null);
        session.setState(UserState.SELECTING_RATING_TYPE);
        showRatingTypeMenu(chatId);
    }

    private void handleRatingTypeSelection(Long chatId, String text, UserSession session) {
        session.pushUndoAction(
            UserState.SELECTING_RATING_TYPE,
            session.getGender(),
            session.getRatingType(),
            null
        );

        if (text.equals("📸 Оценка лица по фото")) {
            session.setRatingType("face");
            session.setState(UserState.WAITING_FOR_PHOTO);
            sendPhotoPrompt(chatId, session);
        } else if (text.equals("🏃 Оценка тела по фото")) {
            session.setRatingType("body");
            session.setState(UserState.WAITING_FOR_PHOTO);
            sendPhotoPrompt(chatId, session);
        } else if (text.equals("📏 Оценка тела по замерам")) {
            session.setRatingType("measure");
            session.getBodyMeasurements().clear();
            session.setState(UserState.WAITING_FOR_HEIGHT);
            sendMeasurementPrompt(chatId, session);
        } else {
            session.undoLastAction();
            sendText(chatId, "❌ Используйте кнопки");
        }
    }

    private void handleMeasurementStep(Long chatId, String text, UserSession session) {
        try {
            double value = Double.parseDouble(text.trim());

            if (value <= 0 || value > 300) {
                sendText(chatId, "❌ Некорректное значение! Укажите число от 1 до 300");
                return;
            }

            if (session.getState() == UserState.WAITING_FOR_HEIGHT) {
                session.pushUndoAction(
                    UserState.WAITING_FOR_HEIGHT,
                    session.getGender(),
                    session.getRatingType(),
                    "height"
                );
                session.getBodyMeasurements().put("height", value);
                session.setState(UserState.WAITING_FOR_WEIGHT);
                sendMeasurementPrompt(chatId, session, "✅ Рост: " + String.format("%.0f", value) + " см");
            } else if (session.getState() == UserState.WAITING_FOR_WEIGHT) {
                session.pushUndoAction(
                    UserState.WAITING_FOR_WEIGHT,
                    session.getGender(),
                    session.getRatingType(),
                    "weight"
                );
                session.getBodyMeasurements().put("weight", value);
                session.setState(UserState.WAITING_FOR_CHEST);
                sendMeasurementPrompt(chatId, session, "✅ Вес: " + String.format("%.1f", value) + " кг");
            } else if (session.getState() == UserState.WAITING_FOR_CHEST) {
                session.pushUndoAction(
                    UserState.WAITING_FOR_CHEST,
                    session.getGender(),
                    session.getRatingType(),
                    "chest"
                );
                session.getBodyMeasurements().put("chest", value);
                session.setState(UserState.WAITING_FOR_WAIST);
                sendMeasurementPrompt(chatId, session, "✅ Грудь: " + String.format("%.0f", value) + " см");
            } else if (session.getState() == UserState.WAITING_FOR_WAIST) {
                session.pushUndoAction(
                    UserState.WAITING_FOR_WAIST,
                    session.getGender(),
                    session.getRatingType(),
                    "waist"
                );
                session.getBodyMeasurements().put("waist", value);
                session.setState(UserState.WAITING_FOR_HIPS);
                sendMeasurementPrompt(chatId, session, "✅ Талия: " + String.format("%.0f", value) + " см");
            } else if (session.getState() == UserState.WAITING_FOR_HIPS) {
                if ("male".equals(session.getGender())) {
                    session.pushUndoAction(
                        UserState.WAITING_FOR_HIPS,
                        session.getGender(),
                        session.getRatingType(),
                        "shoulder"
                    );
                    session.getBodyMeasurements().put("shoulder", value);
                    sendText(
                        chatId,
                        "✅ Плечи: " + String.format("%.0f", value) + " см\n\n🎉 *Данные собраны! Идет анализ...*"
                    );
                } else {
                    session.pushUndoAction(
                        UserState.WAITING_FOR_HIPS,
                        session.getGender(),
                        session.getRatingType(),
                        "hips"
                    );
                    session.getBodyMeasurements().put("hips", value);
                    sendText(
                        chatId,
                        "✅ Бёдра: " + String.format("%.0f", value) + " см\n\n🎉 *Данные собраны! Идет анализ...*"
                    );
                }
                finalizeMeasurements(chatId, session);
            }
        } catch (NumberFormatException e) {
            sendText(chatId, "❌ Ошибка! Введите корректное число\n(без букв и спецсимволов)");
        }
    }

    private void finalizeMeasurements(Long chatId, UserSession session) {
        try {
            double rating = RatingUtils.evaluateBodyByMeasurements(
                session.getBodyMeasurements(),
                session.getGender()
            );
            Double previousRating = session.getPreviousRating("measure");
            session.setLastRating(rating);
            session.addHistoryEntry("measure", session.getGender(), rating);
            persistSessions();

            String result = RatingUtils.generateMeasurementsMessage(
                session.getBodyMeasurements(),
                rating,
                session.isPremium(),
                session.getGender(),
                previousRating
            );
            session.clearUndoActions();
            session.setState(UserState.SELECTING_GENDER);
            sendText(chatId, result);
            mainMenu(chatId);
        } catch (Exception e) {
            sendText(chatId, "❌ Ошибка при обработке данных. Попробуйте еще раз.");
            e.printStackTrace();
        }
    }

    private void handleReceiptFile(Long chatId, String fileId, UserSession session) {
        sendText(chatId, "🔍 Проверяю квитанцию, подождите...");
        try {
            String tmpPath = System.getProperty("java.io.tmpdir") + "/receipt_" + chatId + ".jpg";
            ReceiptAnalyzer.downloadTelegramFile(fileId, this, tmpPath);
            ReceiptAnalyzer.AnalysisResult result = ReceiptAnalyzer.analyzeReceipt(tmpPath);

            sendText(chatId, result.detailedAnalysis);

            if (result.isValid) {
                session.activatePremiumDays(30);
                persistSessions();
                session.setState(UserState.SELECTING_GENDER);
                sendText(chatId,
                    "✅ *Оплата подтверждена!*\n\n"
                    + "💎 Премиум активирован на 30 дней.\n"
                    + "Теперь вам доступны подробный разбор, советы по улучшению и полная история.");
                mainMenu(chatId);
            } else {
                sendText(chatId,
                    result.message + "\n\nПопробуйте отправить более чёткий скриншот квитанции.\n"
                    + "Чтобы отменить — /start");
            }
        } catch (Exception e) {
            sendText(chatId, "❌ Не удалось обработать файл. Отправьте чёткое фото квитанции (JPEG или PNG).");
            e.printStackTrace();
        }
    }

    private void handleGivePremium(Long chatId, String text, UserSession session) {
        if (chatId != ADMIN_CHAT_ID) {
            sendText(chatId, "❌ Нет доступа.");
            return;
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2) {
            session.activatePremiumDays(30);
            persistSessions();
            sendText(chatId, "✅ Премиум активирован вам на 30 дней.");
            mainMenu(chatId);
            return;
        }
        try {
            Long targetId = Long.parseLong(parts[1]);
            UserSession target = users.getOrDefault(targetId, new UserSession());
            users.put(targetId, target);
            target.activatePremiumDays(30);
            persistSessions();
            sendText(chatId, "✅ Премиум выдан пользователю `" + targetId + "` на 30 дней.");
            try {
                sendText(targetId, "🎁 Вам выдан Премиум на 30 дней!\n\nТеперь доступны подробный разбор, советы по улучшению и полная история.");
            } catch (Exception ignored) {}
        } catch (NumberFormatException e) {
            sendText(chatId, "❌ Неверный формат. Используйте: /givepremium <chatId>\nУзнать свой ID: /myid");
        }
    }

    private void handlePhoto(Long chatId, Message msg, UserSession session) {
        try {
            String photoId = msg.getPhoto().get(msg.getPhoto().size() - 1).getFileId();
            String ratingType = session.getRatingType();
            String gender = session.getGender();
            sendText(chatId, "📸 Фото получено! Идет анализ...");

            double rating;
            if ("face".equals(ratingType)) {
                rating = RatingUtils.evaluateFace(photoId, gender);
            } else {
                rating = RatingUtils.evaluateBodyByPhoto(photoId, gender);
            }

            if (rating < 0) {
                sendText(chatId, "😶 На фото не обнаружено лицо.\n\nПожалуйста, скиньте фотографию, где чётко видно лицо анфас — без маски, солнечных очков или сильного наклона головы.");
                mainMenu(chatId);
                return;
            }

            Double previousRating = session.getPreviousRating(ratingType);
            session.setLastRating(rating);
            session.addHistoryEntry(ratingType, gender, rating);
            persistSessions();

            String result = RatingUtils.generateRatingMessage(
                rating,
                session.isPremium(),
                gender,
                previousRating,
                ratingType
            );
            session.clearUndoActions();
            session.setState(UserState.SELECTING_GENDER);
            sendText(chatId, result);
            maybeSendFaceEdit(chatId, photoId, gender, ratingType, rating);
            mainMenu(chatId);
        } catch (Exception e) {
            sendText(chatId, "❌ Ошибка при обработке фото. Попробуйте еще раз.");
            e.printStackTrace();
        }
    }

    private void showHistory(Long chatId, UserSession session) {
        List<UserSession.RatingHistoryEntry> history = session.getHistory();
        if (history.isEmpty()) {
            sendText(
                chatId,
                "📜 *История пуста*\n\nУ вас пока нет ни одной оценки.\n"
                    + "Нажмите «🔄 Новая оценка», чтобы начать!"
            );
            mainMenu(chatId);
            return;
        }

        StringBuilder sb = new StringBuilder("📜 *История ваших оценок*\n");
        sb.append("Всего: ").append(history.size()).append("\n\n");

        if (session.isPremium()) {
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
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🏠 *Главное меню*\n\nЧто хотите сделать?");
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔄 Новая оценка");
        row1.add("⭐ Сравнить со звездой");
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("💎 Премиум");
        row2.add("📜 История");
        rows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("❓ Помощь");
        row3.add(UNDO_BUTTON);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException ignored) {
        }
    }

    private void undo(Long chatId, UserSession session) {
        if (!session.undoLastAction()) {
            sendText(chatId, "↩️ Нечего отменять.");
            return;
        }
        sendStatePrompt(chatId, session, "↩️ Последний шаг отменён.");
    }

    private void sendStatePrompt(Long chatId, UserSession session, String prefix) {
        StringBuilder text = new StringBuilder();
        if (prefix != null && !prefix.isBlank()) {
            text.append(prefix).append("\n\n");
        }

        if (session.getState() == UserState.SELECTING_GENDER) {
            showGenderMenu(chatId, text.append("Выберите ваш пол:").toString());
            return;
        }

        if (session.getState() == UserState.SELECTING_RATING_TYPE) {
            showRatingTypeMenu(chatId, text.append("📋 Выберите тип оценки:").toString());
            return;
        }

        if (session.getState() == UserState.WAITING_FOR_PHOTO) {
            sendPhotoPrompt(chatId, session, text.toString());
            return;
        }

        if (session.getState() == UserState.WAITING_FOR_HEIGHT
            || session.getState() == UserState.WAITING_FOR_WEIGHT
            || session.getState() == UserState.WAITING_FOR_CHEST
            || session.getState() == UserState.WAITING_FOR_WAIST
            || session.getState() == UserState.WAITING_FOR_HIPS) {
            sendMeasurementPrompt(chatId, session, text.toString().trim());
            return;
        }

        mainMenu(chatId);
    }

    private void showGenderMenu(Long chatId) {
        showGenderMenu(chatId, "👋 Добро пожаловать в бот оценки внешности!\n\nВыберите ваш пол:");
    }

    private void showGenderMenu(Long chatId, String text) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("👨 Мужчина");
        row.add("👩 Женщина");
        rows.add(row);

        KeyboardRow undoRow = new KeyboardRow();
        undoRow.add(UNDO_BUTTON);
        rows.add(undoRow);

        keyboard.setKeyboard(rows);
        sendText(chatId, text, keyboard);
    }

    private void showRatingTypeMenu(Long chatId) {
        showRatingTypeMenu(chatId, "📋 Выберите тип оценки:");
    }

    private void showRatingTypeMenu(Long chatId, String text) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📸 Оценка лица по фото");
        row1.add("🏃 Оценка тела по фото");
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📏 Оценка тела по замерам");
        rows.add(row2);

        KeyboardRow undoRow = new KeyboardRow();
        undoRow.add(UNDO_BUTTON);
        rows.add(undoRow);

        keyboard.setKeyboard(rows);
        sendText(chatId, text, keyboard);
    }

    private void sendPhotoPrompt(Long chatId, UserSession session) {
        sendPhotoPrompt(chatId, session, "");
    }

    private void sendPhotoPrompt(Long chatId, UserSession session, String prefix) {
        String baseText = "face".equals(session.getRatingType())
            ? "📸 Отправьте фото лица\n\nТребования: четкое фото, анфас, хорошее освещение"
            : "📸 Отправьте фото тела\n\nТребования: четкое фото, в полный рост";
        sendText(chatId, joinPrefix(prefix, baseText), buildUndoKeyboard());
    }

    private void sendMeasurementPrompt(Long chatId, UserSession session) {
        sendMeasurementPrompt(chatId, session, null);
    }

    private void sendMeasurementPrompt(Long chatId, UserSession session, String prefix) {
        String prompt;
        if (session.getState() == UserState.WAITING_FOR_HEIGHT) {
            prompt = "📏 *ПОШАГОВЫЙ ВВОД ЗАМЕРОВ*\n\n📍 Шаг 1 из 5\n\n*Введите ваш рост (см):*\n(например: 175)";
        } else if (session.getState() == UserState.WAITING_FOR_WEIGHT) {
            prompt = "📍 Шаг 2 из 5\n\n*Введите ваш вес (кг):*\n(например: 75)";
        } else if (session.getState() == UserState.WAITING_FOR_CHEST) {
            prompt = "📍 Шаг 3 из 5\n\n*Введите объём груди (см):*\n(например: 100)";
        } else if (session.getState() == UserState.WAITING_FOR_WAIST) {
            prompt = "📍 Шаг 4 из 5\n\n*Введите объём талии (см):*\n(например: 80)";
        } else if ("male".equals(session.getGender())) {
            prompt = "📍 Шаг 5 из 5\n\n*Введите ширину плеч (см):*\n(например: 110)";
        } else {
            prompt = "📍 Шаг 5 из 5\n\n*Введите объём бёдер (см):*\n(например: 90)";
        }
        sendText(chatId, joinPrefix(prefix, prompt), buildUndoKeyboard());
    }

    private ReplyKeyboardMarkup buildUndoKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(UNDO_BUTTON);
        rows.add(row);
        keyboard.setKeyboard(rows);

        return keyboard;
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
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        try {
            execute(message);
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
