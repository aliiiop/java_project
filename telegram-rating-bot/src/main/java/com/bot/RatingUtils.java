package com.bot;
import java.util.*;
public class RatingUtils {
    public static double evaluateFace(String photoId, String gender) { return 1 + Math.random() * 9; }
    public static double evaluateBodyByPhoto(String photoId, String gender) { return 1 + Math.random() * 9; }
    public static double evaluateBodyByMeasurements(Map<String, Double> m, String gender) {
        double waist = m.getOrDefault("waist", 80.0);
        double hips = m.getOrDefault("hips", 90.0);
        double chest = m.getOrDefault("chest", 90.0);
        if (gender.equals("male")) return Math.max(1, Math.min(10, 10 - (Math.abs(waist-80)/10) - (Math.abs(chest-100)/15)));
        return Math.max(1, Math.min(10, 10 - (Math.abs(waist-65)/8) - (Math.abs(hips-90)/8)));
    }
    public static String generateRatingMessage(double rating, boolean premium, String gender) {
        String msg = "🌟 *Ваша оценка: " + String.format("%.1f", rating) + " PSL* 🌟\n\n";
        if (premium) msg += "🔍 *Детальный разбор:*\n" + (rating<3.5?"Низкий уровень":rating<5.5?"Средний уровень":"Высокий уровень") + "\n\n";
        else msg += "💰 Премиум доступ: /premium\n\n";
        msg += "📈 *Потенциал:* " + (rating<4?"Низкий":rating<7?"Средний":"Высокий") + "\n";
        msg += (rating<3.5?"\n🎭 *ЭДИТ: ТЕБЯ МОГГАЮТ* 👎":rating<=5.5?"\n🎭 *ЭДИТ: НЕЙТРАЛЬНО* 😐":"\n🎭 *ЭДИТ: ТЫ МОГГАЕШЬ!* 🔥");
        if (rating<4) msg += "\n\n⚪ *WHITE PILL* ⚪\nВнешность не главное. Развивайся! 💪";
        return msg;
    }
    public static String compareToCelebrity(double rating, String gender) {
        String[] celebs = gender.equals("male") ? new String[]{"Генри Кавилл (9.5)","Тимоти Шаламе (8.0)"} : new String[]{"Марго Робби (9.5)","Зендая (8.5)"};
        String celeb = celebs[(int)(Math.random()*celebs.length)];
        return "⭐ *Сравнение:*\nВаш рейтинг: "+String.format("%.1f",rating)+"\nЗнаменитость: "+celeb;
    }
}
