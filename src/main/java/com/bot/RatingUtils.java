package com.bot;
import java.util.*;

public class RatingUtils {
    public static double evaluateFace(String photoId, String gender) {
        if (photoId == null || photoId.isEmpty()) return 0;
        int hash = photoId.hashCode();
        double rating = 3.5 + (Math.abs(hash % 50) / 10.0);
        return Math.max(1.0, Math.min(10.0, rating));
    }

    public static double evaluateBodyByPhoto(String photoId, String gender) {
        if (photoId == null || photoId.isEmpty()) return 0;
        int hash = photoId.hashCode();
        double rating = 3.0 + (Math.abs(hash % 60) / 10.0);
        return Math.max(1.0, Math.min(10.0, rating));
    }

    public static double evaluateBodyByMeasurements(Map<String, Double> m, String gender) {
        double height = m.getOrDefault("height", 175.0);
        double weight = m.getOrDefault("weight", 75.0);
        double chest = m.getOrDefault("chest", 100.0);
        double waist = m.getOrDefault("waist", 80.0);
        double hips = m.getOrDefault("hips", 90.0);
        double shoulder = m.getOrDefault("shoulder", 45.0);
        double neck = m.getOrDefault("neck", 38.0);

        double rating = 5.0;

        if (gender.equals("male")) {
            double bmi = weight / ((height / 100.0) * (height / 100.0));
            if (bmi >= 18.5 && bmi <= 25.0) rating += 1.5;
            else if (bmi >= 25.0 && bmi <= 27.0) rating += 0.8;
            else if (bmi >= 17.0 && bmi < 18.5) rating += 0.5;
            else rating -= Math.abs(bmi - 23.0) * 0.3;

            double shoulderWaistRatio = shoulder / waist;
            if (shoulderWaistRatio >= 1.5 && shoulderWaistRatio <= 1.7) rating += 1.5;
            else if (shoulderWaistRatio >= 1.4 && shoulderWaistRatio <= 1.8) rating += 0.8;
            else rating -= Math.abs(shoulderWaistRatio - 1.6) * 0.8;

            double chestWaistRatio = chest / waist;
            if (chestWaistRatio >= 1.20 && chestWaistRatio <= 1.35) rating += 1.2;
            else rating -= Math.abs(chestWaistRatio - 1.27) * 0.5;

            if (height >= 175 && height <= 185) rating += 1.0;
            else if (height >= 170 && height < 175) rating += 0.5;
            else if (height > 185 && height <= 195) rating += 0.3;
            else rating -= Math.abs(height - 177.0) * 0.04;

            if (neck >= 39 && neck <= 43) rating += 0.8;
            else rating -= Math.abs(neck - 41.0) * 0.1;

        } else {
            double bmi = weight / ((height / 100.0) * (height / 100.0));
            if (bmi >= 19.0 && bmi <= 24.0) rating += 1.5;
            else if (bmi >= 24.0 && bmi <= 26.0) rating += 0.7;
            else if (bmi >= 18.0 && bmi < 19.0) rating += 0.8;
            else rating -= Math.abs(bmi - 21.5) * 0.3;

            double waistHipsRatio = waist / hips;
            if (waistHipsRatio >= 0.65 && waistHipsRatio <= 0.75) rating += 2.0;
            else if (waistHipsRatio >= 0.60 && waistHipsRatio <= 0.80) rating += 1.0;
            else rating -= Math.abs(waistHipsRatio - 0.70) * 1.5;

            double chestWaistRatio = chest / waist;
            if (chestWaistRatio >= 0.85 && chestWaistRatio <= 0.95) rating += 1.2;
            else rating -= Math.abs(chestWaistRatio - 0.90) * 0.6;

            if (height >= 162 && height <= 175) rating += 1.0;
            else if (height >= 158 && height < 162) rating += 0.4;
            else if (height > 175 && height <= 183) rating += 0.6;
            else rating -= Math.abs(height - 168.0) * 0.03;

            if (chest >= 85 && chest <= 95) rating += 0.8;
            else rating -= Math.abs(chest - 90.0) * 0.08;
        }

        return Math.max(1.0, Math.min(10.0, rating));
    }

    public static String generateRatingMessage(double rating, boolean premium, String gender) {
        StringBuilder msg = new StringBuilder();
        msg.append("🌟 *ВАШ РЕЙТИНГ: ").append(String.format("%.1f", rating)).append("/10* 🌟\n\n");

        if (!premium) {
            msg.append("💎 *Обновись до Премиума для полного анализа!*\n\n");
            msg.append("Базовая информация:\n");
            msg.append("📈 Потенциал: ").append(rating < 4 ? "Низкий" : rating < 7 ? "Средний" : "Высокий").append("\n");
            msg.append(rating < 3.5 ? "🎭 Уровень: ПОКА ЧТО НЕ ДО КОНЦА 👎" :
                      rating <= 5.5 ? "🎭 Уровень: НЕЙТРАЛЬНО 😐" :
                      "🎭 Уровень: ХОРОШО! 🔥").append("\n\n");
            msg.append("Нажми 💎 Премиум для:\n");
            msg.append("✅ Подробного разбора\n");
            msg.append("✅ Персональных советов\n");
            msg.append("✅ Планов улучшения\n");
        } else {
            msg.append("═══════════════════════════════\n");
            msg.append("🔍 *ПРЕМИУМ АНАЛИЗ*\n");
            msg.append("═══════════════════════════════\n\n");

            // Подробный разбор
            if (rating < 3.5) {
                msg.append("📊 *УРОВЕНЬ: НЕ ДО КОНЦА*\n");
                msg.append("Твой профиль требует активной работы над внешностью.\n\n");
                msg.append("🎯 *РЕКОМЕНДАЦИИ:*\n");
                msg.append("1️⃣ *Физическая подготовка:*\n");
                msg.append("   • Тренировки 4-5 раз в неделю\n");
                msg.append("   • Силовые упражнения для мышечного корсета\n");
                msg.append("   • Кардио для рельефа\n\n");
                msg.append("2️⃣ *Питание:*\n");
                msg.append("   • Белки: 1.5-2г на кг веса\n");
                msg.append("   • Калорийный дефицит для сжигания жира\n");
                msg.append("   • Вода: минимум 2-3л в день\n\n");
                msg.append("3️⃣ *Внешний вид:*\n");
                msg.append("   • Качественный уход за кожей\n");
                msg.append("   • Стильная причёска\n");
                msg.append("   • Подходящий гардероб\n\n");
                msg.append("⏱️ *Примерный срок улучшения: 3-6 месяцев*\n");
            } else if (rating < 5.5) {
                msg.append("📊 *УРОВЕНЬ: СРЕДНИЙ*\n");
                msg.append("Хорошая база! Есть куда расти.\n\n");
                msg.append("🎯 *РЕКОМЕНДАЦИИ:*\n");
                msg.append("1️⃣ *Фокус на мышцы:*\n");
                msg.append("   • Увеличить объём мышц (гипертрофия)\n");
                msg.append("   • Тренировки 3-4 раза в неделю\n");
                msg.append("   • Прогрессивная перегрузка\n\n");
                msg.append("2️⃣ *Оптимизация пропорций:*\n");
                msg.append("   • Работа над шириной плеч/спины\n");
                msg.append("   • Снижение процента жира в организме\n");
                msg.append("   • Улучшение осанки\n\n");
                msg.append("3️⃣ *Стиль и имидж:*\n");
                msg.append("   • Качественная одежда под твой тип\n");
                msg.append("   • Современный образ\n");
                msg.append("   • Уход за лицом\n\n");
                msg.append("⏱️ *Потенциал: до 7.5-8 за 4-6 месяцев*\n");
            } else if (rating < 8) {
                msg.append("📊 *УРОВЕНЬ: ХОРОШИЙ* 💪\n");
                msg.append("Отличная физическая форма! Ты на правильном пути.\n\n");
                msg.append("🎯 *РЕКОМЕНДАЦИИ ДЛЯ ЭЛИТНОГО УРОВНЯ:*\n");
                msg.append("1️⃣ *Тонкая настройка:*\n");
                msg.append("   • Специализированные тренировки\n");
                msg.append("   • Работа над слабыми группами мышц\n");
                msg.append("   • Микронутриентная оптимизация\n\n");
                msg.append("2️⃣ *Качество тела:*\n");
                msg.append("   • Снижение процента жира до 8-12%\n");
                msg.append("   • Максимизация рельефа\n");
                msg.append("   • Сухость мышц\n\n");
                msg.append("3️⃣ *Персональный имидж:*\n");
                msg.append("   • Премиальный стиль одежды\n");
                msg.append("   • Уникальный образ\n");
                msg.append("   • Ухоженный вид\n\n");
                msg.append("⏱️ *Потенциал: до 8.5-9 за 2-3 месяца*\n");
            } else {
                msg.append("📊 *УРОВЕНЬ: ЭЛИТ* 👑\n");
                msg.append("Ты находишься в топе! Поздравляем!\n\n");
                msg.append("🎯 *ПОДДЕРЖАНИЕ УРОВНЯ:*\n");
                msg.append("1️⃣ *Консистентность:*\n");
                msg.append("   • Регулярные тренировки\n");
                msg.append("   • Дисциплина в питании\n");
                msg.append("   • Восстановление и сон\n\n");
                msg.append("2️⃣ *Совершенствование:*\n");
                msg.append("   • Работа над деталями\n");
                msg.append("   • Инновационные тренировочные методы\n");
                msg.append("   • Персонализованный подход\n\n");
                msg.append("3️⃣ *Презентация:*\n");
                msg.append("   • Премиум имидж\n");
                msg.append("   • Уверенность и харизма\n");
                msg.append("   • Уникальный стиль\n\n");
                msg.append("✨ *Ты в 1% лучших! Держи это!* ✨\n");
            }

            msg.append("\n═══════════════════════════════\n");
        }

        return msg.toString();
    }

    public static String generateMeasurementsMessage(Map<String, Double> measurements, double rating, boolean premium, String gender) {
        double height = measurements.getOrDefault("height", 175.0);
        double weight = measurements.getOrDefault("weight", 75.0);
        double chest = measurements.getOrDefault("chest", 100.0);
        double waist = measurements.getOrDefault("waist", 80.0);
        double hips = measurements.getOrDefault("hips", 90.0);
        double shoulder = measurements.getOrDefault("shoulder", 45.0);

        double bmi = weight / ((height / 100.0) * (height / 100.0));
        StringBuilder msg = new StringBuilder();

        msg.append("🌟 *АНАЛИЗ ПО ЗАМЕРАМ: ").append(String.format("%.1f", rating)).append("/10* 🌟\n\n");

        if (!premium) {
            msg.append("📊 *ТВОИ ПАРАМЕТРЫ:*\n");
            if (gender.equals("male")) {
                msg.append("• Рост: ").append(String.format("%.0f", height)).append(" см\n");
                msg.append("• Вес: ").append(String.format("%.1f", weight)).append(" кг\n");
                msg.append("• BMI: ").append(String.format("%.1f", bmi)).append("\n");
                msg.append("• Плечи: ").append(String.format("%.0f", shoulder)).append(" см\n");
                msg.append("• Грудь: ").append(String.format("%.0f", chest)).append(" см\n");
                msg.append("• Талия: ").append(String.format("%.0f", waist)).append(" см\n");
            } else {
                msg.append("• Рост: ").append(String.format("%.0f", height)).append(" см\n");
                msg.append("• Вес: ").append(String.format("%.1f", weight)).append(" кг\n");
                msg.append("• BMI: ").append(String.format("%.1f", bmi)).append("\n");
                msg.append("• Грудь: ").append(String.format("%.0f", chest)).append(" см\n");
                msg.append("• Талия: ").append(String.format("%.0f", waist)).append(" см\n");
                msg.append("• Бёдра: ").append(String.format("%.0f", hips)).append(" см\n");
            }
            msg.append("\n💎 *Премиум покажет детальный анализ и рекомендации!*\n");
        } else {
            msg.append("═══════════════════════════════\n");
            msg.append("📊 *ПОДРОБНЫЙ АНАЛИЗ ЗАМЕРОВ*\n");
            msg.append("═══════════════════════════════\n\n");

            msg.append("📏 *ТВОИ ПАРАМЕТРЫ:*\n");
            if (gender.equals("male")) {
                msg.append("• Рост: ").append(String.format("%.0f", height)).append(" см ")
                   .append(height >= 175 && height <= 185 ? "✅" : "⚠️").append("\n");
                msg.append("• Вес: ").append(String.format("%.1f", weight)).append(" кг\n");
                msg.append("• BMI: ").append(String.format("%.1f", bmi)).append(" ")
                   .append(bmi >= 18.5 && bmi <= 25.0 ? "✅ (идеально)" : "⚠️").append("\n");
                msg.append("• Плечи: ").append(String.format("%.0f", shoulder)).append(" см\n");
                msg.append("• Грудь: ").append(String.format("%.0f", chest)).append(" см\n");
                msg.append("• Талия: ").append(String.format("%.0f", waist)).append(" см\n\n");

                msg.append("💪 *СООТНОШЕНИЯ (КЛЮ*ЧЕВЫЕ МЕТРИКИ):*\n");
                double shoulderWaistRatio = shoulder / waist;
                double chestWaistRatio = chest / waist;
                msg.append("• Плечи/Талия: ").append(String.format("%.2f", shoulderWaistRatio)).append(" ")
                   .append(shoulderWaistRatio >= 1.5 && shoulderWaistRatio <= 1.7 ? "✅ (идеально)" : "⚠️").append("\n");
                msg.append("• Грудь/Талия: ").append(String.format("%.2f", chestWaistRatio)).append(" ")
                   .append(chestWaistRatio >= 1.20 && chestWaistRatio <= 1.35 ? "✅ (идеально)" : "⚠️").append("\n\n");

                msg.append("🎯 *РЕКОМЕНДАЦИИ:*\n");
                if (shoulder / waist < 1.5) {
                    msg.append("• Расширить плечи: подтягивания, жимы\n");
                }
                if (chest / waist < 1.2) {
                    msg.append("• Увеличить грудь: жимы лёжа, разведения\n");
                }
                if (waist > 85) {
                    msg.append("• Уменьшить талию: кардио, дефицит калорий\n");
                }
                msg.append("• Тренировки: 4-5 раз в неделю\n");
                msg.append("• Прогрессивная перегрузка для роста мышц\n");

            } else {
                msg.append("• Рост: ").append(String.format("%.0f", height)).append(" см ")
                   .append(height >= 162 && height <= 175 ? "✅" : "⚠️").append("\n");
                msg.append("• Вес: ").append(String.format("%.1f", weight)).append(" кг\n");
                msg.append("• BMI: ").append(String.format("%.1f", bmi)).append(" ")
                   .append(bmi >= 19.0 && bmi <= 24.0 ? "✅ (идеально)" : "⚠️").append("\n");
                msg.append("• Грудь: ").append(String.format("%.0f", chest)).append(" см\n");
                msg.append("• Талия: ").append(String.format("%.0f", waist)).append(" см\n");
                msg.append("• Бёдра: ").append(String.format("%.0f", hips)).append(" см\n\n");

                msg.append("✨ *ГАРМОНИЯ ПРОПОРЦИЙ:*\n");
                double waistHipsRatio = waist / hips;
                double chestWaistRatio = chest / waist;
                msg.append("• Талия/Бёдра: ").append(String.format("%.2f", waistHipsRatio)).append(" ")
                   .append(waistHipsRatio >= 0.65 && waistHipsRatio <= 0.75 ? "✅ (идеально)" : "⚠️").append("\n");
                msg.append("• Грудь/Талия: ").append(String.format("%.2f", chestWaistRatio)).append(" ")
                   .append(chestWaistRatio >= 0.85 && chestWaistRatio <= 0.95 ? "✅ (идеально)" : "⚠️").append("\n\n");

                msg.append("🎯 *РЕКОМЕНДАЦИИ:*\n");
                if (waist / hips > 0.75) {
                    msg.append("• Уменьшить талию: кардио, дефицит калорий\n");
                }
                if (hips < 85) {
                    msg.append("• Увеличить объём бёдер: упражнения для глютеуса\n");
                }
                if (chest < 85) {
                    msg.append("• Увеличить грудь: тренировки грудных\n");
                }
                msg.append("• Силовые упражнения 3-4 раза в неделю\n");
                msg.append("• Кардио 2-3 раза для рельефа\n");
            }

            msg.append("\n═══════════════════════════════\n");
        }

        return msg.toString();
    }

    public static String compareToCelebrity(double rating, String gender) {
        String[] celebs = gender.equals("male")
            ? new String[]{"Генри Кавилл (9.5)", "Тимоти Шаламе (8.0)", "Зак Эфрон (9.0)"}
            : new String[]{"Марго Робби (9.5)", "Зендая (8.5)", "Кендалл Дженнер (9.0)"};
        String celeb = celebs[(int) (Math.random() * celebs.length)];
        return "⭐ *СРАВНЕНИЕ СО ЗНАМЕНИТОСТЯМИ:*\n\n" +
               "Твой рейтинг: " + String.format("%.1f", rating) + "/10\n" +
               "Знаменитость: " + celeb + "\n\n" +
               (rating >= 8 ? "🔥 Ты в одной лиге!" : rating >= 6 ? "💪 Очень близко!" : "📈 Есть куда расти!");
    }
}
