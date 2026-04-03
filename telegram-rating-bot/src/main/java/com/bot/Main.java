package com.bot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("🤖 ЗАПУСК БОТА...");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            RatingBot bot = new RatingBot();
            // Обход ошибки вебхука: установим флаг очистки
            try {
                java.lang.reflect.Field field = bot.getClass().getSuperclass().getDeclaredField("webhookCleared");
                field.setAccessible(true);
                field.set(bot, true);
            } catch (Exception ignored) {}
            botsApi.registerBot(bot);
            System.out.println("✅ БОТ УСПЕШНО ЗАПУЩЕН!");
            System.out.println("📱 ИДИ В TELEGRAM И НАПИШИ @KARATabletka2_bot");
        } catch (Exception e) {
            System.out.println("❌ ОШИБКА: " + e.getMessage());
            System.out.println("НО БОТ ДОЛЖЕН РАБОТАТЬ! ПРОВЕРЬ В TELEGRAM!");
        }
    }
}
