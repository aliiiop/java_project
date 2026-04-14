package com.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RatingBot extends TelegramLongPollingBot {

    private final String BOT_TOKEN = "8762441185:AAHBI8LCr47AD6XJRx-bakOSLHfzGgTVpuk";
    private final String BOT_USERNAME = "Kara_Tabletka_Bot";
    private final Map<Long, UserSession> users = new HashMap<>();

    public RatingBot() {
        users.putAll(SessionStorage.loadAll());
        System.out.println("📜 Загружены сессии для " + users.size() + " пользователей");
    }

    private void persistSessions() {
        Map<Long, UserSession> data = new HashMap<>();
        for (Map.Entry<Long, UserSession> e : users.entrySet()) {
            if (e.getValue().hasPersistentData()) {
                data.put(e.getKey(), e.getValue());
            }
        }
        SessionStorage.saveAll(data);
    }

    @Override public String getBotUsername() { return BOT_USERNAME; }
    @Override public String getBotToken() { return BOT_TOKEN; }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (!update.hasMessage()) return;
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
                }

                if (session.getState() == UserState.WAITING_FOR_HEIGHT ||
                    session.getState() == UserState.WAITING_FOR_WEIGHT ||
                    session.getState() == UserState.WAITING_FOR_CHEST ||
                    session.getState() == UserState.WAITING_FOR_WAIST ||
                    session.getState() == UserState.WAITING_FOR_HIPS) {
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
            } else if (msg.hasPhoto() && session.getState() == UserState.WAITING_FOR_PHOTO) {
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
        s.setState(UserState.SELECTING_GENDER);

        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText("👋 Добро пожаловать в бот оценки внешности!\n\nВыберите ваш пол:");

        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("👨 Мужчина");
        row.add("👩 Женщина");
        rows.add(row);
        kb.setKeyboard(rows);
        m.setReplyMarkup(kb);
        try { execute(m); } catch (TelegramApiException e) {}
    }

    private void premium(Long chatId, UserSession s) {
        if (!s.isPremium()) {
            s.activatePremiumDays(30);
            persistSessions();
            sendText(chatId, "✅ Премиум активирован на 30 дней!\n\nТеперь вам доступны:\n• подробный разбор по каждой оценке\n• рекомендации, как поднять балл\n• сравнение с прошлым результатом\n• расширенная история и статистика");
        } else {
            String until = s.getPremiumUntilLabel();
            String suffix = until == null ? "" : "\nАктивен до: " + until;
            sendText(chatId, "💎 Премиум уже активен." + suffix + "\n\nВам доступны подробный разбор, советы по улучшению и полная история.");
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
        sendText(chatId, "🆘 *Помощь*\n\n/start - начать оценку\n/premium - premium доступ\n/compare - сравнить со звездой\n/history - посмотреть историю ваших прошлых оценок\n\nПроцесс: выберите пол -> выберите тип оценки -> отправьте фото или введите замеры" + premiumInfo);
        mainMenu(chatId);
    }

    private void handleGenderSelection(Long chatId, String text, UserSession s) {
        if (text.equals("👨 Мужчина")) {
            s.setGender("male");
        } else if (text.equals("👩 Женщина")) {
            s.setGender("female");
        } else {
            sendText(chatId, "❌ Используйте кнопки");
            return;
        }

        s.setState(UserState.SELECTING_RATING_TYPE);

        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText("📋 Выберите тип оценки:");

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
        kb.setKeyboard(rows);
        m.setReplyMarkup(kb);
        try { execute(m); } catch (TelegramApiException e) {}
    }

    private void handleRatingTypeSelection(Long chatId, String text, UserSession s) {
        if (text.equals("📸 Оценка лица по фото")) {
            s.setRatingType("face");
            s.setState(UserState.WAITING_FOR_PHOTO);
            sendText(chatId, "📸 Отправьте фото лица\n\nТребования: четкое фото, анфас, хорошее освещение");
        } else if (text.equals("🏃 Оценка тела по фото")) {
            s.setRatingType("body");
            s.setState(UserState.WAITING_FOR_PHOTO);
            sendText(chatId, "📸 Отправьте фото тела\n\nТребования: четкое фото, в полный рост");
        } else if (text.equals("📏 Оценка тела по замерам")) {
            s.setRatingType("measure");
            s.getBodyMeasurements().clear();
            s.setState(UserState.WAITING_FOR_HEIGHT);
            sendText(chatId, "📏 *ПОШАГОВЫЙ ВВОД ЗАМЕРОВ*\n\n📍 Шаг 1 из 5\n\n*Введите ваш рост (см):*\n(например: 175)");
        } else {
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
                s.getBodyMeasurements().put("height", value);
                s.setState(UserState.WAITING_FOR_WEIGHT);
                sendText(chatId, "✅ Рост: " + String.format("%.0f", value) + " см\n\n📍 Шаг 2 из 5\n\n*Введите ваш вес (кг):*\n(например: 75)");
            } else if (s.getState() == UserState.WAITING_FOR_WEIGHT) {
                s.getBodyMeasurements().put("weight", value);
                s.setState(UserState.WAITING_FOR_CHEST);
                sendText(chatId, "✅ Вес: " + String.format("%.1f", value) + " кг\n\n📍 Шаг 3 из 5\n\n*Введите объём груди (см):*\n(например: 100)");
            } else if (s.getState() == UserState.WAITING_FOR_CHEST) {
                s.getBodyMeasurements().put("chest", value);
                s.setState(UserState.WAITING_FOR_WAIST);
                sendText(chatId, "✅ Грудь: " + String.format("%.0f", value) + " см\n\n📍 Шаг 4 из 5\n\n*Введите объём талии (см):*\n(например: 80)");
            } else if (s.getState() == UserState.WAITING_FOR_WAIST) {
                s.getBodyMeasurements().put("waist", value);
                if (s.getGender().equals("male")) {
                    s.setState(UserState.WAITING_FOR_HIPS);
                    sendText(chatId, "✅ Талия: " + String.format("%.0f", value) + " см\n\n📍 Шаг 5 из 5\n\n*Введите ширину плеч (см):*\n(например: 110)");
                } else {
                    s.setState(UserState.WAITING_FOR_HIPS);
                    sendText(chatId, "✅ Талия: " + String.format("%.0f", value) + " см\n\n📍 Шаг 5 из 5\n\n*Введите объём бёдер (см):*\n(например: 90)");
                }
            } else if (s.getState() == UserState.WAITING_FOR_HIPS) {
                if (s.getGender().equals("male")) {
                    s.getBodyMeasurements().put("shoulder", value);
                    sendText(chatId, "✅ Плечи: " + String.format("%.0f", value) + " см\n\n🎉 *Данные собраны! Идет анализ...*");
                } else {
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
            sendText(chatId, "📸 Фото получено! Идет анализ...");

            double rating;
            if (s.getRatingType().equals("face")) {
                rating = RatingUtils.evaluateFace(photoId, s.getGender());
            } else {
                rating = RatingUtils.evaluateBodyByPhoto(photoId, s.getGender());
            }

            Double previousRating = s.getPreviousRating(s.getRatingType());
            s.setLastRating(rating);
            s.addHistoryEntry(s.getRatingType(), s.getGender(), rating);
            persistSessions();
            String result = RatingUtils.generateRatingMessage(
                rating, s.isPremium(), s.getGender(), previousRating, s.getRatingType()
            );
            s.setState(UserState.SELECTING_GENDER);
            sendText(chatId, result);
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
            Map<String, Integer> byType = new LinkedHashMap<>();
            for (UserSession.RatingHistoryEntry entry : history) {
                sum += entry.getRating();
                best = Math.max(best, entry.getRating());
                byType.merge(entry.getRatingType(), 1, Integer::sum);
            }
            double average = sum / history.size();
            sb.append("💎 *Премиум-статистика*\n");
            sb.append("Средний балл: ").append(String.format("%.2f", average)).append("\n");
            sb.append("Лучший балл: ").append(String.format("%.1f", best)).append("\n");
            sb.append("По типам: ");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : byType.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(RatingUtils.getRatingTypeLabel(entry.getKey())).append(" - ").append(entry.getValue());
                first = false;
            }
            sb.append("\n\n");

            int idx = 1;
            for (UserSession.RatingHistoryEntry entry : history) {
                sb.append(entry.format(idx++)).append("\n\n");
            }

            sendText(chatId, sb.toString());
            mainMenu(chatId);
            return;
        }

        int start = Math.max(0, history.size() - 10);
        int idx = start + 1;
        for (int i = start; i < history.size(); i++) {
            sb.append(history.get(i).format(idx++)).append("\n\n");
        }

        if (start > 0) {
            sb.append("_(показаны последние 10 из ").append(history.size()).append(")_");
        }

        sendText(chatId, sb.toString());
        mainMenu(chatId);
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
        rows.add(r3);
        kb.setKeyboard(rows);
        m.setReplyMarkup(kb);
        try { execute(m); } catch (TelegramApiException e) {}
    }

    private void sendText(Long chatId, String text) {
        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText(text);
        m.setParseMode("Markdown");
        try { execute(m); } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }
}
