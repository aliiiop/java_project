// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class main {
   private static final String TOKEN = System.getenv("8613345099:AAEtOscIcdgQbpVa0o1-qS8aNP9FZx8Fiqw");
   private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
   private static final Pattern UPDATE_PATTERN = Pattern.compile("\"update_id\"\\s*:\\s*(\\d+).*?\"chat\"\\s*:\\s*\\{.*?\"id\"\\s*:\\s*(-?\\d+).*?\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", 32);

   main() {
   }

   public static void main(String[] var0) throws Exception {
      if (TOKEN != null && !TOKEN.isBlank()) {
         long var1 = 0L;
         System.out.println("Бот запущен...");

         while(true) {
            try {
               String var3 = getUpdates(var1);
               Matcher var4 = UPDATE_PATTERN.matcher(var3);

               while(var4.find()) {
                  long var5 = Long.parseLong(var4.group(1));
                  long var7 = Long.parseLong(var4.group(2));
                  String var9 = unescapeJson(var4.group(3));
                  var1 = var5 + 1L;
                  handleMessage(var7, var9);
               }
            } catch (Exception var10) {
               System.out.println("Ошибка: " + var10.getMessage());
               Thread.sleep(3000L);
            }
         }
      }

      System.out.println("Укажи токен бота в переменной окружения BOT_TOKEN");
   }

   private static void handleMessage(long var0, String var2) throws IOException, InterruptedException {
      if (var2 != null && !var2.isBlank()) {
         if ("/start".equals(var2.trim())) {
            sendMessage(var0, "Привет. Я простой Java Telegram-бот.");
         } else {
            sendMessage(var0, "Ты написал: " + var2);
         }
      }
   }

   private static String getUpdates(long var0) throws IOException, InterruptedException {
      HttpRequest var2 = HttpRequest.newBuilder().uri(URI.create("https://api.telegram.org/bot" + TOKEN + "/getUpdates?timeout=30&offset=" + var0)).timeout(Duration.ofSeconds(35L)).GET().build();
      return (String)CLIENT.send(var2, BodyHandlers.ofString()).body();
   }

   private static void sendMessage(long var0, String var2) throws IOException, InterruptedException {
      String var3 = "chat_id=" + var0 + "&text=" + encode(var2);
      HttpRequest var4 = HttpRequest.newBuilder().uri(URI.create("https://api.telegram.org/bot" + TOKEN + "/sendMessage")).timeout(Duration.ofSeconds(15L)).header("Content-Type", "application/x-www-form-urlencoded").POST(BodyPublishers.ofString(var3)).build();
      CLIENT.send(var4, BodyHandlers.ofString());
   }

   private static String encode(String var0) {
      return URLEncoder.encode(var0, StandardCharsets.UTF_8);
   }

   private static String unescapeJson(String var0) {
      return var0.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
   }
}
