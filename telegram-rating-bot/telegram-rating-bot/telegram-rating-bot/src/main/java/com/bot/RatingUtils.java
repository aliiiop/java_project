package com.bot;

import java.util.Map;

public class RatingUtils {

    public static double evaluateFace(String photoId, String gender) {
        return 1 + Math.random() * 9;
    }

    public static double evaluateBodyByPhoto(String photoId, String gender) {
        return 1 + Math.random() * 9;
    }

    public static double evaluateBodyByMeasurements(Map<String, Double> measurements, String gender) {
        double waist = measurements.getOrDefault("waist", 80.0);
        double hips = measurements.getOrDefault("hips", 90.0);
        double chest = measurements.getOrDefault("chest", 90.0);
        
        double rating;
        if (gender.equals("male")) {
            rating = 10 - (Math.abs(waist - 80) / 10) - (Math.abs(chest - 100) / 15);
        } else {
            rating = 10 - (Math.abs(waist - 65) / 8) - (Math.abs(hips - 90) / 8);
        }
        return Math.max(1, Math.min(10, rating));
    }

    public static String generateRatingMessage(double rating, boolean isPremium, String gender) {
        StringBuilder sb = new StringBuilder();
        
        // Белый экран для низких оценок
        if (rating < 4.0) {
            sb.append("⚪ *WHITE PILL MODE* ⚪\n\n");
            sb.append("┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈\n\n");
        }
        
        sb.append("🌟 *Ваша оценка: ").append(String.format("%.1f", rating)).append(" PSL* 🌟\n\n");

        if (isPremium) {
            sb.append("🔍 *Детальный разбор:*\n");
            sb.append(generateDetailedAnalysis(rating, gender));
            sb.append("\n\n");
        } else {
            sb.append("💰 *ПРЕМИУМ ДОСТУП*\n");
            sb.append("Подпишитесь, чтобы узнать *ПОЧЕМУ* именно эта оценка!\n");
            sb.append("Получите подробный разбор черт лица и фигуры.\n");
            sb.append("🔓 Команда: /premium\n\n");
        }

        sb.append(generatePotentialAdvice(rating, gender));
        sb.append("\n").append(getEditMessage(rating));
        sb.append(getWhitePillMessage(rating, gender));

        return sb.toString();
    }

    private static String generateDetailedAnalysis(double rating, String gender) {
        if (rating < 3.5) {
            return "❌ *Недостатки:*\n• Выраженная асимметрия лица\n• Непропорциональные черты\n• Требуется серьезная работа над образом";
        } else if (rating < 5.5) {
            return "📊 *Средний уровень:*\n• Пропорции в норме\n• Есть зоны для улучшения\n• Правильный стиль может добавить баллов";
        } else {
            return "✨ *Достоинства:*\n• Гармоничные пропорции\n• Выраженные скулы/челюсть\n• Высокий класс внешности";
        }
    }

    private static String generatePotentialAdvice(double rating, String gender) {
        StringBuilder advice = new StringBuilder();
        advice.append("📈 *ПОТЕНЦИАЛ И СОВЕТЫ*\n");
        
        if (rating < 4.0) {
            advice.append("Потенциал: Низкий (но это не приговор!)\n");
            advice.append("💡 *Рекомендации:*\n");
            advice.append("• Смените прическу под тип лица\n");
            advice.append("• Займитесь спортом 3 раза в неделю\n");
            advice.append("• Работайте над осанкой\n");
            advice.append("• Подберите правильную одежду по фигуре\n");
        } else if (rating < 7.0) {
            double potential = Math.min(10, rating + (1.5 + Math.random()));
            advice.append(String.format("Потенциал: Средний (можно раскрыть до %.1f PSL)\n", potential));
            advice.append("💡 *Как раскрыть потенциал:*\n");
            advice.append("• Стильная стрижка +2 к привлекательности\n");
            advice.append("• Уход за кожей лица\n");
            advice.append("• Правильно подобранный гардероб\n");
            advice.append("• Работа над мимикой и улыбкой\n");
        } else {
            double potential = Math.min(10, rating + (0.5 + Math.random()));
            advice.append(String.format("Потенциал: Высокий! Можно достичь %.1f PSL\n", potential));
            advice.append("💡 *Как стать еще лучше:*\n");
            advice.append("• Работайте над харизмой\n");
            advice.append("• Поддерживайте физическую форму\n");
            advice.append("• Развивайте стиль и элегантность\n");
        }

        advice.append("\n💎 *ПЛАТНЫЕ УСЛУГИ*\n");
        advice.append("• Индивидуальный план раскрытия потенциала — 500₽\n");
        advice.append("• Полный разбор внешности со стилистом — 300₽\n");
        advice.append("• Персональные рекомендации по стрижке и стилю — 250₽\n");
        advice.append("📝 Для заказа: /order");

        return advice.toString();
    }

    private static String getEditMessage(double rating) {
        if (rating < 3.5) {
            return "\n🎭 *ЭДИТ: ТЕБЯ МОГГАЮТ* 👎\n(К сожалению, вас превзошли)\n";
        } else if (rating <= 5.5) {
            return "\n🎭 *ЭДИТ: НЕЙТРАЛЬНО* 😐\n(Вы в середине рейтинга)\n";
        } else {
            return "\n🎭 *ЭДИТ: ТЫ МОГГАЕШЬ!* 🔥\n(Вы превосходите большинство)\n";
        }
    }

    private static String getWhitePillMessage(double rating, String gender) {
        if (rating < 4.0) {
            return "\n\n⚪ *WHITE PILL* ⚪\n" +
                   "┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈\n\n" +
                   "Внешность — не главное в жизни.\n" +
                   "Сосредоточься на развитии интеллекта, карьеры и душевных качеств.\n\n" +
                   "Работай над собой, но не зацикливайся.\n" +
                   "*Ты уникален!* 💪\n\n" +
                   "Помни: настоящая ценность внутри тебя.\n" +
                   "┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈";
        }
        return "";
    }

    public static String compareToCelebrity(double rating, String gender) {
        String[] maleCelebs = {
            "Генри Кавилл (9.5)", "Тимоти Шаламе (8.0)", 
            "Киану Ривз (9.0)", "Леонардо ДиКаприо (8.5)"
        };
        String[] femaleCelebs = {
            "Марго Робби (9.5)", "Зендая (8.5)", 
            "Ана де Армас (8.5)", "Скарлетт Йоханссон (9.0)"
        };

        String[] celebs = gender.equals("male") ? maleCelebs : femaleCelebs;
        String celeb = celebs[(int)(Math.random() * celebs.length)];
        double celebRating = Double.parseDouble(celeb.split(" \\(")[1].replace(")", ""));

        StringBuilder result = new StringBuilder();
        result.append("⭐ *СРАВНЕНИЕ СО ЗНАМЕНИТОСТЬЮ* ⭐\n\n");
        result.append("Ваш рейтинг: ").append(String.format("%.1f", rating)).append(" PSL\n");
        result.append("Знаменитость: ").append(celeb).append("\n\n");

        if (rating > celebRating) {
            result.append("✅ *РЕЗУЛЬТАТ:* Вы превосходите эту знаменитость!\n");
            result.append("🎬 Эдит: Вы в роли главной звезды");
        } else if (Math.abs(rating - celebRating) < 0.8) {
            result.append("🔄 *РЕЗУЛЬТАТ:* Вы на одном уровне со звездой!\n");
            result.append("🎬 Эдит: Достойный конкурент");
        } else {
            result.append("📉 *РЕЗУЛЬТАТ:* Знаменитость превосходит вас\n");
            result.append("🎬 Эдит: Работайте над собой");
        }

        return result.toString();
    }
}
