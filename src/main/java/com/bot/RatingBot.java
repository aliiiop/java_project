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
                } else if (text.equals("💳 Купить Премиум")) {
                    showPaymentDetails(chatId, session);
                    return;
                } else if (text.equals("❓ Помощь")) {
                    help(chatId);
                    return;
                }
                
                // Если ждём чек — просим отправить фото
                if (session.getState() == UserState.WAITING_FOR_RECEIPT) {
                    sendText(chatId, "📸 Отправьте *фото или скриншот* чека об оплате.\n\nЕсли хотите вернуться — нажмите «🔄 Новая оценка».");
                    return;
                }

                // Если ждем замеры
                if (session.getState() == UserState.WAITING_FOR_MEASUREMENTS) {
                    handleMeasurements(chatId, text, session);
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
            // Обработка фото — чек или обычная оценка
            else if (msg.hasPhoto()) {
                if (session.getState() == UserState.WAITING_FOR_RECEIPT) {
                    verifyReceipt(chatId, msg, session);
                } else if (session.getState() == UserState.WAITING_FOR_PHOTO) {
                    handlePhoto(chatId, msg, session);
                } else {
                    sendText(chatId, "❌ Сначала выберите тип оценки через меню.");
                }
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
        if (s.isPremium()) {
            sendText(chatId, "✅ *У вас уже активирован Премиум!* 💎");
            mainMenu(chatId);
            return;
        }
        SendMessage m = new SendMessage();
        m.setChatId(chatId.toString());
        m.setText(
            "💎 *ПРЕМИУМ ПОДПИСКА*\n\n" +
            "Разблокируй полный потенциал:\n\n" +
            "✅ Детальный разбор каждой оценки\n" +
            "✅ Персональные рекомендации\n" +
            "✅ План тренировок и питания\n" +
            "✅ Сравнение с топ-моделями мира\n\n" +
            "💰 *Цена: 990 ₸/месяц*\n\n" +
            "Нажми кнопку ниже чтобы оплатить:"
        );
        m.setParseMode("Markdown");
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow r1 = new KeyboardRow();
        r1.add("💳 Купить Премиум");
        rows.add(r1);
        KeyboardRow r2 = new KeyboardRow();
        r2.add("🔄 Новая оценка");
        r2.add("❓ Помощь");
        rows.add(r2);
        kb.setKeyboard(rows);
        m.setReplyMarkup(kb);
        try { execute(m); } catch (TelegramApiException e) {}
    }

    private void showPaymentDetails(Long chatId, UserSession s) {
        s.setState(UserState.WAITING_FOR_RECEIPT);
        sendText(chatId,
            "💳 *РЕКВИЗИТЫ ДЛЯ ОПЛАТЫ*\n\n" +
            "Переведите *990 ₸* на карту:\n\n" +
            "🏦 Банк: *Любой казахстанский банк*\n" +
            "💳 Номер карты:\n`4400 4303 5445 6268`\n" +
            "👤 Получатель: *Bektemirospanov*\n" +
            "💰 Сумма: *990 ₸*\n" +
            "📝 Назначение: *Премиум подписка*\n\n" +
            "━━━━━━━━━━━━━━━━━━━\n" +
            "📸 После оплаты *отправьте фото или скриншот чека* прямо в этот чат.\n\n" +
            "🤖 ИИ автоматически проверит чек и активирует Премиум!"
        );
    }

    private void verifyReceipt(Long chatId, Message msg, UserSession s) {
        sendText(chatId, "🤖 *Проверяем ваш чек через ИИ...*\n\nЭто займёт 10-15 секунд.");
        try {
            String photoId = msg.getPhoto().get(msg.getPhoto().size() - 1).getFileId();
            String tempFilePath = "temp_receipt_" + chatId + ".jpg";

            ReceiptAnalyzer.downloadTelegramFile(photoId, this, tempFilePath);
            ReceiptAnalyzer.AnalysisResult result = ReceiptAnalyzer.analyzeReceipt(tempFilePath);
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(tempFilePath));

            if (result.isValid) {
                s.setPremium(true);
                s.setState(UserState.SELECTING_GENDER);
                sendText(chatId,
                    "✅ *ПЛАТЁЖ ПОДТВЕРЖДЁН!*\n\n" +
                    result.detailedAnalysis + "\n\n" +
                    "🎉 *Премиум подписка активирована!*\n\n" +
                    "Нажмите «🔄 Новая оценка» для полного анализа 💎"
                );
                mainMenu(chatId);
            } else {
                sendText(chatId,
                    result.message + "\n\n" +
                    result.detailedAnalysis + "\n\n" +
                    "Отправьте чёткий скриншот чека ещё раз."
                );
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке чека: " + e.getMessage());
            sendText(chatId,
                "⚠️ *Ошибка при анализе чека*\n\n" +
                "Попробуйте отправить чек ещё раз.\n\n" +
                "_Убедитесь что:\n• Фото чёткое\n• Сумма 990 ₸\n• На карту 4400 4303 5445 6268_"
            );
        }
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
            s.setState(UserState.WAITING_FOR_MEASUREMENTS);
            if (s.getGender().equals("male")) {
                sendText(chatId, "📏 Введите замеры в формате:\n\nгрудь:95 талия:80\n\nПример: грудь:95 талия:80");
            } else {
                sendText(chatId, "📏 Введите замеры в формате:\n\nгрудь:90 талия:65 бедра:90\n\nПример: грудь:90 талия:65 бедра:90");
            }
        } else {
            sendText(chatId, "❌ Используйте кнопки");
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
    
    private void handleMeasurements(Long chatId, String text, UserSession s) {
        try {
            Map<String, Double> measurements = new HashMap<>();
            String[] parts = text.split(" ");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    measurements.put(kv[0].toLowerCase(), Double.parseDouble(kv[1]));
                }
            }
            
            double rating = RatingUtils.evaluateBodyByMeasurements(measurements, s.getGender());
            s.setLastRating(rating);
            String result = RatingUtils.generateRatingMessage(rating, s.isPremium(), s.getGender());
            sendText(chatId, result);
            mainMenu(chatId);
        } catch(Exception e) {
            sendText(chatId, "❌ Неверный формат!\n\nИспользуйте: грудь:90 талия:65 бедра:90\n\nПример: грудь:90 талия:65 бедра:90");
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
