package com.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.*;

public class RatingBot extends TelegramLongPollingBot {
    
    private final String BOT_TOKEN = "8762441185:AAHBI8LCr47AD6XJRx-bakOSLHfzGgTVpuk";
    private final String BOT_USERNAME = "Kara_Tabletka_Bot";
    private Map<Long, UserSession> users = new HashMap<>();
    
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
            
            // Обработка кнопок меню
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
                    help(chatId);
                    return;
                }
                
                // Если ждем замеры (пошаговый ввод)
                if (session.getState() == UserState.WAITING_FOR_HEIGHT ||
                    session.getState() == UserState.WAITING_FOR_WEIGHT ||
                    session.getState() == UserState.WAITING_FOR_CHEST ||
                    session.getState() == UserState.WAITING_FOR_WAIST ||
                    session.getState() == UserState.WAITING_FOR_HIPS) {
                    handleMeasurementStep(chatId, text, session);
                    return;
                }
                
                // Если ждем выбор
                if (session.getState() == UserState.SELECTING_GENDER) {
                    handleGenderSelection(chatId, text, session);
                    return;
                }
                
                if (session.getState() == UserState.SELECTING_RATING_TYPE) {
                    handleRatingTypeSelection(chatId, text, session);
                    return;
                }
                
                // Если нажали что-то другое
                sendText(chatId, "❌ Используйте кнопки меню");
                mainMenu(chatId);
            }
            // Обработка фото
            else if (msg.hasPhoto() && session.getState() == UserState.WAITING_FOR_PHOTO) {
                handlePhoto(chatId, msg, session);
            }
            else {
                sendText(chatId, "❌ Отправьте фото или используйте кнопки");
            }
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void start(Long chatId, UserSession s) {
        s.setState(UserState.SELECTING_GENDER);
        s.setPremium(false);
        s.setLastRating(0);
        
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
        s.setPremium(true);
        sendText(chatId, "✅ Премиум активирован!\n\nТеперь вы будете получать подробный разбор каждой оценки!");
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
    
    private void help(Long chatId) {
        sendText(chatId, "🆘 *Помощь*\n\n/start - начать оценку\n/premium - премиум доступ\n/compare - сравнить со звездой\n\nПроцесс: выберите пол -> выберите тип оценки -> отправьте фото/замеры");
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
                    sendText(chatId, "✅ Талия: " + String.format("%.0f", value) + " см\n\n📍 Шаг 5 из 5\n\n*Введите ширину плеч (см):*\n(например: 45)");
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
            s.setLastRating(rating);
            String result = RatingUtils.generateMeasurementsMessage(s.getBodyMeasurements(), rating, s.isPremium(), s.getGender());
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
            // Получаем фото
            String photoId = msg.getPhoto().get(msg.getPhoto().size() - 1).getFileId();
            sendText(chatId, "📸 Фото получено! Идет анализ...");
            
            double rating;
            if (s.getRatingType().equals("face")) {
                rating = RatingUtils.evaluateFace(photoId, s.getGender());
            } else {
                rating = RatingUtils.evaluateBodyByPhoto(photoId, s.getGender());
            }
            
            s.setLastRating(rating);
            String result = RatingUtils.generateRatingMessage(rating, s.isPremium(), s.getGender());
            sendText(chatId, result);
            mainMenu(chatId);
        } catch (Exception e) {
            sendText(chatId, "❌ Ошибка при обработке фото. Попробуйте еще раз.");
            e.printStackTrace();
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
        r2.add("❓ Помощь");
        rows.add(r2);
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
