import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TestBot extends TelegramLongPollingBot {
    public String getBotUsername() { return "KARATabletka2_bot"; }
    public String getBotToken() { return "8781857268:AAHAh6le1gwNzmJG-5HInn4ApCOhxuIcdFE"; }
    
    public void onUpdateReceived(Update update) {
        System.out.println("ПОЛУЧЕНО ОБНОВЛЕНИЕ: " + update);
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            System.out.println("Сообщение от " + chatId + ": " + text);
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText("БОТ РАБОТАЕТ! Ты написал: " + text);
            try { execute(msg); } catch(Exception e) { 
                System.out.println("Ошибка отправки: " + e);
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("ЗАПУСК БОТА...");
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new TestBot());
        System.out.println("БОТ ЗАПУЩЕН! ЖДУ СООБЩЕНИЙ...");
        Thread.sleep(100000000);
    }
}
