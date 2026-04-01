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
    
    private final String BOT_TOKEN = "8781857268:AAHAh6le1gwNzmJG-5HInn4ApCOhxuIcdFE";
    private final String BOT_USERNAME = "@KARATabletka2_bot";
    
    private Map<Long, UserSession> userSessions = new HashMap<>();

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
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String text = message.getText();
            
            UserSession session = userSessions.getOrDefault(chatId, new UserSession());
            userSessions.put(chatId, session);
            
            if (text.equals("/start")) {
                startCommand(chatId, session);
            } 
            else if (text.equals("/premium")) {
                premiumCommand(chatId, session);
            }
            else if (text.equals("/compare")) {
                compareCommand(chatId, session);
            }
            else if (text.equals("/order")) {
                orderCommand(chatId);
            }
            else if (text.equals("/help")) {
                helpCommand(chatId);
            }
            else {
                handleUserInput(chatId, text, message, session);
            }
        }
    }
    
    private void startCommand(Long chatId, UserSession session) {
        session.setState(UserState.SELECTING_GENDER);
        session.setPremium(false);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("👋 *Добро пожаловать в бот оценки внешности!*\n\n" +
                        "Я помогу оценить вашу внешность по шкале PSL (1-10).\n\n" +
                        "🔹 *Бесплатная версия:* простая оценка\n" +
                        "🔹 *Премиум:* подробный разбор \"ПОЧЕМУ\" такая оценка\n\n" +
                        "Выберите ваш пол:");
        message.setParseMode("Markdown");
        
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("👨 Мужчина");
        row.add("👩 Женщина");
        rows.add(row);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void premiumCommand(Long chatId, UserSession session) {
        session.setPremium(true);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("✅ *Премиум доступ активирован!*\n\n" +
                        "Теперь вы будете получать подробный разбор каждой оценки.\n" +
                        "Вы узнаете *ПОЧЕМУ* именно такая оценка!\n\n" +
                        "Продолжайте использовать бота.");
        message.setParseMode("Markdown");
        
        try {
            execute(message);
            sendMainMenu(chatId);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void compareCommand(Long chatId, UserSession session) {
        if (session.getLastRating() == 0) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("❌ Сначала получите оценку внешности!\n" +
                           "Используйте /start чтобы начать.");
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        
        String comparison = RatingUtils.compareToCelebrity(session.getLastRating(), session.getGender());
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(comparison);
        message.setParseMode("Markdown");
        
        try {
            execute(message);
            sendMainMenu(chatId);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void orderCommand(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📞 *Заказ платных услуг*\n\n" +
                        "Для заказа напишите нашему менеджеру:\n" +
                        "@SupportBot\n\n" +
                        "Укажите желаемую услугу и мы свяжемся с вами!");
        message.setParseMode("Markdown");
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void helpCommand(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🆘 *Помощь*\n\n" +
                        "Доступные команды:\n" +
                        "/start - Начать оценку\n" +
                        "/premium - Активировать премиум\n" +
                        "/compare - Сравнить со знаменитостью\n" +
                        "/order - Заказать услуги\n" +
                        "/help - Эта справка\n\n" +
                        "Процесс оценки:\n" +
                        "1. Выберите пол\n" +
                        "2. Выберите тип оценки (лицо/тело)\n" +
                        "3. Отправьте фото или замеры\n" +
                        "4. Получите результат!");
        message.setParseMode("Markdown");
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void startNewEvaluation(Long chatId, UserSession session) {
        boolean wasPremium = session.isPremium();
        session = new UserSession();
        session.setPremium(wasPremium);
        userSessions.put(chatId, session);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🔄 *Новая оценка*\n\nВыберите ваш пол:");
        message.setParseMode("Markdown");
        
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("👨 Мужчина");
        row.add("👩 Женщина");
        rows.add(row);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleUserInput(Long chatId, String text, Message message, UserSession session) {
        // Обработка кнопок главного меню
        if (text.equals("🔄 Новая оценка")) {
            startNewEvaluation(chatId, session);
            return;
        } else if (text.equals("⭐ Сравнить со звездой")) {
            compareCommand(chatId, session);
            return;
        } else if (text.equals("💎 Премиум")) {
            premiumCommand(chatId, session);
            return;
        } else if (text.equals("❓ Помощь")) {
            helpCommand(chatId);
            return;
        }
        
        // Обработка состояний
        switch (session.getState()) {
            case SELECTING_GENDER:
                handleGenderSelection(chatId, text, session);
                break;
            case SELECTING_RATING_TYPE:
                handleRatingTypeSelection(chatId, text, session);
                break;
            case WAITING_FOR_PHOTO:
                handlePhoto(chatId, message, session);
                break;
            case WAITING_FOR_MEASUREMENTS:
                handleMeasurements(chatId, text, session);
                break;
            default:
                sendMainMenu(chatId);
        }
    }
    
    private void handleGenderSelection(Long chatId, String text, UserSession session) {
        if (text.equals("👨 Мужчина")) {
            session.setGender("male");
        } else if (text.equals("👩 Женщина")) {
            session.setGender("female");
        } else {
            sendInvalidInput(chatId);
            return;
        }
        
        session.setState(UserState.SELECTING_RATING_TYPE);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📋 *Выберите тип оценки:*");
        message.setParseMode("Markdown");
        
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
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleRatingTypeSelection(Long chatId, String text, UserSession session) {
        if (text.equals("📸 Оценка лица по фото")) {
            session.setRatingType("face");
            session.setState(UserState.WAITING_FOR_PHOTO);
            sendPhotoRequest(chatId, "лица");
        } 
        else if (text.equals("🏃 Оценка тела по фото")) {
            session.setRatingType("body_photo");
            session.setState(UserState.WAITING_FOR_PHOTO);
            sendPhotoRequest(chatId, "тела");
        }
        else if (text.equals("📏 Оценка тела по замерам")) {
            session.setRatingType("body_measurements");
            session.setState(UserState.WAITING_FOR_MEASUREMENTS);
            sendMeasurementsRequest(chatId, session.getGender());
        }
        else {
            sendInvalidInput(chatId);
        }
    }
    
    private void sendPhotoRequest(Long chatId, String type) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("📸 *Отправьте фото " + type + "*\n\n" +
                       "Требования к фото:\n" +
                       "• Четкое, анфас\n" +
                       "• Хорошее освещение\n" +
                       "• Без фильтров");
        message.setParseMode("Markdown");
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void sendMeasurementsRequest(Long chatId, String gender) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setParseMode("Markdown");
        
        if (gender.equals("male")) {
            message.setText("📏 *Введите замеры*\n\n" +
                           "Формат: грудь:95 талия:80\n\n" +
                           "Пример: грудь:95 талия:80");
        } else {
            message.setText("📏 *Введите замеры*\n\n" +
                           "Формат: грудь:90 талия:65 бедра:90\n\n" +
                           "Пример: грудь:90 талия:65 бедра:90");
        }
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handlePhoto(Long chatId, Message message, UserSession session) {
        if (message.hasPhoto()) {
            String photoId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
            double rating = 0;
            
            if (session.getRatingType().equals("face")) {
                rating = RatingUtils.evaluateFace(photoId, session.getGender());
            } else {
                rating = RatingUtils.evaluateBodyByPhoto(photoId, session.getGender());
            }
            
            session.setLastRating(rating);
            sendRatingResult(chatId, rating, session);
        } else {
            SendMessage error = new SendMessage();
            error.setChatId(chatId.toString());
            error.setText("❌ Пожалуйста, отправьте фотографию");
            try {
                execute(error);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void handleMeasurements(Long chatId, String text, UserSession session) {
        try {
            Map<String, Double> measurements = new HashMap<>();
            String[] parts = text.split(" ");
            
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    String key = kv[0].toLowerCase();
                    double value = Double.parseDouble(kv[1]);
                    measurements.put(key, value);
                }
            }
            
            session.setBodyMeasurements(measurements);
            double rating = RatingUtils.evaluateBodyByMeasurements(measurements, session.getGender());
            session.setLastRating(rating);
            sendRatingResult(chatId, rating, session);
            
        } catch (Exception e) {
            SendMessage error = new SendMessage();
            error.setChatId(chatId.toString());
            error.setText("❌ *Неверный формат!*\n\n" +
                         "Используйте: грудь:90 талия:65 бедра:90\n\n" +
                         "Пример: грудь:90 талия:65 бедра:90");
            error.setParseMode("Markdown");
            try {
                execute(error);
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void sendRatingResult(Long chatId, double rating, UserSession session) {
        String result = RatingUtils.generateRatingMessage(rating, session.isPremium(), session.getGender());
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(result);
        message.setParseMode("Markdown");
        
        try {
            execute(message);
            sendMainMenu(chatId);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void sendMainMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🏠 *Главное меню*\n\n" +
                       "Что хотите сделать?");
        message.setParseMode("Markdown");
        
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        
        List<KeyboardRow> rows = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔄 Новая оценка");
        row1.add("⭐ Сравнить со звездой");
        rows.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("💎 Премиум");
        row2.add("❓ Помощь");
        rows.add(row2);
        
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void sendInvalidInput(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("❌ Пожалуйста, используйте кнопки меню");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
