import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class SimpleBot extends TelegramLongPollingBot {
    public String getBotUsername() { return "KARATabletka2_bot"; }
    public String getBotToken() { return "8781857268:AAHAh6le1gwNzmJG-5HInn4ApCOhxuIcdFE"; }
    
    public void onUpdateReceived(Update update) {if ("/start".equals(text)) {
    // текст для старта
    } else if ("/help".equals(text)) {
    // текст помощи
    } else {
    // эхо
}
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            System.out.println("Получено: " + text);
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText("✅ Бот работает! Ты написал: " + text);
            try { execute(msg); } catch(Exception e) { 
                System.out.println("Ошибка: " + e);
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 ЗАПУСК БОТА...");
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new SimpleBot());
        System.out.println("✅ БОТ ЗАПУЩЕН! Иди в Telegram и напиши @KARATabletka2_bot");
    }
}
