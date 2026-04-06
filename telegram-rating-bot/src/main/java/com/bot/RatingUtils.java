package com.bot;
import java.util.*;
public class RatingUtils {
    public static double evaluateFace(String photoId, String gender) {
        if (photoId == null || photoId.isEmpty()) return 0;
        // Генерируем стабильную оценку на основе хеша photoId
        int hash = photoId.hashCode();
        double rating = 3.5 + (Math.abs(hash % 50) / 10.0);
        return Math.max(1.0, Math.min(10.0, rating));
    }
    public static double evaluateBodyByPhoto(String photoId, String gender) {
        if (photoId == null || photoId.isEmpty()) return 0;
        // Генерируем стабильную оценку на основе хеша photoId
        int hash = photoId.hashCode();
        double rating = 3.0 + (Math.abs(hash % 60) / 10.0);
        return Math.max(1.0, Math.min(10.0, rating));
    }
    public static double evaluateBodyByMeasurements(Map<String, Double> m, String gender) {
        // Получаем основные параметры
        double height = m.getOrDefault("height", 175.0); // см
        double weight = m.getOrDefault("weight", 75.0);  // кг
        double chest = m.getOrDefault("chest", 100.0);
        double waist = m.getOrDefault("waist", 80.0);
        double hips = m.getOrDefault("hips", 90.0);
        double shoulder = m.getOrDefault("shoulder", 45.0);
        double neck = m.getOrDefault("neck", 38.0);
        
        double rating = 5.0; // базовая оценка
        
        if (gender.equals("male")) {
            // === МУЖЧИНЫ ===
            // 1. BMI анализ (20-25 идеально)
            double bmi = weight / ((height / 100.0) * (height / 100.0));
            if (bmi >= 18.5 && bmi <= 25.0) {
                rating += 1.5;
            } else if (bmi >= 25.0 && bmi <= 27.0) {
                rating += 0.8;
            } else if (bmi >= 17.0 && bmi < 18.5) {
                rating += 0.5;
            } else {
                rating -= Math.abs(bmi - 23.0) * 0.3;
            }
            
            // 2. Плечи относительно талии (идеально 1.5-1.7)
            double shoulderWaistRatio = shoulder / waist;
            if (shoulderWaistRatio >= 1.5 && shoulderWaistRatio <= 1.7) {
                rating += 1.5;
            } else if (shoulderWaistRatio >= 1.4 && shoulderWaistRatio <= 1.8) {
                rating += 0.8;
            } else {
                rating -= Math.abs(shoulderWaistRatio - 1.6) * 0.8;
            }
            
            // 3. Грудь-талия соотношение (идеально 1.25-1.35)
            double chestWaistRatio = chest / waist;
            if (chestWaistRatio >= 1.20 && chestWaistRatio <= 1.35) {
                rating += 1.2;
            } else {
                rating -= Math.abs(chestWaistRatio - 1.27) * 0.5;
            }
            
            // 4. Рост (175-185 идеально)
            if (height >= 175 && height <= 185) {
                rating += 1.0;
            } else if (height >= 170 && height < 175) {
                rating += 0.5;
            } else if (height > 185 && height <= 195) {
                rating += 0.3;
            } else {
                rating -= Math.abs(height - 177.0) * 0.04;
            }
            
            // 5. Шея (40-42 см идеально)
            if (neck >= 39 && neck <= 43) {
                rating += 0.8;
            } else {
                rating -= Math.abs(neck - 41.0) * 0.1;
            }
            
        } else {
            // === ЖЕНЩИНЫ ===
            // 1. BMI анализ (19-24 идеально)
            double bmi = weight / ((height / 100.0) * (height / 100.0));
            if (bmi >= 19.0 && bmi <= 24.0) {
                rating += 1.5;
            } else if (bmi >= 24.0 && bmi <= 26.0) {
                rating += 0.7;
            } else if (bmi >= 18.0 && bmi < 19.0) {
                rating += 0.8;
            } else {
                rating -= Math.abs(bmi - 21.5) * 0.3;
            }
            
            // 2. Талия-бедра соотношение (идеально 0.65-0.75)
            double waistHipsRatio = waist / hips;
            if (waistHipsRatio >= 0.65 && waistHipsRatio <= 0.75) {
                rating += 2.0;
            } else if (waistHipsRatio >= 0.60 && waistHipsRatio <= 0.80) {
                rating += 1.0;
            } else {
                rating -= Math.abs(waistHipsRatio - 0.70) * 1.5;
            }
            
            // 3. Грудь-талия соотношение (идеально 0.85-0.95)
            double chestWaistRatio = chest / waist;
            if (chestWaistRatio >= 0.85 && chestWaistRatio <= 0.95) {
                rating += 1.2;
            } else {
                rating -= Math.abs(chestWaistRatio - 0.90) * 0.6;
            }
            
            // 4. Рост (162-175 идеально)
            if (height >= 162 && height <= 175) {
                rating += 1.0;
            } else if (height >= 158 && height < 162) {
                rating += 0.4;
            } else if (height > 175 && height <= 183) {
                rating += 0.6;
            } else {
                rating -= Math.abs(height - 168.0) * 0.03;
            }
            
            // 5. Объём груди (85-95 см идеально)
            if (chest >= 85 && chest <= 95) {
                rating += 0.8;
            } else {
                rating -= Math.abs(chest - 90.0) * 0.08;
            }
        }
        
        return Math.max(1.0, Math.min(10.0, rating));
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
    public static String generateMeasurementsMessage(Map<String, Double> measurements, double rating, boolean premium, String gender) {
        double height = measurements.getOrDefault("height", 175.0);
        double weight = measurements.getOrDefault("weight", 75.0);
        double chest = measurements.getOrDefault("chest", 100.0);
        double waist = measurements.getOrDefault("waist", 80.0);
        double hips = measurements.getOrDefault("hips", 90.0);
        double shoulder = measurements.getOrDefault("shoulder", 45.0);
        double neck = measurements.getOrDefault("neck", 38.0);
        
        double bmi = weight / ((height / 100.0) * (height / 100.0));
        String msg = "🌟 *АНАЛИЗ ПО ЗАМЕРАМ: " + String.format("%.1f", rating) + " PSL* 🌟\n\n";
        
        if (gender.equals("male")) {
            msg += "📊 *ПАРАМЕТРЫ:*\n";
            msg += "• Рост: " + String.format("%.0f", height) + " см " + (height >= 175 && height <= 185 ? "✅" : "❌") + "\n";
            msg += "• Вес: " + String.format("%.1f", weight) + " кг\n";
            msg += "• BMI: " + String.format("%.1f", bmi) + " " + (bmi >= 18.5 && bmi <= 25.0 ? "✅ (идеально)" : "⚠️") + "\n";
            msg += "• Плечи: " + String.format("%.1f", shoulder) + " см\n";
            msg += "• Грудь: " + String.format("%.1f", chest) + " см\n";
            msg += "• Талия: " + String.format("%.1f", waist) + " см\n";
            msg += "• Шея: " + String.format("%.1f", neck) + " см\n\n";
            
            msg += "💪 *СООТНОШЕНИЯ:*\n";
            double shoulderWaistRatio = shoulder / waist;
            double chestWaistRatio = chest / waist;
            msg += "• Плечи/Талия: " + String.format("%.2f", shoulderWaistRatio) + " " + (shoulderWaistRatio >= 1.5 && shoulderWaistRatio <= 1.7 ? "✅" : "⚠️") + "\n";
            msg += "• Грудь/Талия: " + String.format("%.2f", chestWaistRatio) + " " + (chestWaistRatio >= 1.20 && chestWaistRatio <= 1.35 ? "✅" : "⚠️") + "\n\n";
        } else {
            msg += "📊 *ПАРАМЕТРЫ:*\n";
            msg += "• Рост: " + String.format("%.0f", height) + " см " + (height >= 162 && height <= 175 ? "✅" : "❌") + "\n";
            msg += "• Вес: " + String.format("%.1f", weight) + " кг\n";
            msg += "• BMI: " + String.format("%.1f", bmi) + " " + (bmi >= 19.0 && bmi <= 24.0 ? "✅ (идеально)" : "⚠️") + "\n";
            msg += "• Грудь: " + String.format("%.1f", chest) + " см " + (chest >= 85 && chest <= 95 ? "✅" : "⚠️") + "\n";
            msg += "• Талия: " + String.format("%.1f", waist) + " см\n";
            msg += "• Бёдра: " + String.format("%.1f", hips) + " см\n\n";
            
            msg += "✨ *ПРОПОРЦИИ:*\n";
            double waistHipsRatio = waist / hips;
            double chestWaistRatio = chest / waist;
            msg += "• Талия/Бёдра: " + String.format("%.2f", waistHipsRatio) + " " + (waistHipsRatio >= 0.65 && waistHipsRatio <= 0.75 ? "✅ (идеально)" : "⚠️") + "\n";
            msg += "• Грудь/Талия: " + String.format("%.2f", chestWaistRatio) + " " + (chestWaistRatio >= 0.85 && chestWaistRatio <= 0.95 ? "✅" : "⚠️") + "\n\n";
        }
        
        msg += "🎯 *ИТОГ:*\n";
        if (rating < 3.5) {
            msg += "🔴 *УРОВЕНЬ: НЕ ДО КОНЦА* 👎\nНужна работа над телом. Начни с базы!";
        } else if (rating < 5.0) {
            msg += "🟡 *УРОВЕНЬ: СРЕДНИЙ* 😐\nХорошие базовые параметры. Есть где расти!";
        } else if (rating < 7.0) {
            msg += "🟢 *УРОВЕНЬ: ХОРОШИЙ* 💪\nТвоё тело - наш аналог!";
        } else if (rating < 8.5) {
            msg += "🟢 *УРОВЕНЬ: ОТЛИЧНЫЙ* 🔥\nТы на пике! Держи форму!";
        } else {
            msg += "🟢 *УРОВЕНЬ: ЭЛИТ* 👑\nТы за гранью добра и зла. МОГГЕР 2000!";
        }
        
        if (!premium) msg += "\n\n💰 Премиум разбор: /premium";
        return msg;
    }
    public static String compareToCelebrity(double rating, String gender) {
        String[] celebs = gender.equals("male") ? new String[]{"Генри Кавилл (9.5)","Тимоти Шаламе (8.0)"} : new String[]{"Марго Робби (9.5)","Зендая (8.5)"};
        String celeb = celebs[(int)(Math.random()*celebs.length)];
        return "⭐ *Сравнение:*\nВаш рейтинг: "+String.format("%.1f",rating)+"\nЗнаменитость: "+celeb;
    }
}
