public class CheckOutput {
    public static void main(String[] args) {
        System.out.println("=== Проверка сравнения ===");
        System.out.println(com.bot.RatingUtils.compareToCelebrity(9.0, "male"));
        System.out.println("\n=== Проверка бесплатного сообщения ===");
        System.out.println(com.bot.RatingUtils.generateRatingMessage(6.0, false, "female"));
        System.out.println("\n=== Проверка премиум сообщения ===");
        System.out.println(com.bot.RatingUtils.generateRatingMessage(8.5, true, "male"));
    }
}
